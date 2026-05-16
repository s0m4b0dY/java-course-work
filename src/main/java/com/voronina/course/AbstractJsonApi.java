package com.voronina.course;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public abstract class AbstractJsonApi implements Api {
    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    protected Gson gson() {
        return gson;
    }

    protected String getJson(String url) throws IOException, InterruptedException {
        return getJson(url, null);
    }

    protected String getJson(String url, Map<String, String> headers) throws IOException, InterruptedException {
        System.out.println("Debug: sending GET request to " + url);

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).GET();

        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                builder.header(header.getKey(), header.getValue());
            }
        }

        HttpRequest request = builder.build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(name() + " HTTP error: " + response.statusCode() + " - " + response.body());
        }

        return response.body();
    }
}
