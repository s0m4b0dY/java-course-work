package com.voronina.course;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SourceNames {
  public static String sanitize(String name) {
    if (name == null) {
      return "api";
    }

    return name.replaceAll("[^A-Za-z0-9_-]", "_").toLowerCase();
  }

  public static Map<String, String> buildPrintAliases(List<Api> apis) {
    Map<String, String> aliases = new LinkedHashMap<>();

    for (Api api : apis) {
      String apiName = sanitize(api.name());
      aliases.put(apiName, apiName);

      // RandomUserApi can be printed as randomuser too.
      if (apiName.endsWith("api")) {
        aliases.put(apiName.substring(0, apiName.length() - 3), apiName);
      }
    }

    return aliases;
  }

  public static List<String> parseRequestedSources(String apiToPrint, Map<String, String> aliases) {
    List<String> result = new ArrayList<>();

    if (apiToPrint == null || apiToPrint.isBlank()) {
      return result;
    }

    for (String raw : apiToPrint.split(",")) {
      String key = sanitize(raw.trim());

      if (key.isBlank()) {
        continue;
      }

      String resolved = aliases.getOrDefault(key, key);
      if (!result.contains(resolved)) {
        result.add(resolved);
      }
    }

    return result;
  }
}
