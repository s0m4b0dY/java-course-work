package com.voronina.course;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PollingConfigTest {
  @Test
  void constructorFixesTooSmallNumbers() {
    PollingConfig config = new PollingConfig(0, -5, 0);

    assertEquals(1, config.maxConcurrentTasks());
    assertEquals(0, config.intervalSeconds());
    assertTrue(config.isInfinite());
  }

  @Test
  void positiveCountIsNotInfinite() {
    PollingConfig config = new PollingConfig(2, 1, 5);

    assertEquals(5, config.maxObjectsPerApi());
    assertFalse(config.isInfinite());
  }
}
