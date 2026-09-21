package com.voronina.course;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RealHttpSender implements HttpSender {
  private final HttpClient client;

  public RealHttpSender() {
    client = HttpClient.newHttpClient();
  }

  @Override
  public HttpResult send(HttpRequest request) throws IOException, InterruptedException {
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    return new HttpResult(response.statusCode(), response.body());
  }
}
