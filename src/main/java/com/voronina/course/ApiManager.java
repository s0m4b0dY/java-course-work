package com.voronina.course;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.BufferedReader;
import java.io.StringWriter;
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
   * Fetch up to objects_count items from each API and save them depending on format.
   * Assumptions:
   * - If a fetch returns no objects or throws IOException/InterruptedException it's a failure.
   * - After maxConsecutiveFailures consecutive failures for one API we abort with fatal message.
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

      while (list.size() < perApiTarget) {
        try {
          ApiObject[] objs = api.fetchData();
          if (objs == null || objs.length == 0) {
            consecutiveFailures++;
            System.out.println("Warning: api '" + api.name() + "' returned no objects (failure #" + consecutiveFailures + ").");
          } else {
            // reset failure streak on success
            consecutiveFailures = 0;
            for (ApiObject o : objs) {
              if (o == null) continue;
              list.add(o);
              if (list.size() >= perApiTarget) break;
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
          System.out.println("Fatal: too many consecutive failures for '" + api.name() + "' (>= " + maxConsecutiveFailures + "). Aborting run.");
          return;
        }

        if (intervalMillis > 0 && list.size() < perApiTarget) {
          try {
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
    // Build unified header: UUID, source, timestamp, then api1_field1, api1_field2, api2_field1, ...
    List<String> allHeaders = new ArrayList<>();
    allHeaders.add("UUID");
    allHeaders.add("source");
    allHeaders.add("timestamp");

    // remember per-api field names and their offset in the row
    Map<String, String[]> apiFieldNames = new LinkedHashMap<>();
    for (Map.Entry<String, List<ApiObject>> e : collected.entrySet()) {
      if (!e.getValue().isEmpty()) {
        String apiName = e.getKey();
        String[] fields = e.getValue().get(0).csvHeaders();
        apiFieldNames.put(apiName, fields);
        for (String f : fields) {
          allHeaders.add(apiName + "_" + f);
        }
      }
    }

    String[] headers = allHeaders.toArray(new String[0]);
    Path out = Paths.get(outputFileName + ".csv");
    String timestamp = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    boolean fileExists = Files.exists(out);
    boolean shouldOverwrite = overwrite || !fileExists;

    // If appending, verify that the existing header row matches the new one.
    // If they differ, overwrite the whole file instead.
    if (!shouldOverwrite) {
      try {
        String existingHeader = Files.lines(out, StandardCharsets.UTF_8).findFirst().orElse("");
        StringWriter sw = new StringWriter();
        try (CSVPrinter hp = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
          hp.printRecord((Object[]) headers);
        }
        String newHeaderLine = sw.toString().trim();
        if (!existingHeader.trim().equals(newHeaderLine)) {
          System.out.println("Warning: CSV headers changed for '" + outputFileName + ".csv', overwriting the file.");
          shouldOverwrite = true;
        }
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }

    StandardOpenOption[] options = shouldOverwrite
        ? new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING}
        : new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND};

    try (BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8, options);
         CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

      // Write header only when creating / overwriting (not when appending to a valid file)
      if (shouldOverwrite || !fileExists) {
        printer.printRecord((Object[]) headers);
      }

      for (Map.Entry<String, List<ApiObject>> e : collected.entrySet()) {
        String apiName = e.getKey();
        if (!apiFieldNames.containsKey(apiName)) continue;
        String[] apiFields = apiFieldNames.get(apiName);

        // Calculate the column offset for this API's data fields
        int offset = 3; // UUID + source + timestamp
        for (Map.Entry<String, String[]> ae : apiFieldNames.entrySet()) {
          if (ae.getKey().equals(apiName)) break;
          offset += ae.getValue().length;
        }

        for (ApiObject obj : e.getValue()) {
          String[] values = obj.toCsvFields();
          Object[] row = new Object[headers.length]; // all null → empty cells
          row[0] = UUID.randomUUID().toString();
          row[1] = apiName;
          row[2] = timestamp;
          for (int i = 0; i < values.length && i < apiFields.length; i++) {
            row[offset + i] = values[i];
          }
          printer.printRecord(row);
        }
      }

      System.out.println("Wrote CSV output to " + out.toAbsolutePath());
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
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
      for (String s : apiToPrint.split(",")) keys.add(sanitizeName(s.trim()));

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
    if (name == null) return "api";
    return name.replaceAll("[^A-Za-z0-9_-]", "_").toLowerCase();
  }
}
