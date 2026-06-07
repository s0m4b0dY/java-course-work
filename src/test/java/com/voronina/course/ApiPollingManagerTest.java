package com.voronina.course;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiPollingManagerTest {
  @TempDir
  Path tempDir;

  @Test
  void pollingStopsAfterRequiredCount() throws Exception {
    ApiObject one = new TestObjects.SimpleObject(new String[] { "a" }, new String[] { "1" });
    ApiObject two = new TestObjects.SimpleObject(new String[] { "a" }, new String[] { "2" });
    Api api = new TestObjects.SimpleApi("DemoApi", new String[] { "a" },
        new ApiObject[] { one, two },
        new ApiObject[] { one, two });

    ThreadSafeOutputWriter writer = new ThreadSafeOutputWriter(
        List.of(api),
        OutputFileFormat.JSON,
        tempDir.resolve("polling").toString(),
        true);

    ApiPollingManager manager = new ApiPollingManager(
        List.of(api),
        new PollingConfig(1, 0, 3),
        writer);

    manager.start();
    manager.awaitCompletion();
    manager.stop();

    String text = Files.readString(tempDir.resolve("polling.json"));
    assertEquals(3, com.google.gson.JsonParser.parseString(text).getAsJsonArray().size());
  }
}
