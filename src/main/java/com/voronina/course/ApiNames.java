package com.voronina.course;

public class ApiNames {
    public static String sanitize(String name) {
        if (name == null)
            return "api";
        return name.replaceAll("[^A-Za-z0-9_-]", "_").toLowerCase();
    }
}
