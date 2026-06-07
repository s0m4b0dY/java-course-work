package com.voronina.course;

import io.github.cdimascio.dotenv.Dotenv;

import java.nio.file.Path;

public class EnvFile {
  public static String get(String key) {
    return get(key, Path.of("."));
  }

  static String get(String key, Path dir) {
    if (key == null || key.isBlank()) {
      return "";
    }

    String realEnv = System.getenv(key);
    if (realEnv != null && !realEnv.isBlank()) {
      System.out.println("Debug: read " + key + " from system environment");
      return realEnv;
    }

    try {
      Dotenv dotenv = Dotenv.configure()
          .directory(dir.toString())
          .ignoreIfMissing()
          .load();

      String value = dotenv.get(key);
      if (value != null && !value.isBlank()) {
        System.out.println("Debug: read " + key + " from .env file");
        return value;
      }
    } catch (Exception ex) {
      System.out.println("Debug: could not read .env: " + ex.getMessage());
    }

    return "";
  }

  public static String getFirst(String... keys) {
    return getFirst(Path.of("."), keys);
  }

  static String getFirst(Path dir, String... keys) {
    if (keys == null) {
      return "";
    }

    for (String key : keys) {
      String value = get(key, dir);
      if (value != null && !value.isBlank()) {
        return value;
      }
    }

    return "";
  }
}
