package com.voronina.course;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    assertFalse(manager.isRunning());
  }

  @Test
  void completionWaitsForEveryApi() throws Exception {
    ApiObject objectA = new TestObjects.SimpleObject(new String[] { "value" }, new String[] { "a" });
    ApiObject objectB = new TestObjects.SimpleObject(new String[] { "value" }, new String[] { "b" });

    Api first = new TestObjects.SimpleApi("FirstApi", new String[] { "value" },
        new ApiObject[] { objectA });
    Api second = new TestObjects.SimpleApi("SecondApi", new String[] { "value" },
        new ApiObject[] { objectB });

    ThreadSafeOutputWriter writer = new ThreadSafeOutputWriter(
        List.of(first, second), OutputFileFormat.JSON,
        tempDir.resolve("two-apis").toString(), true);

    ApiPollingManager manager = new ApiPollingManager(
        List.of(first, second), new PollingConfig(2, 0, 1), writer);

    manager.start();
    assertTimeoutPreemptively(Duration.ofSeconds(3), manager::awaitCompletion);
    manager.stop();

    String text = Files.readString(tempDir.resolve("two-apis.json"));
    assertEquals(2, com.google.gson.JsonParser.parseString(text).getAsJsonArray().size());
  }

  @Test
  void stopReleasesLatchInInfiniteMode() {
    Api api1 = new TestObjects.SimpleApi("OneApi", new String[] { "a" });
    Api api2 = new TestObjects.SimpleApi("TwoApi", new String[] { "a" });
    ThreadSafeOutputWriter writer = new ThreadSafeOutputWriter(
        List.of(api1, api2), OutputFileFormat.JSON,
        tempDir.resolve("infinite").toString(), true);

    ApiPollingManager manager = new ApiPollingManager(
        List.of(api1, api2), new PollingConfig(1, 60, 0), writer);

    manager.start();
    manager.start(); // second start should simply do nothing
    manager.stop();

    assertFalse(manager.isRunning());
    assertTimeoutPreemptively(Duration.ofSeconds(1), manager::awaitCompletion);
    manager.stop(); // stopping an already stopped manager is also harmless
  }

  @Test
  void ioFailureIsRetriedAndLaterSuccessfulResultIsWritten() throws Exception {
    ApiObject object = new TestObjects.SimpleObject(new String[] { "value" }, new String[] { "ok" });
    AtomicInteger calls = new AtomicInteger();

    Api api = new Api() {
      @Override
      public String name() {
        return "RetryApi";
      }

      @Override
      public String[] csvHeaders() {
        return new String[] { "value" };
      }

      @Override
      public ApiObject[] fetchData() throws IOException {
        if (calls.getAndIncrement() == 0) {
          throw new IOException("temporary problem");
        }
        return new ApiObject[] { object };
      }
    };

    ThreadSafeOutputWriter writer = new ThreadSafeOutputWriter(
        List.of(api), OutputFileFormat.JSON,
        tempDir.resolve("retry").toString(), true);
    ApiPollingManager manager = new ApiPollingManager(
        List.of(api), new PollingConfig(1, 0, 1), writer);

    manager.start();
    assertTimeoutPreemptively(Duration.ofSeconds(3), manager::awaitCompletion);
    manager.stop();

    assertTrue(calls.get() >= 2);
    String text = Files.readString(tempDir.resolve("retry.json"));
    assertEquals(1, com.google.gson.JsonParser.parseString(text).getAsJsonArray().size());
  }

  @Test
  void emptyResultCanBeStoppedCleanly() throws Exception {
    CountDownLatch called = new CountDownLatch(1);
    Api api = new Api() {
      @Override
      public String name() {
        return "EmptyApi";
      }

      @Override
      public String[] csvHeaders() {
        return new String[] { "value" };
      }

      @Override
      public ApiObject[] fetchData() {
        called.countDown();
        return new ApiObject[0];
      }
    };

    ThreadSafeOutputWriter writer = new ThreadSafeOutputWriter(
        List.of(api), OutputFileFormat.JSON,
        tempDir.resolve("empty").toString(), true);
    ApiPollingManager manager = new ApiPollingManager(
        List.of(api), new PollingConfig(1, 60, 0), writer);

    manager.start();
    assertTrue(called.await(2, TimeUnit.SECONDS));
    manager.stop();

    assertTimeoutPreemptively(Duration.ofSeconds(1), manager::awaitCompletion);
  }
}
