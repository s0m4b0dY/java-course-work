package com.voronina.course.agentnexusapi;

import com.voronina.course.ApiObject;
import com.voronina.course.HttpResult;
import com.voronina.course.HttpSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpRequest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AgentNexusApiTest {
  @Test
  void fetchDataParsesMatchesAndRotatesSearchItems() throws Exception {
    HttpSender httpSender = mock(HttpSender.class);
    when(httpSender.send(any(HttpRequest.class)))
        .thenReturn(
            new HttpResult(200, response("resend", "Resend", "api")),
            new HttpResult(200, response("stripe", "Stripe", "api")));

    AgentNexusApi api = new AgentNexusApi(httpSender);

    ApiObject[] first = api.fetchData();
    ApiObject[] second = api.fetchData();

    assertEquals(1, first.length);
    assertEquals("Resend", ((AgentNexusEntry) first[0]).getName());
    assertEquals("send a transactional email", ((AgentNexusEntry) first[0]).getSearchNeed());

    assertEquals(1, second.length);
    assertEquals("Stripe", ((AgentNexusEntry) second[0]).getName());
    assertEquals("process online payments", ((AgentNexusEntry) second[0]).getSearchNeed());

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(httpSender, times(2)).send(captor.capture());
    List<HttpRequest> requests = captor.getAllValues();

    assertTrue(requests.get(0).uri().toString().contains("need=send+a+transactional+email"));
    assertTrue(requests.get(0).uri().toString().contains("limit=5"));
    assertTrue(requests.get(1).uri().toString().contains("need=process+online+payments"));
    assertEquals("GET", requests.get(0).method());
  }

  @Test
  void emptyMatchesThrowsAndKeepsSameSearchForRetry() throws Exception {
    HttpSender httpSender = mock(HttpSender.class);
    when(httpSender.send(any(HttpRequest.class)))
        .thenReturn(
            new HttpResult(200, "{\"need\":\"send a transactional email\",\"count\":0,\"matches\":[]}"),
            new HttpResult(200, response("resend", "Resend", "api")));

    AgentNexusApi api = new AgentNexusApi(httpSender);

    IllegalStateException ex = assertThrows(IllegalStateException.class, api::fetchData);
    assertTrue(ex.getMessage().contains("send a transactional email"));

    ApiObject[] retry = api.fetchData();
    assertEquals("Resend", ((AgentNexusEntry) retry[0]).getName());

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(httpSender, times(2)).send(captor.capture());
    assertTrue(captor.getAllValues().get(0).uri().toString().contains("need=send+a+transactional+email"));
    assertTrue(captor.getAllValues().get(1).uri().toString().contains("need=send+a+transactional+email"));
  }

  @Test
  void missingMatchesAlsoThrows() throws Exception {
    HttpSender httpSender = mock(HttpSender.class);
    when(httpSender.send(any(HttpRequest.class)))
        .thenReturn(new HttpResult(200, "{\"need\":\"send a transactional email\"}"));

    AgentNexusApi api = new AgentNexusApi(httpSender);
    assertThrows(IllegalStateException.class, api::fetchData);
  }

  @Test
  void nullResponseAlsoThrows() throws Exception {
    HttpSender httpSender = mock(HttpSender.class);
    when(httpSender.send(any(HttpRequest.class)))
        .thenReturn(new HttpResult(200, "null"));

    AgentNexusApi api = new AgentNexusApi(httpSender);
    assertThrows(IllegalStateException.class, api::fetchData);
  }

  @Test
  void searchListWrapsAroundAfterLastItem() throws Exception {
    HttpSender httpSender = mock(HttpSender.class);
    when(httpSender.send(any(HttpRequest.class)))
        .thenReturn(new HttpResult(200, response("item", "Item", "api")));

    AgentNexusApi api = new AgentNexusApi(httpSender);
    for (int i = 0; i < 9; i++) {
      api.fetchData();
    }

    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(httpSender, times(9)).send(captor.capture());
    List<HttpRequest> requests = captor.getAllValues();

    assertTrue(requests.get(0).uri().toString().contains("need=send+a+transactional+email"));
    assertTrue(requests.get(8).uri().toString().contains("need=send+a+transactional+email"));
  }

  @Test
  void nameAndHeadersAreAvailable() {
    AgentNexusApi api = new AgentNexusApi(mock(HttpSender.class));

    assertEquals("AgentNexusApi", api.name());
    assertArrayEquals(AgentNexusEntry.CSV_HEADERS, api.csvHeaders());
  }

  private static String response(String slug, String name, String category) {
    return """
        {
          "need": "send a transactional email",
          "coverage": "exact",
          "count": 1,
          "matches": [
            {
              "slug": "%s",
              "name": "%s",
              "category": "%s",
              "summary": "Useful service",
              "match_score": 11.5,
              "matched_on": ["capability", "summary"],
              "capabilities": ["send email", "webhook"],
              "call": {
                "endpoint": "https://api.example.test/v1",
                "auth_mode": "bearer",
                "auth_params": ["Authorization"],
                "input_format": "json",
                "output_format": "json",
                "rate_limit": "10 requests/sec",
                "pricing": {"type":"free-tier"},
                "example": {"method":"POST"},
                "docs_url": "https://example.test/docs"
              },
              "trust": {
                "verified": true,
                "reliability_score": 99.5,
                "uptime": 99.9,
                "samples": 123,
                "avg_latency_ms": 85.5,
                "last_probe_ok": true,
                "last_probe_at": "2026-09-22T10:00:00Z"
              },
              "report": {"available":true}
            }
          ]
        }
        """.formatted(slug, name, category);
  }
}
