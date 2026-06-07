package com.voronina.course;

import com.voronina.course.emojisapi.EmojiApi;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiRegistryTest {
  @Test
  void createsKnownApiIgnoringCase() {
    assertTrue(ApiRegistry.create("EMOJI") instanceof EmojiApi);
    assertNull(ApiRegistry.create("missing"));
  }

  @Test
  void mainCreatesAllOrSelectedApis() {
    assertEquals(3, Main.createApis("").size());
    List<Api> selected = Main.createApis("emoji,unknown");
    assertEquals(1, selected.size());
    assertEquals("EmojiApi", selected.get(0).name());
  }
}
