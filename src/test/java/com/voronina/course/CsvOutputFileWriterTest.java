package com.voronina.course;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CsvOutputFileWriterTest {
  @TempDir
  Path tempDir;

  @Test
  void writesHeaderAndRows() throws Exception {
    Api api = new TestObjects.SimpleApi("DemoApi", new String[] { "a", "b" });
    CsvOutputFileWriter writer = new CsvOutputFileWriter(List.of(api), tempDir.resolve("out").toString(), true);

    writer.writeBatch("demoapi", List.of(new TestObjects.SimpleObject(
        new String[] { "a", "b" },
        new String[] { "one", "two" })));

    List<String> lines = Files.readAllLines(tempDir.resolve("out.csv"));
    assertEquals("UUID,source,timestamp,demoapi_a,demoapi_b", lines.get(0));
    assertTrue(lines.get(1).contains(",demoapi,"));
    assertTrue(lines.get(1).endsWith(",one,two"));
  }

  @Test
  void appendModeMigratesOldCsvWhenHeadersChanged() throws Exception {
    Api oldApi = new TestObjects.SimpleApi("DemoApi", new String[] { "a" });
    CsvOutputFileWriter oldWriter = new CsvOutputFileWriter(List.of(oldApi), tempDir.resolve("data").toString(), true);
    oldWriter.writeBatch("demoapi", List.of(new TestObjects.SimpleObject(
        new String[] { "a" },
        new String[] { "old" })));

    Api newApi = new TestObjects.SimpleApi("DemoApi", new String[] { "a", "b" });
    CsvOutputFileWriter newWriter = new CsvOutputFileWriter(List.of(newApi), tempDir.resolve("data").toString(), false);
    newWriter.writeBatch("demoapi", List.of(new TestObjects.SimpleObject(
        new String[] { "a", "b" },
        new String[] { "new", "second" })));

    try (BufferedReader reader = Files.newBufferedReader(tempDir.resolve("data.csv"), StandardCharsets.UTF_8);
        CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {

      assertEquals(List.of("UUID", "source", "timestamp", "demoapi_a", "demoapi_b"), parser.getHeaderNames());

      List<CSVRecord> rows = parser.getRecords();
      assertEquals(2, rows.size());
      assertEquals("old", rows.get(0).get("demoapi_a"));
      assertEquals("", rows.get(0).get("demoapi_b"));
      assertEquals("new", rows.get(1).get("demoapi_a"));
      assertEquals("second", rows.get(1).get("demoapi_b"));
    }
  }

  @Test
  void mergeHeadersKeepsOldOrderAndAddsMissing() {
    CsvOutputFileWriter writer = new CsvOutputFileWriter(List.of(), tempDir.resolve("x").toString(), true);

    List<String> merged = writer.mergeHeaders(
        List.of("UUID", "source", "old"),
        List.of("UUID", "source", "new"));

    assertEquals(List.of("UUID", "source", "old", "new"), merged);
  }

  @Test
  void printCanBeDisabled() {
    Api api = new TestObjects.SimpleApi("DemoApi", new String[] { "a" });
    CsvOutputFileWriter writer = new CsvOutputFileWriter(List.of(api), tempDir.resolve("out2").toString(), true);
    writer.writeBatch("demoapi", List.of(new TestObjects.SimpleObject(new String[] { "a" }, new String[] { "v" })));

    writer.printOutput(null);
    assertTrue(Files.exists(tempDir.resolve("out2.csv")));
  }
}
