package com.voronina.course.freepikapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FreepikIconTest {
  @Test
  void jsonAndCsvConversionWorks() {
    String json = """
        {
          "id":5,
          "name":"home",
          "slug":"home-icon",
          "free_svg":true,
          "created":"2024-01-01",
          "style":{"id":1,"name":"solid"},
          "family":{"id":2,"name":"basic","total":10},
          "author":{"id":3,"name":"Alice","slug":"alice","avatar":"avatar.png","assets":99},
          "tags":[{"name":"house","slug":"house"},{"name":"building","slug":"building"}],
          "thumbnails":[{"width":10,"height":20,"url":"url1"},{"width":30,"height":40,"url":"url2"}]
        }
        """;

    FreepikIcon icon = FreepikIcon.fromJson(json);
    String[] fields = icon.toCsvFields();

    assertEquals(5, icon.getId());
    assertEquals("home", icon.getName());
    assertEquals("home-icon", icon.getSlug());
    assertTrue(icon.isFree_svg());
    assertEquals("2024-01-01", icon.getCreated());
    assertEquals(1, icon.getStyle().getId());
    assertEquals("solid", icon.getStyle().getName());
    assertEquals(2, icon.getFamily().getId());
    assertEquals("basic", icon.getFamily().getName());
    assertEquals(10, icon.getFamily().getTotal());
    assertEquals(3, icon.getAuthor().getId());
    assertEquals("Alice", icon.getAuthor().getName());
    assertEquals("alice", icon.getAuthor().getSlug());
    assertEquals("avatar.png", icon.getAuthor().getAvatar());
    assertEquals(99, icon.getAuthor().getAssets());
    assertEquals("house", icon.getTags().get(0).getName());
    assertEquals("house", icon.getTags().get(0).getSlug());
    assertEquals(10, icon.getThumbnails().get(0).getWidth());
    assertEquals(20, icon.getThumbnails().get(0).getHeight());
    assertEquals("url1", icon.getThumbnails().get(0).getUrl());

    assertEquals("5", fields[0]);
    assertEquals("home", fields[1]);
    assertEquals("true", fields[3]);
    assertEquals("house;building", fields[15]);
    assertEquals("url1;url2", fields[16]);
    assertArrayEquals(FreepikIcon.CSV_HEADERS, icon.csvHeaders());
    assertNotNull(icon.toGson());
    assertTrue(icon.toJson().contains("home"));
  }

  @Test
  void settersWorkForMainFields() {
    FreepikIcon icon = new FreepikIcon();
    icon.setId(9);
    icon.setName("name");
    icon.setSlug("slug");
    icon.setFree_svg(true);
    icon.setCreated("today");
    icon.setStyle(null);
    icon.setFamily(null);
    icon.setAuthor(null);
    icon.setTags(null);
    icon.setThumbnails(null);

    assertEquals(9, icon.getId());
    assertEquals("name", icon.getName());
    assertEquals("slug", icon.getSlug());
    assertTrue(icon.isFree_svg());
    assertEquals("today", icon.getCreated());
    assertNull(icon.getStyle());
    assertNull(icon.getFamily());
    assertNull(icon.getAuthor());
    assertNull(icon.getTags());
    assertNull(icon.getThumbnails());
  }
}
