package com.voronina.course.lastmessageapi;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.net.URI;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.voronina.course.Api;
import com.voronina.course.ApiObject;
import com.voronina.course.ObjectObtainedCallback;

public class LastMessageApi implements Api {
  private static final String API_URL = "http://localhost:8000/last_message";

  private ObjectObtainedCallback callback;

  public LastMessageApi() {
  }

  public LastMessageApi(ObjectObtainedCallback callback) {
    this.callback = callback;
  }

  public void setCallback(ObjectObtainedCallback callback) {
    this.callback = callback;
  }

  @Override
  public ApiObject fetchData() throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL)).GET().build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    Gson gson = new GsonBuilder().serializeNulls().create();
    LastMessage msg = gson.fromJson(response.body(), LastMessage.class);
    if (msg != null && callback != null) {
      callback.onObjectObtained(msg);
    }
    return msg;

  }
}
