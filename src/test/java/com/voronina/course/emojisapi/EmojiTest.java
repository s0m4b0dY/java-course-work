package com.voronina.course.emojisapi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmojiTest {
  @Test
  void jsonAndCsvConversionWorks() {
    Emoji emoji = Emoji.fromJson("{\"name\":\"grin\",\"category\":\"smileys\",\"group\":\"face\",\"htmlCode\":[\"&#128512;\"],\"unicode\":[\"U+1F600\"]}");

    assertEquals("grin", emoji.getName());
    assertEquals("smileys", emoji.getCategory());
    assertEquals("face", emoji.getGroup());
    assertEquals(List.of("&#128512;"), emoji.getHtmlCode());
    assertEquals(List.of("U+1F600"), emoji.getUnicode());
    assertTrue(emoji.toJson().contains("grin"));

    String[] fields = emoji.toCsvFields();
    assertArrayEquals(new String[] { "grin", "smileys", "face", "&#128512;", "U+1F600" }, fields);
    assertArrayEquals(Emoji.CSV_HEADERS, emoji.csvHeaders());
    assertNotNull(emoji.toGson());
  }

  @Test
  void settersSetValues() {
    Emoji emoji = new Emoji();
    emoji.setName("n");
    emoji.setCategory("c");
    emoji.setGroup("g");
    emoji.setHtmlCode(List.of("h"));
    emoji.setUnicode(List.of("u"));

    assertEquals("n", emoji.getName());
    assertEquals("c", emoji.getCategory());
    assertEquals("g", emoji.getGroup());
    assertEquals(List.of("h"), emoji.getHtmlCode());
    assertEquals(List.of("u"), emoji.getUnicode());
  }
}
