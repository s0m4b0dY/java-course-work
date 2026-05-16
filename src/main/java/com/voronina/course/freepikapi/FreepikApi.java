package com.voronina.course.freepikapi;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.annotations.SerializedName;
import com.voronina.course.AbstractJsonApi;
import com.voronina.course.ApiObject;
import com.voronina.course.EnvFile;

public class FreepikApi extends AbstractJsonApi {
    private static final String BASE_URL = "https://api.freepik.com/v1/icons";
    private static final int PER_PAGE = 10;
    private static final String API_KEY_NAME = "FREEPIK_API_KEY";

    private int currentPage = 1;

    @Override
    public String name() {
        return "FreepikApi";
    }

    @Override
    public ApiObject[] fetchData() throws IOException, InterruptedException {
        String apiKey = EnvFile.get(API_KEY_NAME);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException("Freepik API key not found. Put FREEPIK_API_KEY=your_key_here into .env file");
        }

        String url = BASE_URL + "?order=relevance&per_page=" + PER_PAGE + "&page=" + currentPage;

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-freepik-api-key", apiKey);

        String json = getJson(url, headers);
        ResponseWrapper wrapper = gson().fromJson(json, ResponseWrapper.class);

        if (wrapper == null || wrapper.data == null || wrapper.data.length == 0) {
            throw new IllegalStateException("No icons returned from FreepikApi (page " + currentPage + ")");
        }

        currentPage++;
        return wrapper.data;
    }

    private static class ResponseWrapper {
        @SerializedName("data")
        FreepikIcon[] data;
    }
}
