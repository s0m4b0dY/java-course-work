package com.voronina.course;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonOutputWriter implements OutputWriter {
    private final Gson prettyGson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

    @Override
    public void write(Map<String, List<ApiObject>> collected, String outputFileName, boolean overwrite) {
        JsonArray root = new JsonArray();
        Path out = Paths.get(outputFileName + ".json");

        if (!overwrite && Files.exists(out)) {
            try (BufferedReader reader = Files.newBufferedReader(out, StandardCharsets.UTF_8)) {
                try {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    if (parsed != null && parsed.isJsonArray()) {
                        root = parsed.getAsJsonArray();
                    }
                } catch (Exception ex) {
                    System.out.println("Warning: old JSON is broken, starting from empty array");
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
                if (obj == null) {
                    continue;
                }

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
            Files.writeString(out, prettyGson.toJson(root), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Wrote JSON output to " + out.toAbsolutePath());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void print(String baseName, String apiToPrint) {
        Path out = Paths.get(baseName + ".json");
        if (!Files.exists(out)) {
            System.out.println("No JSON output file found: " + out.toAbsolutePath());
            return;
        }

        try {
            String content = Files.readString(out, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(content);

            if (apiToPrint == null) {
                return;
            }

            if (apiToPrint.isBlank()) {
                System.out.println("=== JSON output (" + out.toAbsolutePath() + ") ===");
                System.out.println(prettyGson.toJson(parsed));
                return;
            }

            if (!parsed.isJsonArray()) {
                System.out.println("JSON output is not an array: cannot filter by source");
                return;
            }

            String[] requested = apiToPrint.split(",");
            for (String req : requested) {
                String key = ApiNames.sanitize(req.trim());
                JsonArray filtered = new JsonArray();

                for (JsonElement el : parsed.getAsJsonArray()) {
                    if (el.isJsonObject()) {
                        JsonElement src = el.getAsJsonObject().get("source");
                        if (src != null && key.equals(src.getAsString())) {
                            filtered.add(el);
                        }
                    }
                }

                System.out.println("--- source: " + req.trim() + " (" + filtered.size() + " records) ---");
                System.out.println(prettyGson.toJson(filtered));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
