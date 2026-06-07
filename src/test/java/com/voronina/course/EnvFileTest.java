package com.voronina.course;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EnvFileTest {
  @TempDir
  Path tempDir;

  @Test
  void readsValueFromDotenvFile() throws Exception {
    Files.writeString(tempDir.resolve(".env"), "COURSE_TEST_DOTENV_KEY=secret-value\n");

    assertEquals("secret-value", EnvFile.get("COURSE_TEST_DOTENV_KEY", tempDir));
  }

  @Test
  void getFirstReturnsFirstPresentValue() throws Exception {
    Files.writeString(tempDir.resolve(".env"), "SECOND_TEST_KEY=hello\n");

    assertEquals("hello", EnvFile.getFirst(tempDir, "FIRST_TEST_KEY", "SECOND_TEST_KEY"));
  }

  @Test
  void missingValueReturnsEmptyString() {
    assertEquals("", EnvFile.get("VERY_MISSING_KEY_FOR_TESTS", tempDir));
  }
}
