package com.voronina.course.chatsapi;

import java.io.IOException;

import com.voronina.course.Api;
import com.voronina.course.ApiObject;

public class ChatsApi implements Api {
  private static final String API_URL = "http://localhost:8000/chats";

  public String name() {
    return "ChatsApi";
  }

  @Override
  public ApiObject[] fetchData() throws IOException, InterruptedException {
    java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
    java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(API_URL))
        .GET()
        .build();

    java.net.http.HttpResponse<String> response = client.send(request,
        java.net.http.HttpResponse.BodyHandlers.ofString());

    com.google.gson.Gson gson = new com.google.gson.GsonBuilder().serializeNulls().create();
    ResponseWrapper wrapper = gson.fromJson(response.body(), ResponseWrapper.class);
    if (wrapper == null || wrapper.chats == null || wrapper.chats.length == 0) {
      throw new IllegalStateException("No chats returned from API");
    }
    return wrapper.chats;
  }

  // Minimal wrapper matching the chats API response
  private static class ResponseWrapper {
    int total;
    Chat[] chats;
  }
}
