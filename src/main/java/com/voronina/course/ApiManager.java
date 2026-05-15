package com.voronina.course;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.io.BufferedWriter;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ApiManager {
  private final int defaultObjectsCount = 50;
  private final String defaultOutputFileName = "output";

  /**
   * Fetch up to objects_count items from each API and save them depending on
   * format.
   * Assumptions:
   * - If a fetch returns no objects or throws IOException/InterruptedException
   * it's a failure.
   * - After maxConsecutiveFailures consecutive failures for one API we abort with
   * fatal message.
   */
  public void run(List<Api> apis,
      OutputFileFormat outputFileFormat,
      String outputFileName,
      boolean overwrite,
      String apiToPrint,
      int objectsCount,
      long intervalMillis) {
    final int perApiTarget = objectsCount > 0 ? objectsCount : defaultObjectsCount;
    final int maxConsecutiveFailures = 5; // reasonable default

    Map<String, List<ApiObject>> collected = new LinkedHashMap<>();

    for (Api api : apis) {
      String apiName = sanitizeName(api.name());
      List<ApiObject> list = new ArrayList<>(perApiTarget);
      int consecutiveFailures = 0;

      System.out.println("Starting fetch from API: " + api.name() + " (target: " + perApiTarget + " objects)");

      while (list.size() < perApiTarget) {
        try {
          System.out.println("Fetching from '" + api.name() + "' (current: " + list.size() + "/" + perApiTarget + ")");
          ApiObject[] objs = api.fetchData();
          if (objs == null || objs.length == 0) {
            consecutiveFailures++;
            System.out.println(
                "Warning: api '" + api.name() + "' returned no objects (failure #" + consecutiveFailures + ").");
          } else {
            // reset failure streak on success
            consecutiveFailures = 0;
            System.out.println("Successfully fetched " + objs.length + " objects from '" + api.name() + "'");
            for (ApiObject o : objs) {
              if (o == null)
                continue;
              list.add(o);
              if (list.size() >= perApiTarget)
                break;
            }
          }
        } catch (IOException e) {
          consecutiveFailures++;
          System.out.println("Warning: IOException while fetching from '" + api.name() + "': " + e.getMessage());
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          System.out.println("Fatal: interrupted while fetching from '" + api.name() + "'. Aborting.");
          return;
        } catch (RuntimeException e) {
          consecutiveFailures++;
          System.out.println("Warning: unexpected error while fetching from '" + api.name() + "': " + e.getMessage());
        }

        if (consecutiveFailures >= maxConsecutiveFailures) {
          System.out.println("Fatal: too many consecutive failures for '" + api.name() + "' (>= "
              + maxConsecutiveFailures + "). Aborting run.");
          return;
        }

        if (intervalMillis > 0 && list.size() < perApiTarget) {
          try {
            System.out.println("Waiting " + intervalMillis + "ms before next fetch...");
            Thread.sleep(intervalMillis);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Fatal: interrupted during interval sleep. Aborting.");
            return;
          }
        }
      }

      collected.put(apiName, list);
      System.out.println("Collected " + list.size() + " objects from api '" + api.name() + "'.");
    }

    // write results
    String baseName = outputFileName == null || outputFileName.isBlank() ? defaultOutputFileName : outputFileName;
    if (outputFileFormat == OutputFileFormat.JSON) {
      writeJson(collected, baseName, overwrite);
      printJsonOutput(baseName, apiToPrint);
    } else {
      writeCsv(collected, baseName, overwrite);
      printCsvOutput(baseName, apiToPrint);
    }
  }

  private void writeJson(Map<String, List<ApiObject>> collected, String outputFileName, boolean overwrite) {
    Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
    JsonArray root = new JsonArray();
    Path out = Paths.get(outputFileName + ".json");

    // If not overwriting and file exists, load existing records and append
    if (!overwrite && Files.exists(out)) {
      try (BufferedReader reader = Files.newBufferedReader(out, StandardCharsets.UTF_8)) {
        try {
          com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseReader(reader);
          if (parsed != null && parsed.isJsonArray()) {
            root = parsed.getAsJsonArray();
          }
        } catch (Exception ex) {
          root = new JsonArray();
        }
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }

    String timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    for (Map.Entry<String, List<ApiObject>> e : collected.entrySet()) {
      String apiName = e.getKey();
      for (ApiObject obj : e.getValue()) {
        Gson objGson = obj.toGson();
        JsonElement data = objGson.toJsonTree(obj);

        JsonObject envelope = new JsonObject();
        envelope.addProperty("id", UUID.randomUUID().toString());
        envelope.addProperty("source", apiName);
        envelope.addProperty("timestamp", timestamp);
        envelope.add("data", data);
        root.add(envelope);
      }
    }

    try {
      Files.writeString(out, gson.toJson(root), StandardCharsets.UTF_8,
          StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      System.out.println("Wrote JSON output to " + out.toAbsolutePath());
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private void writeCsv(Map<String, List<ApiObject>> collected, String outputFileName, boolean overwrite) {
    Path out = Paths.get(outputFileName + ".csv");

    JsonArray root = new JsonArray();

    // source -> csv fields
    Map<String, List<String>> apiFieldNames = new LinkedHashMap<>();

    /*
     * Step 1:
     * If we are not overwriting, read the old CSV and convert it into
     * JSON-like objects:
     *
     * {
     * "id": "...",
     * "source": "...",
     * "timestamp": "...",
     * "data": {
     * "field1": "...",
     * "field2": "..."
     * }
     * }
     */
    if (!overwrite && Files.exists(out)) {
      try (BufferedReader reader = Files.newBufferedReader(out, StandardCharsets.UTF_8);
          CSVParser parser = CSVFormat.DEFAULT.builder()
              .setHeader()
              .setSkipHeaderRecord(true)
              .build()
              .parse(reader)) {

        List<String> headers = parser.getHeaderNames();

        for (CSVRecord record : parser) {
          String source = safeCsvGet(record, "source");
          if (source.isBlank()) {
            continue;
          }

          String id = safeCsvGet(record, "UUID");
          if (id.isBlank()) {
            id = UUID.randomUUID().toString();
          }

          String timestamp = safeCsvGet(record, "timestamp");

          JsonObject data = new JsonObject();

          String prefix = source + "_";

          for (String header : headers) {
            if (header.equals("UUID") || header.equals("source") || header.equals("timestamp")) {
              continue;
            }

            if (!header.startsWith(prefix)) {
              continue;
            }

            String fieldName = header.substring(prefix.length());
            String value = safeCsvGet(record, header);

            data.addProperty(fieldName, value);
            addFieldName(apiFieldNames, source, fieldName);
          }

          JsonObject envelope = new JsonObject();
          envelope.addProperty("id", id);
          envelope.addProperty("source", source);
          envelope.addProperty("timestamp", timestamp);
          envelope.add("data", data);

          root.add(envelope);
        }
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }

    /*
     * Step 2:
     * Add newly fetched API objects into the same JSON-like array.
     *
     * Important:
     * We use csvHeaders() + toCsvFields() here instead of Gson object fields,
     * because CSV format is controlled by these two methods.
     */
    String timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    for (Map.Entry<String, List<ApiObject>> e : collected.entrySet()) {
      String apiName = e.getKey();

      for (ApiObject obj : e.getValue()) {
        if (obj == null) {
          continue;
        }

        String[] headers = obj.csvHeaders();
        String[] values = obj.toCsvFields();

        JsonObject data = new JsonObject();

        for (int i = 0; i < headers.length; ++i) {
          String fieldName = headers[i];
          String value = i < values.length ? values[i] : "";

          data.addProperty(fieldName, value);
          addFieldName(apiFieldNames, apiName, fieldName);
        }

        JsonObject envelope = new JsonObject();
        envelope.addProperty("id", UUID.randomUUID().toString());
        envelope.addProperty("source", apiName);
        envelope.addProperty("timestamp", timestamp);
        envelope.add("data", data);

        root.add(envelope);
      }
    }

    /*
     * Step 3:
     * Build a new full CSV header from all known APIs and fields.
     */
    List<String> allHeaders = new ArrayList<>();
    allHeaders.add("UUID");
    allHeaders.add("source");
    allHeaders.add("timestamp");

    for (Map.Entry<String, List<String>> e : apiFieldNames.entrySet()) {
      String apiName = e.getKey();

      for (String fieldName : e.getValue()) {
        allHeaders.add(apiName + "_" + fieldName);
      }
    }

    String[] headers = allHeaders.toArray(new String[0]);

    /*
     * Step 4:
     * Write the whole CSV back from JSON-like data.
     */
    try (BufferedWriter writer = Files.newBufferedWriter(
        out,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

      printer.printRecord((Object[]) headers);

      for (JsonElement element : root) {
        if (!element.isJsonObject()) {
          continue;
        }

        JsonObject envelope = element.getAsJsonObject();

        String id = getStringOrDefault(envelope, "id", UUID.randomUUID().toString());
        String source = getStringOrDefault(envelope, "source", "");
        String rowTimestamp = getStringOrDefault(envelope, "timestamp", "");

        JsonObject data = new JsonObject();
        if (envelope.has("data") && envelope.get("data").isJsonObject()) {
          data = envelope.getAsJsonObject("data");
        }

        Object[] row = new Object[headers.length];

        row[0] = id;
        row[1] = source;
        row[2] = rowTimestamp;

        for (int i = 3; i < headers.length; ++i) {
          String header = headers[i];
          String prefix = source + "_";

          if (!header.startsWith(prefix)) {
            row[i] = "";
            continue;
          }

          String fieldName = header.substring(prefix.length());

          if (data.has(fieldName) && !data.get(fieldName).isJsonNull()) {
            JsonElement value = data.get(fieldName);

            if (value.isJsonPrimitive()) {
              row[i] = value.getAsString();
            } else {
              row[i] = value.toString();
            }
          } else {
            row[i] = "";
          }
        }

        printer.printRecord(row);
      }

      System.out.println("Wrote CSV output to " + out.toAbsolutePath());
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private static void addFieldName(Map<String, List<String>> apiFieldNames, String apiName, String fieldName) {
    List<String> fields = apiFieldNames.computeIfAbsent(apiName, k -> new ArrayList<>());

    if (!fields.contains(fieldName)) {
      fields.add(fieldName);
    }
  }

  private static String safeCsvGet(CSVRecord record, String name) {
    try {
      if (!record.isMapped(name)) {
        return "";
      }

      String value = record.get(name);
      return value == null ? "" : value;
    } catch (IllegalArgumentException ex) {
      return "";
    }
  }

  private static String getStringOrDefault(JsonObject object, String fieldName, String defaultValue) {
    if (!object.has(fieldName) || object.get(fieldName).isJsonNull()) {
      return defaultValue;
    }

    return object.get(fieldName).getAsString();
  }

  private void printJsonOutput(String baseName, String apiToPrint) {
    Path out = Paths.get(baseName + ".json");
    if (!Files.exists(out)) {
      System.out.println("No JSON output file found: " + out.toAbsolutePath());
      return;
    }

    try {
      String content = Files.readString(out, StandardCharsets.UTF_8);
      com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseString(content);

      if (apiToPrint == null || apiToPrint.isBlank()) {
        // print everything
        System.out.println("=== JSON output (" + out.toAbsolutePath() + ") ===");
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(parsed));
        return;
      }

      // filter by source field
      if (!parsed.isJsonArray()) {
        System.out.println("JSON output is not an array: cannot filter by source");
        return;
      }
      String[] requested = apiToPrint.split(",");
      for (String req : requested) {
        String key = sanitizeName(req.trim());
        JsonArray filtered = new JsonArray();
        for (com.google.gson.JsonElement el : parsed.getAsJsonArray()) {
          if (el.isJsonObject()) {
            com.google.gson.JsonElement src = el.getAsJsonObject().get("source");
            if (src != null && key.equals(src.getAsString())) {
              filtered.add(el);
            }
          }
        }
        System.out.println("--- source: " + req.trim() + " (" + filtered.size() + " records) ---");
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(filtered));
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void printCsvOutput(String baseName, String apiToPrint) {
    Path out = Paths.get(baseName + ".csv");
    if (!Files.exists(out)) {
      System.out.println("CSV file not found: " + out.toAbsolutePath());
      return;
    }
    System.out.println("=== CSV: " + out.toAbsolutePath() + " ===");
    try {
      if (apiToPrint == null || apiToPrint.isBlank()) {
        // Print the whole file as-is
        Files.lines(out, StandardCharsets.UTF_8).forEach(System.out::println);
        return;
      }

      // Collect requested source keys (sanitized)
      List<String> keys = new ArrayList<>();
      for (String s : apiToPrint.split(","))
        keys.add(sanitizeName(s.trim()));

      // Use CSVParser to read; re-print header + matching rows via CSVPrinter
      try (BufferedReader reader = Files.newBufferedReader(out, StandardCharsets.UTF_8);
          CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader);
          CSVPrinter printer = new CSVPrinter(new java.io.OutputStreamWriter(System.out), CSVFormat.DEFAULT)) {

        printer.printRecord(parser.getHeaderNames().toArray());
        for (CSVRecord record : parser) {
          String src = record.get("source");
          if (keys.contains(sanitizeName(src))) {
            printer.printRecord(record.toList().toArray());
          }
        }
        printer.flush();
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String sanitizeName(String name) {
    if (name == null)
      return "api";
    return name.replaceAll("[^A-Za-z0-9_-]", "_").toLowerCase();
  }
}
