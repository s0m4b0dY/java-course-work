package com.voronina.course.emojisapi;

import java.io.IOException;

import com.voronina.course.AbstractJsonApi;
import com.voronina.course.ApiObject;

public class EmojiApi extends AbstractJsonApi {
    private static final String API_URL = "https://emojihub.yurace.pro/api/random";

    @Override
    public String name() {
        return "EmojiApi";
    }

    @Override
    public ApiObject[] fetchData() throws IOException, InterruptedException {
        String json = getJson(API_URL);
        Emoji emoji = gson().fromJson(json, Emoji.class);

        if (emoji == null) {
            throw new IllegalStateException("No emoji returned from API");
        }

        return new ApiObject[] { emoji };
    }
}
