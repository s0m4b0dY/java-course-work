package com.voronina.course;

public class PollingConfig {
  private final int maxConcurrentTasks;
  private final long intervalSeconds;
  private final int maxObjectsPerApi;

  public PollingConfig(int maxConcurrentTasks, long intervalSeconds, int maxObjectsPerApi) {
    this.maxConcurrentTasks = Math.max(1, maxConcurrentTasks);
    this.intervalSeconds = Math.max(0, intervalSeconds);
    this.maxObjectsPerApi = maxObjectsPerApi;
  }

  public int getMaxConcurrentTasks() {
    return maxConcurrentTasks;
  }

  public long getIntervalSeconds() {
    return intervalSeconds;
  }

  public int getMaxObjectsPerApi() {
    return maxObjectsPerApi;
  }

  public boolean isInfinite() {
    return maxObjectsPerApi <= 0;
  }
}