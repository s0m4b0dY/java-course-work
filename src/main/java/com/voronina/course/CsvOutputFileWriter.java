package com.voronina.course;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
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

public class CsvOutputFileWriter implements OutputWriter {
  private final String baseFileName;
  private final boolean overwrite;
  private final Map<String, String[]> apiFieldNames = new LinkedHashMap<>();
  private final Map<String, String> printAliases;
  private List<String> activeHeaders;
  private boolean initialized = false;

  public CsvOutputFileWriter(List<Api> apis, String baseFileName, boolean overwrite) {
    this.baseFileName = baseFileName == null || baseFileName.isBlank() ? "output" : baseFileName;
    this.overwrite = overwrite;

    for (Api api : apis) {
      String apiName = SourceNames.sanitize(api.name());
      apiFieldNames.put(apiName, api.csvHeaders());
    }

    this.printAliases = SourceNames.buildPrintAliases(apis);
    this.activeHeaders = buildCsvHeaders();
  }

  @Override
  public void writeBatch(String source, List<ApiObject> objects) {
    if (objects == null || objects.isEmpty()) {
      return;
    }

    try {
      initializeIfNeeded();
      writeCsvBatch(source, objects);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void initializeIfNeeded() throws IOException {
    if (initialized) {
      return;
    }

    Path out = getOutputPath();
    boolean fileExists = Files.exists(out);

    if (overwrite || !fileExists) {
      writeAllRows(out, activeHeaders, List.of());
      initialized = true;
      return;
    }

    List<String> oldHeaders = readHeader(out);
    if (oldHeaders.isEmpty()) {
      System.out.println("Debug: csv file is empty, writing new header");
      writeAllRows(out, activeHeaders, List.of());
      initialized = true;
      return;
    }

    List<String> mergedHeaders = mergeHeaders(oldHeaders, activeHeaders);
    activeHeaders = mergedHeaders;

    if (!oldHeaders.equals(mergedHeaders)) {
      System.out.println("Debug: CSV headers changed for '" + out + "', migrating old rows");
      List<Map<String, String>> oldRows = readCsvAsMaps(out);
      writeAllRows(out, mergedHeaders, oldRows);
    }

    initialized = true;
  }

  private void writeCsvBatch(String source, List<ApiObject> objects) throws IOException {
    String apiName = SourceNames.sanitize(source);
    String[] apiFields = apiFieldNames.get(apiName);

    if (apiFields == null) {
      System.out.println("Warning: unknown API source for CSV writing: " + source);
      return;
    }

    String timestamp = nowUtc();
    Path out = getOutputPath();

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

        Map<String, String> row = new LinkedHashMap<>();
        row.put("UUID", UUID.randomUUID().toString());
        row.put("source", apiName);
        row.put("timestamp", timestamp);

        String[] values = obj.toCsvFields();
        for (int i = 0; i < values.length && i < apiFields.length; i++) {
          row.put(apiName + "_" + apiFields[i], values[i]);
        }

        printer.printRecord(toRecord(activeHeaders, row));
      }
    }
  }

  List<String> buildCsvHeaders() {
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

    return headers;
  }

  List<String> mergeHeaders(List<String> oldHeaders, List<String> newHeaders) {
    List<String> merged = new ArrayList<>();

    for (String h : oldHeaders) {
      if (!merged.contains(h)) {
        merged.add(h);
      }
    }

    for (String h : newHeaders) {
      if (!merged.contains(h)) {
        merged.add(h);
      }
    }

    return merged;
  }

  private List<String> readHeader(Path out) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(out, StandardCharsets.UTF_8);
        CSVParser parser = CSVFormat.DEFAULT.parse(reader)) {

      for (CSVRecord record : parser) {
        return record.toList();
      }
    }

    return List.of();
  }

  private List<Map<String, String>> readCsvAsMaps(Path out) throws IOException {
    List<Map<String, String>> rows = new ArrayList<>();

    try (BufferedReader reader = Files.newBufferedReader(out, StandardCharsets.UTF_8);
        CSVParser parser = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .build()
            .parse(reader)) {

      List<String> headers = parser.getHeaderNames();
      for (CSVRecord record : parser) {
        Map<String, String> row = new LinkedHashMap<>();
        for (String header : headers) {
          row.put(header, record.isMapped(header) ? record.get(header) : "");
        }
        rows.add(row);
      }
    }

    return rows;
  }

  private void writeAllRows(Path out, List<String> headers, List<Map<String, String>> rows) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(
        out,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {

      printer.printRecord(headers);
      for (Map<String, String> row : rows) {
        printer.printRecord(toRecord(headers, row));
      }
    }
  }

  private List<String> toRecord(List<String> headers, Map<String, String> row) {
    List<String> values = new ArrayList<>();

    for (String header : headers) {
      values.add(row.getOrDefault(header, ""));
    }

    return values;
  }

  @Override
  public void printOutput(String apiToPrint) {
    try {
      printCsvOutput(apiToPrint);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
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

    List<String> keys = SourceNames.parseRequestedSources(apiToPrint, printAliases);

    try (BufferedReader reader = Files.newBufferedReader(out, StandardCharsets.UTF_8);
        CSVParser parser = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .build()
            .parse(reader);
        CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(System.out), CSVFormat.DEFAULT)) {

      printer.printRecord(parser.getHeaderNames());

      for (CSVRecord record : parser) {
        String src = record.get("source");

        if (keys.contains(SourceNames.sanitize(src))) {
          printer.printRecord(record.toList());
        }
      }

      printer.flush();
    }
  }

  private Path getOutputPath() {
    return Paths.get(baseFileName + ".csv");
  }

  private static String nowUtc() {
    return ZonedDateTime.now(ZoneOffset.UTC)
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
  }
}
