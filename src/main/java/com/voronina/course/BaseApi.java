package com.voronina.course;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;

public abstract class BaseApi implements Api {
  private final String apiName;
  protected final Gson gson;
  protected final HttpSender httpSender;

  protected BaseApi(String apiName) {
    this(apiName, new RealHttpSender());
  }

  protected BaseApi(String apiName, HttpSender httpSender) {
    this.apiName = apiName;
    this.httpSender = httpSender;
    this.gson = new GsonBuilder().serializeNulls().create();
  }

  @Override
  public String name() {
    return apiName;
  }

  protected String getBody(String url) throws IOException, InterruptedException {
    HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).GET();
    fillHeaders(builder);

    HttpResult response = httpSender.send(builder.build());
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
