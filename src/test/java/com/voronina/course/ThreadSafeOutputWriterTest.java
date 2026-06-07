package com.voronina.course;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThreadSafeOutputWriterTest {
  @TempDir
  Path tempDir;

  @Test
  void delegatesToCsvWriter() {
    Api api = new TestObjects.SimpleApi("DemoApi", new String[] { "a" });
    ThreadSafeOutputWriter writer = new ThreadSafeOutputWriter(List.of(api), OutputFileFormat.CSV, tempDir.resolve("safe").toString(), true);

    writer.writeBatch("demoapi", List.of(new TestObjects.SimpleObject(new String[] { "a" }, new String[] { "hello" })));

    assertTrue(Files.exists(tempDir.resolve("safe.csv")));
  }
}
