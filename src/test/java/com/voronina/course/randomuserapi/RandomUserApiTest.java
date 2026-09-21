package com.voronina.course.randomuserapi;

import com.voronina.course.ApiObject;
import com.voronina.course.HttpResult;
import com.voronina.course.HttpSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.http.HttpRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RandomUserApiTest {
  @Test
  void fetchDataParsesUsersWithoutRealNetworkRequest() throws Exception {
    HttpSender httpSender = mock(HttpSender.class);
    when(httpSender.send(any(HttpRequest.class))).thenReturn(new HttpResult(200, """
        {
          "results": [
            {
              "gender": "female",
              "name": {"title":"Ms","first":"Anna","last":"Stone"},
              "location": {"city":"Madrid","street":{"number":12,"name":"Main"}},
              "email": "anna@example.com",
              "login": {"uuid":"u1","username":"anna","password":"pw"},
              "registered": {"date":"2024-01-01","age":2},
              "phone": "123",
              "cell": "456",
              "id": {"name":"DNI","value":"777"}
            }
          ]
        }
        """));

    RandomUserApi api = new RandomUserApi(httpSender);
    ApiObject[] result = api.fetchData();

    assertEquals(1, result.length);
    RandomUser user = (RandomUser) result[0];
    assertEquals("female", user.getGender());
    assertEquals("Anna", user.getName().getFirst());
    assertEquals("Madrid", user.getLocation().getCity());
    assertEquals("anna@example.com", user.getEmail());
    assertArrayEquals(RandomUser.CSV_HEADERS, api.csvHeaders());
    assertEquals("RandomUserApi", api.name());

    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(httpSender).send(requestCaptor.capture());
    assertEquals("https://randomuser.me/api/", requestCaptor.getValue().uri().toString());
  }

  @Test
  void fetchDataRejectsMissingResults() throws Exception {
    HttpSender httpSender = mock(HttpSender.class);
    when(httpSender.send(any(HttpRequest.class)))
        .thenReturn(new HttpResult(200, "{}"));

    RandomUserApi api = new RandomUserApi(httpSender);

    IOException ex = assertThrows(IOException.class, api::fetchData);
    assertTrue(ex.getMessage().contains("Invalid response"));
  }

  @Test
  void fetchDataRejectsNullWrapper() throws Exception {
    HttpSender httpSender = mock(HttpSender.class);
    when(httpSender.send(any(HttpRequest.class)))
        .thenReturn(new HttpResult(200, "null"));

    RandomUserApi api = new RandomUserApi(httpSender);

    assertThrows(IOException.class, api::fetchData);
  }
}
