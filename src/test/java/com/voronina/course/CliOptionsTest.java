package com.voronina.course;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CliOptionsTest {
  @Test
  void parseAutoCsvAppendAndNumbers() {
    CliOptions options = CliOptions.parse(new String[] {
        "--auto",
        "--format=csv",
        "--output=result",
        "--append",
        "--apis=randomuser,emoji",
        "--print-apis=emoji",
        "--count=15",
        "--threads=3",
        "--interval=2"
    });

    assertTrue(options.runAuto);
    assertEquals(OutputFileFormat.CSV, options.format);
    assertEquals("result", options.outputName);
    assertFalse(options.overwrite);
    assertEquals("randomuser,emoji", options.apisArg);
    assertEquals("emoji", options.apiToPrint);
    assertEquals(15, options.objectsCount);
    assertEquals(3, options.maxConcurrentTasks);
    assertEquals(2, options.intervalSeconds);
  }

  @Test
  void invalidNumbersKeepDefaultValues() {
    CliOptions options = CliOptions.parse(new String[] {
        "--count=nope",
        "--threads=nope",
        "--interval=nope"
    });

    assertEquals(50, options.objectsCount);
    assertEquals(2, options.maxConcurrentTasks);
    assertEquals(5, options.intervalSeconds);
  }

  @Test
  void nullArgsKeepDefaults() {
    CliOptions options = CliOptions.parse(null);

    assertFalse(options.runAuto);
    assertEquals(OutputFileFormat.JSON, options.format);
    assertEquals("output", options.outputName);
    assertTrue(options.overwrite);
    assertEquals(50, options.objectsCount);
  }

  @Test
  void shortFlagsAndNoOverwriteAreSupported() {
    CliOptions options = CliOptions.parse(new String[] {
        "-a",
        "--no-overwrite",
        "-n=4",
        "-t=3",
        "--format=something-else"
    });

    assertTrue(options.runAuto);
    assertFalse(options.overwrite);
    assertEquals(4, options.maxConcurrentTasks);
    assertEquals(3, options.intervalSeconds);
    assertEquals(OutputFileFormat.JSON, options.format);
  }
}
