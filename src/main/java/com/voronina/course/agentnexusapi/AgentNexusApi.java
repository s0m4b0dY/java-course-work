package com.voronina.course.agentnexusapi;

import com.voronina.course.ApiObject;
import com.voronina.course.BaseApi;
import com.voronina.course.HttpSender;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AgentNexusApi extends BaseApi {
  private static final String BASE_URL = "https://agentnexus.app/api/public/discover";
  private static final int LIMIT = 5;

  // Different searches make polling return different kinds of registry entries.
  private static final String[] SEARCH_ITEMS = {
      "send a transactional email",
      "process online payments",
      "manage github repositories and pull requests",
      "query a postgres database",
      "transcode a video file",
      "monitor application errors",
      "manage dns records",
      "send sms messages"
  };

  private int currentSearchIndex = 0;

  public AgentNexusApi() {
    super("AgentNexusApi");
  }

  AgentNexusApi(HttpSender httpSender) {
    super("AgentNexusApi", httpSender);
  }

  @Override
  public String[] csvHeaders() {
    return AgentNexusEntry.CSV_HEADERS;
  }

  @Override
  public ApiObject[] fetchData() throws IOException, InterruptedException {
    String search = SEARCH_ITEMS[currentSearchIndex];
    String encodedSearch = URLEncoder.encode(search, StandardCharsets.UTF_8);
    String url = BASE_URL + "?need=" + encodedSearch + "&limit=" + LIMIT;

    String body = getBody(url);
    ResponseWrapper wrapper = parseJson(body, ResponseWrapper.class);

    if (wrapper == null || wrapper.matches == null || wrapper.matches.length == 0) {
      throw new IllegalStateException("No matches returned from AgentNexusApi for search: " + search);
    }

    for (AgentNexusEntry entry : wrapper.matches) {
      if (entry != null) {
        entry.setSearchNeed(search);
      }
    }

    currentSearchIndex = (currentSearchIndex + 1) % SEARCH_ITEMS.length;
    return wrapper.matches;
  }

  @SuppressWarnings("unused")
  private static class ResponseWrapper {
    String need;
    String coverage;
    int count;
    AgentNexusEntry[] matches;
  }
}
