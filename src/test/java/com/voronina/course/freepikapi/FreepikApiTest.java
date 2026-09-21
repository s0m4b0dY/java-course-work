package com.voronina.course.freepikapi;

import com.voronina.course.ApiObject;
import com.voronina.course.HttpResult;
import com.voronina.course.HttpSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FreepikApiTest {
  @Test
  void emptyApiKeyFailsBeforeHttpRequest() {
    HttpSender httpSender = mock(HttpSender.class);
    FreepikApi api = new FreepikApi("", httpSender);

    IOException ex = assertThrows(IOException.class, api::fetchData);
    assertTrue(ex.getMessage().contains("FREEPIK_API_KEY"));
    verifyNoInteractions(httpSender);
  }

  @Test
  void nullApiKeyIsAlsoTreatedAsMissing() {
    HttpSender httpSender = mock(HttpSender.class);
    FreepikApi api = new FreepikApi(null, httpSender);

    assertThrows(IOException.class, api::fetchData);
    verifyNoInteractions(httpSender);
  }

  @Test
  void fetchDataParsesIconsAddsApiKeyAndIncrementsPage() throws Exception {
    HttpSender httpSender = mock(HttpSender.class);

    when(httpSender.send(any(HttpRequest.class)))
        .thenReturn(
            new HttpResult(200, iconResponse(10, "home")),
            new HttpResult(200, iconResponse(20, "user")));

    FreepikApi api = new FreepikApi("  test-key  ", httpSender);

    ApiObject[] first = api.fetchData();
    ApiObject[] second = api.fetchData();

    assertEquals(1, first.length);
    assertEquals(10, ((FreepikIcon) first[0]).getId());
    assertEquals("home", ((FreepikIcon) first[0]).getName());
    assertEquals(20, ((FreepikIcon) second[0]).getId());

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(httpSender, times(2)).send(captor.capture());
    List<HttpRequest> requests = captor.getAllValues();

    assertTrue(requests.get(0).uri().toString().contains("per_page=10"));
    assertTrue(requests.get(0).uri().toString().contains("page=1"));
    assertTrue(requests.get(1).uri().toString().contains("page=2"));
    assertEquals("test-key",
        requests.get(0).headers().firstValue("x-freepik-api-key").orElse(null));
  }

  @Test
  void emptyDataThrowsAndKeepsSamePageForRetry() throws Exception {
    HttpSender httpSender = mock(HttpSender.class);
    when(httpSender.send(any(HttpRequest.class)))
        .thenReturn(
            new HttpResult(200, "{\"data\":[]}"),
            new HttpResult(200, iconResponse(1, "retry")));

    FreepikApi api = new FreepikApi("key", httpSender);

    IllegalStateException ex = assertThrows(IllegalStateException.class, api::fetchData);
    assertTrue(ex.getMessage().contains("page 1"));

    ApiObject[] retry = api.fetchData();
    assertEquals("retry", ((FreepikIcon) retry[0]).getName());

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(httpSender, times(2)).send(captor.capture());
    assertTrue(captor.getAllValues().get(0).uri().toString().contains("page=1"));
    assertTrue(captor.getAllValues().get(1).uri().toString().contains("page=1"));
  }

  @Test
  void missingDataAlsoThrows() throws Exception {
    HttpSender httpSender = mock(HttpSender.class);
    when(httpSender.send(any(HttpRequest.class)))
        .thenReturn(new HttpResult(200, "{}"));

    FreepikApi api = new FreepikApi("key", httpSender);
    assertThrows(IllegalStateException.class, api::fetchData);
  }

  @Test
  void nameAndHeadersAreAvailable() {
    FreepikApi api = new FreepikApi("test-key", mock(HttpSender.class));

    assertEquals("FreepikApi", api.name());
    assertArrayEquals(FreepikIcon.CSV_HEADERS, api.csvHeaders());
  }

  private static String iconResponse(int id, String name) {
    return """
        {
          "data": [
            {
              "id": %d,
              "name": "%s",
              "slug": "%s-icon",
              "free_svg": true,
              "created": "2026-01-01",
              "style": {"id":1,"name":"outline"},
              "family": {"id":2,"name":"basic","total":5},
              "author": {"id":3,"name":"Author","slug":"author","avatar":"a.png","assets":7},
              "tags": [{"name":"tag","slug":"tag"}],
              "thumbnails": [{"width":64,"height":64,"url":"https://img.test/icon.png"}]
            }
          ],
          "meta": {
            "pagination": {"per_page":10,"total":100,"last_page":10,"current_page":1}
          }
        }
        """.formatted(id, name, name);
  }
}
