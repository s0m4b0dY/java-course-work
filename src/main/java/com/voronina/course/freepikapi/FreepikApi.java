package com.voronina.course.freepikapi;

import java.io.IOException;
import java.net.http.HttpRequest;

import com.google.gson.annotations.SerializedName;
import com.voronina.course.ApiObject;
import com.voronina.course.BaseApi;
import com.voronina.course.EnvFile;

public class FreepikApi extends BaseApi {
  private static final String BASE_URL = "https://api.freepik.com/v1/icons";
  private static final int PER_PAGE = 10;

  private final String apiKey;
  private int currentPage = 1;

  public FreepikApi() {
    this(EnvFile.getFirst("FREEPIK_API_KEY", "API_KEY"));
  }

  FreepikApi(String apiKey) {
    super("FreepikApi");
    this.apiKey = apiKey == null ? "" : apiKey.trim();
  }

  @Override
  public String[] csvHeaders() {
    return FreepikIcon.CSV_HEADERS;
  }

  @Override
  protected void fillHeaders(HttpRequest.Builder builder) {
    if (!apiKey.isBlank()) {
      builder.header("x-freepik-api-key", apiKey);
    }
  }

  @Override
  public ApiObject[] fetchData() throws IOException, InterruptedException {
    if (apiKey.isBlank()) {
      throw new IOException("Freepik API key is not configured. Put FREEPIK_API_KEY in system env or .env file.");
    }

    String url = BASE_URL + "?order=relevance&per_page=" + PER_PAGE + "&page=" + currentPage;
    String body = getBody(url);

    ResponseWrapper wrapper = parseJson(body, ResponseWrapper.class);

    if (wrapper == null || wrapper.data == null || wrapper.data.length == 0) {
      throw new IllegalStateException("No icons returned from FreepikApi (page " + currentPage + ")");
    }

    currentPage++;
    return wrapper.data;
  }

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
