package com.voronina.course.randomuserapi;

import java.io.IOException;

import com.voronina.course.Api;
import com.voronina.course.ApiObject;

public class RandomUserApi implements Api {
  private static final String API_URL = "https://randomuser.me/api/";

  @Override
  public ApiObject fetchData() throws IOException, InterruptedException {
    java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
    java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(API_URL))
        .GET()
        .build();

    java.net.http.HttpResponse<String> response = client.send(request,
        java.net.http.HttpResponse.BodyHandlers.ofString());
    com.google.gson.Gson gson = new com.google.gson.GsonBuilder().serializeNulls().create();
    ResponseWrapper wrapper = gson.fromJson(response.body(), ResponseWrapper.class);
    return wrapper.results[0];
  }

  // Minimal wrapper matching randomuser.me response
  private static class ResponseWrapper {
    RandomUser[] results;
  }
}
