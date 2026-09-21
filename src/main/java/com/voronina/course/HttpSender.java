package com.voronina.course;

import java.io.IOException;
import java.net.http.HttpRequest;

public interface HttpSender {
  HttpResult send(HttpRequest request) throws IOException, InterruptedException;
}
