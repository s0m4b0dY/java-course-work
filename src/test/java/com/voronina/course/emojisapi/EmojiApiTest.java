package com.voronina.course.emojisapi;

import com.voronina.course.ApiObject;
import com.voronina.course.HttpResult;
import com.voronina.course.HttpSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.http.HttpRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmojiApiTest {
  @Test
  void fetchDataParsesEmojiWithoutRealNetworkRequest() throws Exception {
    HttpSender httpSender = mock(HttpSender.class);
    when(httpSender.send(any(HttpRequest.class))).thenReturn(new HttpResult(200, """
        {
          "name": "grinning face",
          "category": "smileys",
          "group": "face",
          "htmlCode": ["&#128512;"],
          "unicode": ["U+1F600"]
        }
        """));

    EmojiApi api = new EmojiApi(httpSender);
    ApiObject[] result = api.fetchData();

    assertEquals(1, result.length);
    Emoji emoji = (Emoji) result[0];
    assertEquals("grinning face", emoji.getName());
    assertEquals("smileys", emoji.getCategory());
    assertEquals("face", emoji.getGroup());
    assertEquals("&#128512;", emoji.getHtmlCode().get(0));
    assertArrayEquals(Emoji.CSV_HEADERS, api.csvHeaders());
    assertEquals("EmojiApi", api.name());

    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(httpSender).send(requestCaptor.capture());
    assertEquals("https://emojihub.yurace.pro/api/random", requestCaptor.getValue().uri().toString());
    assertEquals("GET", requestCaptor.getValue().method());
  }

  @Test
  void fetchDataRejectsNullEmojiResponse() throws Exception {
    HttpSender httpSender = mock(HttpSender.class);
    when(httpSender.send(any(HttpRequest.class)))
        .thenReturn(new HttpResult(200, "null"));

    EmojiApi api = new EmojiApi(httpSender);

    IllegalStateException ex = assertThrows(IllegalStateException.class, api::fetchData);
    assertTrue(ex.getMessage().contains("No emoji"));
  }
}
