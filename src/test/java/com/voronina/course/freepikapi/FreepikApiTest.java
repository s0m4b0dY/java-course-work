package com.voronina.course.freepikapi;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FreepikApiTest {
  @Test
  void emptyApiKeyFailsBeforeHttpRequest() {
    FreepikApi api = new FreepikApi("");

    IOException ex = assertThrows(IOException.class, api::fetchData);
    assertTrue(ex.getMessage().contains("FREEPIK_API_KEY"));
  }

  @Test
  void nameAndHeadersAreAvailable() {
    FreepikApi api = new FreepikApi("test-key");

    assertEquals("FreepikApi", api.name());
    assertArrayEquals(FreepikIcon.CSV_HEADERS, api.csvHeaders());
  }
}
