package com.voronina.course;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.util.concurrent.locks.ReentrantLock;

public class ThreadSafeOutputWriter implements AutoCloseable {
  private final OutputFileFormat format;
  private final String baseFileName;
  private final boolean overwrite;

  private final Map<String, String[]> apiFieldNames = new LinkedHashMap<>();
  private final Map<String, String> printAliases = new LinkedHashMap<>();
  private final String[] csvHeaders;

  private final ReentrantLock lock = new ReentrantLock();

  private boolean initialized = false;

  private final Gson gson = new GsonBuilder()
      .serializeNulls()
      .setPrettyPrinting()
      .create();

  public ThreadSafeOutputWriter(
      List<Api> apis,
      OutputFileFormat format,
      String baseFileName,
      boolean overwrite) {
    this.format = format != null ? format : OutputFileFormat.JSON;
    this.baseFileName = baseFileName == null || baseFileName.isBlank()
        ? "output"
        : baseFileName;
    this.overwrite = overwrite;

    for (Api api : apis) {
      String apiName = sanitizeName(api.name());

      apiFieldNames.put(apiName, api.csvHeaders());

      // Allow printing by real API name: RandomUserApi -> randomuserapi
      printAliases.put(apiName, apiName);

      // Allow printing by registry-style key: RandomUserApi -> randomuser
      if (apiName.endsWith("api")) {
        printAliases.put(apiName.substring(0, apiName.length() - 3), apiName);
      }
    }

    this.csvHeaders = buildCsvHeaders();
  }

