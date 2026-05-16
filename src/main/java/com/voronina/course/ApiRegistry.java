package com.voronina.course;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.voronina.course.emojisapi.EmojiApi;
import com.voronina.course.freepikapi.FreepikApi;
import com.voronina.course.randomuserapi.RandomUserApi;

public class ApiRegistry {

    public record ApiEntry(String key, String displayName) {}

    public static List<ApiEntry> all() {
        List<ApiEntry> result = new ArrayList<>();

        result.add(new ApiEntry("randomuser", "RandomUserApi"));
        result.add(new ApiEntry("emoji",      "EmojiApi"));
        result.add(new ApiEntry("freepik",    "FreepikApi"));

        return Collections.unmodifiableList(result);
    }

    public static Api create(String key) {
        if (key == null) return null;
        String lowerKey = key.trim().toLowerCase();

        if (lowerKey.equals("randomuser")) return new RandomUserApi();
        if (lowerKey.equals("emoji"))      return new EmojiApi();
        if (lowerKey.equals("freepik"))    return new FreepikApi();

        return null;
    }
}