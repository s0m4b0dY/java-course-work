package com.voronina.course.freepikapi;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.voronina.course.Api;
import com.voronina.course.ApiObject;

public class FreepikApi implements Api {
  private static final String API_KEY = "FPSXa86f3a1ce00580de012a30ffc2086b37";
  private static final String BASE_URL = "https://api.freepik.com/v1/icons";
  private static final int PER_PAGE = 10;

  /** Current page — increments with each fetchData() call so ApiManager gets fresh items. */
  private int currentPage = 1;

  @Override
  public String name() {
    return "FreepikApi";
  }

  @Override
  public ApiObject[] fetchData() throws IOException, InterruptedException {
    String url = BASE_URL + "?order=relevance&per_page=" + PER_PAGE + "&page=" + currentPage;

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .GET()
        .header("x-freepik-api-key", API_KEY)
        .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new IOException("FreepikApi HTTP error: " + response.statusCode() + " — " + response.body());
    }

    ResponseWrapper wrapper = new GsonBuilder().serializeNulls().create()
        .fromJson(response.body(), ResponseWrapper.class);

    if (wrapper == null || wrapper.data == null || wrapper.data.length == 0) {
      throw new IllegalStateException("No icons returned from FreepikApi (page " + currentPage + ")");
    }

    currentPage++;
    return wrapper.data;
  }

  // ---- Response wrapper ----

  @SuppressWarnings("unused")
  private static class ResponseWrapper {
    @SerializedName("data")
    FreepikIcon[] data;

    @SerializedName("meta")
    Meta meta;
  }

  @SuppressWarnings("unused")
  private static class Meta {
    Pagination pagination;
  }

  @SuppressWarnings("unused")
  private static class Pagination {
    int per_page;
    int total;
    int last_page;
    int current_page;
  }
}
