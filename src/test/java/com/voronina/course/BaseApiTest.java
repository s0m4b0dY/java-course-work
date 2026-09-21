package com.voronina.course;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.http.HttpRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BaseApiTest {
  static class ParseApi extends BaseApi {
    ParseApi() {
      super("ParseApi");
    }

    ParseApi(HttpSender httpSender) {
      super("ParseApi", httpSender);
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

    String readBody(String url) throws IOException, InterruptedException {
      return getBody(url);
    }

    @Override
    protected void fillHeaders(HttpRequest.Builder builder) {
      builder.header("X-Test-Header", "hello");
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

  @Test
  void getBodyUsesInjectedHttpSenderAndReturnsSuccessfulBody() throws Exception {
    HttpSender httpSender = mock(HttpSender.class);
    when(httpSender.send(any(HttpRequest.class)))
        .thenReturn(new HttpResult(200, "{\"ok\":true}"));

    ParseApi api = new ParseApi(httpSender);
    String body = api.readBody("https://example.test/data");

    assertEquals("{\"ok\":true}", body);

    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(httpSender).send(requestCaptor.capture());

    HttpRequest request = requestCaptor.getValue();
    assertEquals("GET", request.method());
    assertEquals("https://example.test/data", request.uri().toString());
    assertEquals("hello", request.headers().firstValue("X-Test-Header").orElse(null));
  }

  @Test
  void getBodyThrowsIOExceptionForHttpError() throws Exception {
    HttpSender httpSender = mock(HttpSender.class);
    when(httpSender.send(any(HttpRequest.class)))
        .thenReturn(new HttpResult(503, "server is unavailable"));

    ParseApi api = new ParseApi(httpSender);

    IOException ex = assertThrows(IOException.class,
        () -> api.readBody("https://example.test/error"));

    assertTrue(ex.getMessage().contains("503"));
    assertTrue(ex.getMessage().contains("server is unavailable"));
  }

  @Test
  void getBodyPropagatesIOExceptionFromHttpSender() throws Exception {
    HttpSender httpSender = mock(HttpSender.class);
    when(httpSender.send(any(HttpRequest.class)))
        .thenThrow(new IOException("network down"));

    ParseApi api = new ParseApi(httpSender);

    IOException ex = assertThrows(IOException.class,
        () -> api.readBody("https://example.test/data"));
    assertEquals("network down", ex.getMessage());
  }

  @Test
  void getBodyPropagatesInterruptedExceptionFromHttpSender() throws Exception {
    HttpSender httpSender = mock(HttpSender.class);
    when(httpSender.send(any(HttpRequest.class)))
        .thenThrow(new InterruptedException("interrupted"));

    ParseApi api = new ParseApi(httpSender);

    InterruptedException ex = assertThrows(InterruptedException.class,
        () -> api.readBody("https://example.test/data"));
    assertEquals("interrupted", ex.getMessage());
  }
}
