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

  @Test
  void parseRequestedSourcesHandlesBlankUnknownAndDuplicates() {
    Map<String, String> aliases = Map.of("emoji", "emojiapi");

    assertTrue(SourceNames.parseRequestedSources(null, aliases).isEmpty());
    assertTrue(SourceNames.parseRequestedSources("   ", aliases).isEmpty());
    assertEquals(
        List.of("emojiapi", "unknown"),
        SourceNames.parseRequestedSources("emoji, emoji, unknown", aliases));
  }
}
