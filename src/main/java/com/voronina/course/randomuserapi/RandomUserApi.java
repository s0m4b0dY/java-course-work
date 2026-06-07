package com.voronina.course.randomuserapi;

import java.io.IOException;

import com.voronina.course.ApiObject;
import com.voronina.course.BaseApi;

public class RandomUserApi extends BaseApi {
  private static final String API_URL = "https://randomuser.me/api/";

  public RandomUserApi() {
    super("RandomUserApi");
  }

  @Override
  public String[] csvHeaders() {
    return RandomUser.CSV_HEADERS;
  }

  @Override
  public ApiObject[] fetchData() throws IOException, InterruptedException {
    String body = getBody(API_URL);
    ResponseWrapper wrapper = parseJson(body, ResponseWrapper.class);

    if (wrapper == null || wrapper.results == null) {
      throw new IOException("Invalid response from RandomUserApi");
    }

    return wrapper.results;
  }

  private static class ResponseWrapper {
    RandomUser[] results;
  }
}
