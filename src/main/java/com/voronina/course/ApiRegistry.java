package com.voronina.course;

import com.voronina.course.emojisapi.EmojiApi;
import com.voronina.course.freepikapi.FreepikApi;
import com.voronina.course.randomuserapi.RandomUserApi;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central registry of all available API implementations.
 * To add a new API: add one entry to REGISTRY below — nothing else needs to change.
 */
public class ApiRegistry {

  /** Ordered map: display-name → factory lambda */
  private static final Map<String, ApiEntry> REGISTRY = new LinkedHashMap<>();

  static {
    register("randomuser", "RandomUserApi", RandomUserApi::new);
    register("emoji",      "EmojiApi",      EmojiApi::new);
    register("freepik",    "FreepikApi",    FreepikApi::new);
  }

  // -------------------------------------------------------------------------

  public record ApiEntry(String key, String displayName, ApiFactory factory) {}

  @FunctionalInterface
  public interface ApiFactory {
    Api create();
  }

  private static void register(String key, String displayName, ApiFactory factory) {
    REGISTRY.put(key.toLowerCase(), new ApiEntry(key.toLowerCase(), displayName, factory));
  }

  /** All registered entries in insertion order. */
  public static Map<String, ApiEntry> all() {
    return java.util.Collections.unmodifiableMap(REGISTRY);
  }

  /**
   * Create an Api instance by key (case-insensitive).
   * Returns null if the key is unknown.
   */
  public static Api create(String key) {
    if (key == null) return null;
    ApiEntry entry = REGISTRY.get(key.trim().toLowerCase());
    return entry != null ? entry.factory().create() : null;
  }
}