  public void writeBatch(String source, List<ApiObject> objects) {
    if (objects == null || objects.isEmpty()) {
      return;
    }

    lock.lock();
    try {
      initializeIfNeeded();

      if (format == OutputFileFormat.JSON) {
        writeJsonBatch(source, objects);
      } else {
        writeCsvBatch(source, objects);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      lock.unlock();
    }
  }

  public void printOutput(String apiToPrint) {
    lock.lock();
    try {
      if (format == OutputFileFormat.JSON) {
        printJsonOutput(apiToPrint);
      } else {
        printCsvOutput(apiToPrint);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      lock.unlock();
    }
  }

  private void initializeIfNeeded() throws IOException {
    if (initialized) {
      return;
    }

    Path out = getOutputPath();

    if (format == OutputFileFormat.JSON) {
      initializeJson(out);
    } else {
      initializeCsv(out);
    }

    initialized = true;
  }

  private void initializeJson(Path out) throws IOException {
    if (overwrite || !Files.exists(out)) {
      Files.writeString(
          out,
          "[]",
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    }
  }

  private void initializeCsv(Path out) throws IOException {
    boolean fileExists = Files.exists(out);

    if (overwrite || !fileExists) {
      writeCsvHeader(out, true);
      return;
    }

    String existingHeader = Files.lines(out, StandardCharsets.UTF_8)
        .findFirst()
        .orElse("");

    String newHeader = csvHeaderAsString();

    if (!existingHeader.trim().equals(newHeader.trim())) {
      System.out.println("Warning: CSV headers changed for '" + out + "', overwriting the file.");
      writeCsvHeader(out, true);
    }
  }

  private void writeJsonBatch(String source, List<ApiObject> objects) throws IOException {
    Path out = getOutputPath();

    JsonArray root = new JsonArray();

    if (Files.exists(out)) {
      try (BufferedReader reader = Files.newBufferedReader(out, StandardCharsets.UTF_8)) {
        JsonElement parsed = com.google.gson.JsonParser.parseReader(reader);
        if (parsed != null && parsed.isJsonArray()) {
          root = parsed.getAsJsonArray();
        }
      } catch (Exception ignored) {
        root = new JsonArray();
      }
    }

    String timestamp = nowUtc();

    for (ApiObject obj : objects) {
      if (obj == null) {
        continue;
      }

      JsonObject envelope = new JsonObject();
      envelope.addProperty("id", UUID.randomUUID().toString());
      envelope.addProperty("source", source);
      envelope.addProperty("timestamp", timestamp);
      envelope.add("data", obj.toGson().toJsonTree(obj));

      root.add(envelope);
    }

    Files.writeString(
        out,
        gson.toJson(root),
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);
  }

  private void writeCsvBatch(String source, List<ApiObject> objects) throws IOException {
    Path out = getOutputPath();

    String apiName = sanitizeName(source);
    String[] apiFields = apiFieldNames.get(apiName);

    if (apiFields == null) {
      System.out.println("Warning: unknown API source for CSV writing: " + source);
      return;
    }

    int offset = calculateCsvOffset(apiName);
    String timestamp = nowUtc();

    try (BufferedWriter writer = Files.newBufferedWriter(
        out,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.APPEND);
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

      for (ApiObject obj : objects) {
        if (obj == null) {
          continue;
        }

        String[] values = obj.toCsvFields();

        Object[] row = new Object[csvHeaders.length];
        row[0] = UUID.randomUUID().toString();
        row[1] = apiName;
        row[2] = timestamp;

        for (int i = 0; i < values.length && i < apiFields.length; i++) {
          row[offset + i] = values[i];
        }

        printer.printRecord(row);
      }
    }
  }

  private String[] buildCsvHeaders() {
    List<String> headers = new ArrayList<>();

    headers.add("UUID");
    headers.add("source");
    headers.add("timestamp");

    for (Map.Entry<String, String[]> entry : apiFieldNames.entrySet()) {
      String apiName = entry.getKey();

      for (String field : entry.getValue()) {
        headers.add(apiName + "_" + field);
      }
    }

    return headers.toArray(new String[0]);
  }

  private int calculateCsvOffset(String apiName) {
    int offset = 3;

    for (Map.Entry<String, String[]> entry : apiFieldNames.entrySet()) {
      if (entry.getKey().equals(apiName)) {
        break;
      }

      offset += entry.getValue().length;
    }

    return offset;
  }

  private void writeCsvHeader(Path out, boolean truncate) throws IOException {
    StandardOpenOption[] options = truncate
        ? new StandardOpenOption[] {
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        }
        : new StandardOpenOption[] {
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        };

    try (BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8, options);
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

      printer.printRecord((Object[]) csvHeaders);
    }
  }

  private String csvHeaderAsString() throws IOException {
    StringWriter sw = new StringWriter();

    try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
      printer.printRecord((Object[]) csvHeaders);
    }

    return sw.toString().trim();
  }

  private void printJsonOutput(String apiToPrint) throws IOException {
    Path out = getOutputPath();

    if (!Files.exists(out)) {
      System.out.println("No JSON output file found: " + out.toAbsolutePath());
      return;
    }

    String content = Files.readString(out, StandardCharsets.UTF_8);
    JsonElement parsed = com.google.gson.JsonParser.parseString(content);

    if (apiToPrint == null) {
      System.out.println("Printing disabled.");
      return;
    }

    if (apiToPrint.isBlank()) {
      System.out.println("=== JSON output (" + out.toAbsolutePath() + ") ===");
      System.out.println(gson.toJson(parsed));
      return;
    }

    if (!parsed.isJsonArray()) {
      System.out.println("JSON output is not an array: cannot filter by source");
      return;
    }

    List<String> requestedSources = parseRequestedSources(apiToPrint);

    for (String requestedSource : requestedSources) {
      JsonArray filtered = new JsonArray();

      for (JsonElement element : parsed.getAsJsonArray()) {
        if (!element.isJsonObject()) {
          continue;
        }

        JsonElement src = element.getAsJsonObject().get("source");

        if (src != null && requestedSource.equals(sanitizeName(src.getAsString()))) {
          filtered.add(element);
        }
      }

      System.out.println("--- source: " + requestedSource + " (" + filtered.size() + " records) ---");
      System.out.println(gson.toJson(filtered));
    }
  }

  private void printCsvOutput(String apiToPrint) throws IOException {
    Path out = getOutputPath();

    if (!Files.exists(out)) {
      System.out.println("CSV file not found: " + out.toAbsolutePath());
      return;
    }

    if (apiToPrint == null) {
      System.out.println("Printing disabled.");
      return;
    }

    System.out.println("=== CSV: " + out.toAbsolutePath() + " ===");

    if (apiToPrint.isBlank()) {
      Files.lines(out, StandardCharsets.UTF_8).forEach(System.out::println);
      return;
    }

    List<String> keys = parseRequestedSources(apiToPrint);

    try (BufferedReader reader = Files.newBufferedReader(out, StandardCharsets.UTF_8);
        CSVParser parser = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .build()
            .parse(reader);
        CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(System.out), CSVFormat.DEFAULT)) {

      printer.printRecord(parser.getHeaderNames().toArray());

      for (CSVRecord record : parser) {
        String src = record.get("source");

        if (keys.contains(sanitizeName(src))) {
          printer.printRecord(record.toList().toArray());
        }
      }

      printer.flush();
    }
  }

  private Path getOutputPath() {
    String extension = format == OutputFileFormat.JSON ? ".json" : ".csv";
    return Paths.get(baseFileName + extension);
  }

  private static String nowUtc() {
    return ZonedDateTime.now(ZoneOffset.UTC)
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
  }

  private static String sanitizeName(String name) {
    if (name == null) {
      return "api";
    }

    return name.replaceAll("[^A-Za-z0-9_-]", "_").toLowerCase();
  }

  @Override
  public void close() {
    // No permanent opened stream.
    // The method exists so ApiPollingManager can close it consistently.
  }

  private List<String> parseRequestedSources(String apiToPrint) {
    List<String> result = new ArrayList<>();

    if (apiToPrint == null || apiToPrint.isBlank()) {
      return result;
    }

    for (String raw : apiToPrint.split(",")) {
      String key = sanitizeName(raw.trim());

      if (key.isBlank()) {
        continue;
      }

      String resolved = printAliases.getOrDefault(key, key);

      if (!result.contains(resolved)) {
        result.add(resolved);
      }
    }

    return result;
  }
}