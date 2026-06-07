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
}
