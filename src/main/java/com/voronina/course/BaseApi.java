package com.voronina.course;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public abstract class BaseApi implements Api {
  private final String apiName;
  protected final Gson gson;
  protected final HttpClient client;

  protected BaseApi(String apiName) {
    this(apiName, HttpClient.newHttpClient());
  }

  protected BaseApi(String apiName, HttpClient client) {
    this.apiName = apiName;
    this.client = client;
    this.gson = new GsonBuilder().serializeNulls().create();
  }

  @Override
  public String name() {
    return apiName;
  }

  protected String getBody(String url) throws IOException, InterruptedException {
    HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).GET();
    fillHeaders(builder);

    HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException(apiName + " HTTP error: " + response.statusCode() + " — " + response.body());
    }

    return response.body();
  }

  protected void fillHeaders(HttpRequest.Builder builder) {
    // Child APIs can add keys here.
  }

  protected <T> T parseJson(String json, Class<T> type) {
    return gson.fromJson(json, type);
  }
}
