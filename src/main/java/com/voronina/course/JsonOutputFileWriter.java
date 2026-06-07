package com.voronina.course;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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

public class JsonOutputFileWriter implements OutputWriter {
  private final String baseFileName;
  private final boolean overwrite;
  private final Map<String, String> printAliases;
  private boolean initialized = false;

  private final Gson gson = new GsonBuilder()
      .serializeNulls()
      .setPrettyPrinting()
      .create();

  public JsonOutputFileWriter(List<Api> apis, String baseFileName, boolean overwrite) {
    this.baseFileName = baseFileName == null || baseFileName.isBlank() ? "output" : baseFileName;
    this.overwrite = overwrite;
    this.printAliases = SourceNames.buildPrintAliases(apis);
  }

  @Override
  public void writeBatch(String source, List<ApiObject> objects) {
    if (objects == null || objects.isEmpty()) {
      return;
    }

    try {
      initializeIfNeeded();
      writeJsonBatch(SourceNames.sanitize(source), objects);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void initializeIfNeeded() throws IOException {
    if (initialized) {
      return;
    }

    Path out = getOutputPath();
    if (overwrite || !Files.exists(out)) {
      Files.writeString(
          out,
          "[]",
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    }

    initialized = true;
  }

  private void writeJsonBatch(String source, List<ApiObject> objects) throws IOException {
    Path out = getOutputPath();
    JsonArray root = readExistingJson(out);
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

  private JsonArray readExistingJson(Path out) throws IOException {
    JsonArray root = new JsonArray();

    if (!Files.exists(out)) {
      return root;
    }

    try (BufferedReader reader = Files.newBufferedReader(out, StandardCharsets.UTF_8)) {
      JsonElement parsed = com.google.gson.JsonParser.parseReader(reader);
      if (parsed != null && parsed.isJsonArray()) {
        root = parsed.getAsJsonArray();
      }
    } catch (Exception ignored) {
      System.out.println("Debug: old json is broken, starting new array");
      root = new JsonArray();
    }

    return root;
  }

  @Override
  public void printOutput(String apiToPrint) {
    try {
      printJsonOutput(apiToPrint);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void printJsonOutput(String apiToPrint) throws IOException {
    Path out = getOutputPath();

    if (!Files.exists(out)) {
      System.out.println("No JSON output file found: " + out.toAbsolutePath());
      return;
    }

    if (apiToPrint == null) {
      System.out.println("Printing disabled.");
      return;
    }

    String content = Files.readString(out, StandardCharsets.UTF_8);
    JsonElement parsed = com.google.gson.JsonParser.parseString(content);

    if (apiToPrint.isBlank()) {
      System.out.println("=== JSON output (" + out.toAbsolutePath() + ") ===");
      System.out.println(gson.toJson(parsed));
      return;
    }

    if (!parsed.isJsonArray()) {
      System.out.println("JSON output is not an array: cannot filter by source");
      return;
    }

    List<String> requestedSources = SourceNames.parseRequestedSources(apiToPrint, printAliases);

    for (String requestedSource : requestedSources) {
      JsonArray filtered = new JsonArray();

      for (JsonElement element : parsed.getAsJsonArray()) {
        if (!element.isJsonObject()) {
          continue;
        }

        JsonElement src = element.getAsJsonObject().get("source");
        if (src != null && requestedSource.equals(SourceNames.sanitize(src.getAsString()))) {
          filtered.add(element);
        }
      }

      System.out.println("--- source: " + requestedSource + " (" + filtered.size() + " records) ---");
      System.out.println(gson.toJson(filtered));
    }
  }

  private Path getOutputPath() {
    return Paths.get(baseFileName + ".json");
  }

  private static String nowUtc() {
    return ZonedDateTime.now(ZoneOffset.UTC)
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
  }
}
