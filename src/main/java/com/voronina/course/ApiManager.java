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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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
    JsonObject root = new JsonObject();
    Path out = Paths.get(outputFileName + ".json");

    // If not overwriting and file exists, try to merge
    if (!overwrite && Files.exists(out)) {
      try (BufferedReader reader = Files.newBufferedReader(out, StandardCharsets.UTF_8)) {
        try {
          com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseReader(reader);
          if (parsed != null && parsed.isJsonObject()) {
            root = parsed.getAsJsonObject();
          }
        } catch (Exception ex) {
          // ignore parse errors and start fresh
          root = new JsonObject();
        }
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }

    for (Map.Entry<String, List<ApiObject>> e : collected.entrySet()) {
      JsonArray arr = root.has(e.getKey()) && root.get(e.getKey()).isJsonArray() ? root.getAsJsonArray(e.getKey()) : new JsonArray();
      for (ApiObject obj : e.getValue()) {
        Gson objGson = obj.toGson();
        JsonElement el = objGson.toJsonTree(obj);
        arr.add(el);
      }
      root.add(e.getKey(), arr);
    }

    try {
      Files.writeString(out, gson.toJson(root), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
      System.out.println("Wrote JSON output to " + out.toAbsolutePath());
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private void writeCsv(Map<String, List<ApiObject>> collected, String outputFileName, boolean overwrite) {
    for (Map.Entry<String, List<ApiObject>> e : collected.entrySet()) {
      String apiName = e.getKey();
      List<ApiObject> list = e.getValue();
      if (list.isEmpty()) continue;

      Path out = Paths.get(outputFileName + "_" + apiName + ".csv");
      try {
        String[] headers = list.get(0).csvHeaders();
        boolean exists = Files.exists(out);
        boolean writeHeader = !exists || overwrite || Files.size(out) == 0;

        if (exists && !overwrite) {
          // append mode
          try (BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
               CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            if (writeHeader) {
              printer.printRecord((Object[]) headers);
            }
            for (ApiObject obj : list) {
              printer.printRecord((Object[]) obj.toCsvFields());
            }
          }
        } else {
          // create or overwrite
          try (BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
               CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            if (writeHeader) {
              printer.printRecord((Object[]) headers);
            }
            for (ApiObject obj : list) {
              printer.printRecord((Object[]) obj.toCsvFields());
            }
          }
        }
        System.out.println("Wrote CSV output to " + out.toAbsolutePath());
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
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
      if (apiToPrint == null || apiToPrint.isBlank()) {
        System.out.println("=== JSON output (" + out.toAbsolutePath() + ") ===");
        System.out.println(content);
        return;
      }

      // print only specified APIs (support comma-separated names)
      String[] requested = apiToPrint.split(",");
      com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseString(content);
      if (!parsed.isJsonObject()) {
        System.out.println("JSON output is not an object: cannot select keys");
        return;
      }
      com.google.gson.JsonObject root = parsed.getAsJsonObject();
      for (String req : requested) {
        String key = sanitizeName(req.trim());
        System.out.println("--- api: " + req.trim() + " (sanitized: " + key + ") ---");
        if (root.has(key)) {
          System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(root.get(key)));
        } else {
          System.out.println("(no data for key '" + key + "')");
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void printCsvOutput(String baseName, String apiToPrint) {
    try {
      if (apiToPrint == null || apiToPrint.isBlank()) {
        // print all CSV files matching baseName_*.csv
        Path dir = Paths.get(".");
        Files.list(dir)
            .filter(p -> p.getFileName().toString().startsWith(baseName + "_") && p.getFileName().toString().endsWith(".csv"))
            .forEach(p -> printCsvFile(p));
        return;
      }

      String[] requested = apiToPrint.split(",");
      for (String req : requested) {
        String key = sanitizeName(req.trim());
        Path p = Paths.get(baseName + "_" + key + ".csv");
        printCsvFile(p);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void printCsvFile(Path p) {
    if (!Files.exists(p)) {
      System.out.println("CSV file not found: " + p.toAbsolutePath());
      return;
    }
    System.out.println("=== CSV: " + p.toAbsolutePath() + " ===");
    try {
      Files.lines(p, StandardCharsets.UTF_8).forEach(System.out::println);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String sanitizeName(String name) {
    if (name == null) return "api";
    return name.replaceAll("[^A-Za-z0-9_-]", "_").toLowerCase();
  }
}
