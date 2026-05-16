package com.voronina.course;

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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CsvOutputWriter implements OutputWriter {
    @Override
    public void write(Map<String, List<ApiObject>> collected, String outputFileName, boolean overwrite) {
        Path out = Paths.get(outputFileName + ".csv");
        JsonArray root = new JsonArray();

        Map<String, List<String>> apiFieldNames = new LinkedHashMap<>();

        if (!overwrite && Files.exists(out)) {
            System.out.println("Debug: reading old CSV before rewriting it");
            readOldCsvToJson(out, root, apiFieldNames);
        }

        addNewObjects(collected, root, apiFieldNames);

        List<String> allHeaders = buildHeaders(apiFieldNames);
        String[] headers = allHeaders.toArray(new String[0]);

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
                Object[] row = makeCsvRow(envelope, headers);
                printer.printRecord(row);
            }

            System.out.println("Wrote CSV output to " + out.toAbsolutePath());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void print(String baseName, String apiToPrint) {
        Path out = Paths.get(baseName + ".csv");
        if (!Files.exists(out)) {
            System.out.println("CSV file not found: " + out.toAbsolutePath());
            return;
        }

        if (apiToPrint == null) {
            return;
        }

        System.out.println("=== CSV: " + out.toAbsolutePath() + " ===");

        try {
            if (apiToPrint.isBlank()) {
                Files.lines(out, StandardCharsets.UTF_8).forEach(System.out::println);
                return;
            }

            List<String> keys = new ArrayList<>();
            for (String s : apiToPrint.split(",")) {
                keys.add(ApiNames.sanitize(s.trim()));
            }

            try (BufferedReader reader = Files.newBufferedReader(out, StandardCharsets.UTF_8);
                    CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
                            .parse(reader);
                    CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(System.out), CSVFormat.DEFAULT)) {

                printer.printRecord(parser.getHeaderNames().toArray());

                for (CSVRecord record : parser) {
                    String src = safeCsvGet(record, "source");
                    if (keys.contains(ApiNames.sanitize(src))) {
                        printer.printRecord(record.toList().toArray());
                    }
                }

                printer.flush();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void readOldCsvToJson(Path out, JsonArray root, Map<String, List<String>> apiFieldNames) {
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

    private void addNewObjects(Map<String, List<ApiObject>> collected,
            JsonArray root,
            Map<String, List<String>> apiFieldNames) {
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
    }

    private List<String> buildHeaders(Map<String, List<String>> apiFieldNames) {
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

        return allHeaders;
    }

    private Object[] makeCsvRow(JsonObject envelope, String[] headers) {
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

        return row;
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
}
