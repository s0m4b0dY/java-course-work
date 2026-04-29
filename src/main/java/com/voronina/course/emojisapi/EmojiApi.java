package com.voronina.course.emojisapi;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.GsonBuilder;
import com.voronina.course.Api;
import com.voronina.course.ApiObject;

public class EmojiApi implements Api {
  private static final String API_URL = "https://emojihub.yurace.pro/api/random";

  @Override
  public String name() {
    return "EmojiApi";
  }

  @Override
  public ApiObject[] fetchData() throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
        .GET()
        .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    Emoji emoji = new GsonBuilder().serializeNulls().create().fromJson(response.body(), Emoji.class);
    if (emoji == null) {
      throw new IllegalStateException("No emoji returned from API");
    }
    return new ApiObject[]{ emoji };
  }
}
