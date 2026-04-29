package com.voronina.course.freepikapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.voronina.course.ApiObject;

import java.util.List;
import java.util.stream.Collectors;

public class FreepikIcon implements ApiObject {
  static final String[] CSV_HEADERS = {
      "id",
      "name",
      "slug",
      "free_svg",
      "created",
      "style_id",
      "style_name",
      "family_id",
      "family_name",
      "family_total",
      "author_id",
      "author_name",
      "author_slug",
      "author_avatar",
      "author_assets",
      "tags",
      "thumbnails"
  };

  private int id;
  private String name;
  private String slug;
  private boolean free_svg;
  private String created;
  private Style style;
  private Family family;
  private Author author;
  private List<Tag> tags;
  private List<Thumbnail> thumbnails;

  // ---- Nested classes ----

  public static class Style {
    private int id;
    private String name;
    public int getId() { return id; }
    public String getName() { return name; }
  }

  public static class Family {
    private int id;
    private String name;
    private int total;
    public int getId() { return id; }
    public String getName() { return name; }
    public int getTotal() { return total; }
  }

  public static class Author {
    private int id;
    private String name;
    private String slug;
    private String avatar;
    private int assets;
    public int getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getAvatar() { return avatar; }
    public int getAssets() { return assets; }
  }

  public static class Tag {
    private String name;
    private String slug;
    public String getName() { return name; }
    public String getSlug() { return slug; }
  }

  public static class Thumbnail {
    private int width;
    private int height;
    private String url;
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getUrl() { return url; }
  }

  // ---- ApiObject ----

  @Override
  public String[] toCsvFields() {
    String tagNames = tags != null
        ? tags.stream().map(t -> t.getName() != null ? t.getName() : "").collect(Collectors.joining(";"))
        : "";
    String thumbUrls = thumbnails != null
        ? thumbnails.stream().map(t -> t.getUrl() != null ? t.getUrl() : "").collect(Collectors.joining(";"))
        : "";

    return new String[] {
        String.valueOf(id),
        safe(name),
        safe(slug),
        String.valueOf(free_svg),
        safe(created),
        style != null ? String.valueOf(style.getId()) : "",
        style != null ? safe(style.getName()) : "",
        family != null ? String.valueOf(family.getId()) : "",
        family != null ? safe(family.getName()) : "",
        family != null ? String.valueOf(family.getTotal()) : "",
        author != null ? String.valueOf(author.getId()) : "",
        author != null ? safe(author.getName()) : "",
        author != null ? safe(author.getSlug()) : "",
        author != null ? safe(author.getAvatar()) : "",
        author != null ? String.valueOf(author.getAssets()) : "",
        tagNames,
        thumbUrls
    };
  }

  @Override
  public String[] csvHeaders() {
    return CSV_HEADERS;
  }

  @Override
  public Gson toGson() {
    return new GsonBuilder().serializeNulls().create();
  }

  // ---- JSON convenience ----

  public static FreepikIcon fromJson(String json) {
    return new GsonBuilder().create().fromJson(json, FreepikIcon.class);
  }

  public String toJson() {
    return new GsonBuilder().create().toJson(this);
  }

  // ---- Getters / Setters ----

  public int getId() { return id; }
  public void setId(int id) { this.id = id; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getSlug() { return slug; }
  public void setSlug(String slug) { this.slug = slug; }

  public boolean isFree_svg() { return free_svg; }
  public void setFree_svg(boolean free_svg) { this.free_svg = free_svg; }

  public String getCreated() { return created; }
  public void setCreated(String created) { this.created = created; }

  public Style getStyle() { return style; }
  public void setStyle(Style style) { this.style = style; }

  public Family getFamily() { return family; }
  public void setFamily(Family family) { this.family = family; }

  public Author getAuthor() { return author; }
  public void setAuthor(Author author) { this.author = author; }

  public List<Tag> getTags() { return tags; }
  public void setTags(List<Tag> tags) { this.tags = tags; }

  public List<Thumbnail> getThumbnails() { return thumbnails; }
  public void setThumbnails(List<Thumbnail> thumbnails) { this.thumbnails = thumbnails; }

  private static String safe(String v) { return v != null ? v : ""; }
}
