package com.voronina.course;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleGuiTest {
  @Test
  void interactiveModeCanBeCancelledBeforeStart() {
    String input = String.join("\n",
        "1",
        "csv",
        "append",
        "myfile",
        "bad-number",
        "bad-threads",
        "bad-interval",
        "none",
        "no") + "\n";

    var oldIn = System.in;
    var oldOut = System.out;
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    try {
      System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
      System.setOut(new PrintStream(output));

      new ConsoleGui().run();
    } finally {
      System.setIn(oldIn);
      System.setOut(oldOut);
    }

    String text = output.toString(StandardCharsets.UTF_8);
    assertTrue(text.contains("Interactive mode"));
    assertTrue(text.contains("Invalid number, using 50"));
    assertTrue(text.contains("Polling was not started."));
  }
}
