package com.voronina.course;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SourceNamesTest {
  @Test
  void sanitizeMakesStableSimpleNames() {
    assertEquals("randomuserapi", SourceNames.sanitize("RandomUserApi"));
    assertEquals("bad_name_", SourceNames.sanitize("Bad Name!"));
    assertEquals("api", SourceNames.sanitize(null));
  }

  @Test
  void aliasesAllowApiSuffixToBeSkipped() {
    Api api = new TestObjects.SimpleApi("EmojiApi", new String[] { "name" });
    Map<String, String> aliases = SourceNames.buildPrintAliases(List.of(api));

    assertEquals(List.of("emojiapi"), SourceNames.parseRequestedSources("emoji", aliases));
  }
}
