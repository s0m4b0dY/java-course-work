package com.voronina.course.randomuserapi;

import java.io.IOException;

import com.voronina.course.AbstractJsonApi;
import com.voronina.course.ApiObject;

public class RandomUserApi extends AbstractJsonApi {
    private static final String API_URL = "https://randomuser.me/api/";

    @Override
    public String name() {
        return "RandomUserApi";
    }

    @Override
    public ApiObject[] fetchData() throws IOException, InterruptedException {
        String json = getJson(API_URL);
        ResponseWrapper wrapper = gson().fromJson(json, ResponseWrapper.class);

        if (wrapper == null || wrapper.results == null || wrapper.results.length == 0) {
            throw new IllegalStateException("No users returned from RandomUserApi");
        }

        return wrapper.results;
    }

    private static class ResponseWrapper {
        RandomUser[] results;
    }
}
