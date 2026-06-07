package com.voronina.course;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ApiPollingManager implements AutoCloseable {
  private final List<Api> apis;
  private final PollingConfig config;
  private final ThreadSafeOutputWriter outputWriter;

  private final ScheduledExecutorService scheduler;
  private final ExecutorService workers;

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final Map<String, AtomicInteger> fetchedPerApi = new ConcurrentHashMap<>();

  private final CountDownLatch finishedLatch;

  public ApiPollingManager(
      List<Api> apis,
      PollingConfig config,
      ThreadSafeOutputWriter outputWriter
  ) {
    this.apis = new ArrayList<>(apis);
    this.config = config;
    this.outputWriter = outputWriter;

    this.scheduler = Executors.newScheduledThreadPool(Math.max(1, apis.size()));
    this.workers = Executors.newFixedThreadPool(config.maxConcurrentTasks());

    this.finishedLatch = new CountDownLatch(apis.size());

    for (Api api : apis) {
      fetchedPerApi.put(api.name(), new AtomicInteger(0));
    }
  }

  public boolean isRunning() {
    return running.get();
  }

  public void start() {
    if (!running.compareAndSet(false, true)) {
      return;
    }

    System.out.println("Polling started.");
    System.out.println("Max concurrent tasks: " + config.maxConcurrentTasks());
    System.out.println("Interval after each completed request: " + config.intervalSeconds() + " seconds");

    for (Api api : apis) {
      scheduleNext(api, 0);
    }
  }

  public void stop() {
    if (!running.compareAndSet(true, false)) {
      return;
    }

    System.out.println("Stopping polling...");

    scheduler.shutdownNow();
    workers.shutdown();

    try {
      if (!workers.awaitTermination(10, TimeUnit.SECONDS)) {
        workers.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      workers.shutdownNow();
    }

    System.out.println("Polling stopped.");
  }

  public void awaitCompletion() {
    try {
      finishedLatch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void scheduleNext(Api api, long delaySeconds) {
    if (!running.get()) {
      return;
    }

    if (isApiFinished(api)) {
      finishedLatch.countDown();
      return;
    }

    scheduler.schedule(() -> {
      if (!running.get()) {
        return;
      }

      workers.submit(() -> pollOnce(api));

    }, delaySeconds, TimeUnit.SECONDS);
  }

  private void pollOnce(Api api) {
    String apiName = api.name();

    try {
      if (!running.get()) {
        return;
      }

      if (isApiFinished(api)) {
        finishedLatch.countDown();
        return;
      }

      System.out.println("Fetching from API: " + apiName);

      ApiObject[] result = api.fetchData();

      if (result == null || result.length == 0) {
        System.out.println("API returned no data: " + apiName);
      } else {
        List<ApiObject> objects = limitObjectsIfNeeded(apiName, Arrays.asList(result));

        if (!objects.isEmpty()) {
          outputWriter.writeBatch(sanitizeName(apiName), objects);
          int total = fetchedPerApi.get(apiName).addAndGet(objects.size());

          System.out.println("Fetched " + objects.size() + " objects from " + apiName
              + ". Total: " + total);
        }
      }
    } catch (IOException e) {
      System.out.println("IOException from " + apiName + ": " + e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.out.println("Interrupted while fetching from " + apiName);
    } catch (RuntimeException e) {
      System.out.println("Unexpected error from " + apiName + ": " + e.getMessage());
    } finally {
      if (running.get() && !isApiFinished(api)) {
        /*
         * Important:
         * The next request for the same API is scheduled only after this request
         * has fully finished. This satisfies the requirement:
         *
         * "Повторный опрос одного и того же API должен происходить не чаще,
         * чем через t секунд после завершения предыдущего запроса"
         */
        scheduleNext(api, config.intervalSeconds());
      } else {
        finishedLatch.countDown();
      }
    }
  }

  private List<ApiObject> limitObjectsIfNeeded(String apiName, List<ApiObject> objects) {
    if (config.isInfinite()) {
      return objects;
    }

    int alreadyFetched = fetchedPerApi.get(apiName).get();
    int remaining = config.maxObjectsPerApi() - alreadyFetched;

    if (remaining <= 0) {
      return List.of();
    }

    if (objects.size() <= remaining) {
      return objects;
    }

    return objects.subList(0, remaining);
  }

  private boolean isApiFinished(Api api) {
    if (config.isInfinite()) {
      return false;
    }

    AtomicInteger counter = fetchedPerApi.get(api.name());
    return counter != null && counter.get() >= config.maxObjectsPerApi();
  }

  private static String sanitizeName(String name) {
    if (name == null) {
      return "api";
    }

    return name.replaceAll("[^A-Za-z0-9_-]", "_").toLowerCase();
  }

  @Override
  public void close() {
    stop();
    outputWriter.close();
  }
}