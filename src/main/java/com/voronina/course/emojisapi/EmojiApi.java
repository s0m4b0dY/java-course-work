package com.voronina.course.emojisapi;

import java.io.IOException;

import com.voronina.course.ApiObject;
import com.voronina.course.BaseApi;

public class EmojiApi extends BaseApi {
  private static final String API_URL = "https://emojihub.yurace.pro/api/random";

  public EmojiApi() {
    super("EmojiApi");
  }

  @Override
  public String[] csvHeaders() {
    return Emoji.CSV_HEADERS;
  }

  @Override
  public ApiObject[] fetchData() throws IOException, InterruptedException {
    String body = getBody(API_URL);
    Emoji emoji = parseJson(body, Emoji.class);

    if (emoji == null) {
      throw new IllegalStateException("No emoji returned from API");
    }

    return new ApiObject[] { emoji };
  }
}
