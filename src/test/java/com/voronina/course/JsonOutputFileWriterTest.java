package com.voronina.course;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonOutputFileWriterTest {
  @TempDir
  Path tempDir;

  @Test
  void writesAndAppendsJsonArray() throws Exception {
    Api api = new TestObjects.SimpleApi("DemoApi", new String[] { "a" });

    JsonOutputFileWriter writer = new JsonOutputFileWriter(List.of(api), tempDir.resolve("jsondata").toString(), true);
    writer.writeBatch("demoapi", List.of(new TestObjects.SimpleObject(new String[] { "a" }, new String[] { "first" })));

    JsonOutputFileWriter appendWriter = new JsonOutputFileWriter(List.of(api), tempDir.resolve("jsondata").toString(), false);
    appendWriter.writeBatch("demoapi", List.of(new TestObjects.SimpleObject(new String[] { "a" }, new String[] { "second" })));

    String text = Files.readString(tempDir.resolve("jsondata.json"), StandardCharsets.UTF_8);
    JsonElement parsed = com.google.gson.JsonParser.parseString(text);

    assertTrue(parsed.isJsonArray());
    JsonArray array = parsed.getAsJsonArray();
    assertEquals(2, array.size());
    assertEquals("demoapi", array.get(0).getAsJsonObject().get("source").getAsString());
  }

  @Test
  void brokenOldJsonStartsFresh() throws Exception {
    Files.writeString(tempDir.resolve("bad.json"), "not json", StandardCharsets.UTF_8);
    Api api = new TestObjects.SimpleApi("DemoApi", new String[] { "a" });

    JsonOutputFileWriter writer = new JsonOutputFileWriter(List.of(api), tempDir.resolve("bad").toString(), false);
    writer.writeBatch("demoapi", List.of(new TestObjects.SimpleObject(new String[] { "a" }, new String[] { "x" })));

    JsonArray array = com.google.gson.JsonParser.parseString(Files.readString(tempDir.resolve("bad.json"))).getAsJsonArray();
    assertEquals(1, array.size());
  }
}
