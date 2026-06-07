package com.voronina.course;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaseApiTest {
  static class ParseApi extends BaseApi {
    ParseApi() {
      super("ParseApi");
    }

    @Override
    public String[] csvHeaders() {
      return new String[] { "name" };
    }

    @Override
    public ApiObject[] fetchData() {
      return new ApiObject[0];
    }

    TestDto parse(String json) {
      return parseJson(json, TestDto.class);
    }
  }

  static class TestDto {
    String name;
  }

  @Test
  void baseApiStoresNameAndParsesJson() {
    ParseApi api = new ParseApi();
    TestDto dto = api.parse("{\"name\":\"abc\"}");

    assertEquals("ParseApi", api.name());
    assertEquals("abc", dto.name);
  }
}
