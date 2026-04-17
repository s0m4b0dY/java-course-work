package com.voronina.course.lastmessageapi;

import java.io.IOException;

import com.voronina.course.Api;
import com.voronina.course.ApiObject;

public class LastMessageApi implements Api {
  private static final String API_URL = "http://localhost:8000/last_message";
  
  public String name() {
    return "LastMessageApi";
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
    LastMessage msg = gson.fromJson(response.body(), LastMessage.class);
    return new LastMessage[] { msg };
  }
}
