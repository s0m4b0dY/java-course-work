package com.voronina.course;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvFile {
    private static final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    public static String get(String key) {
        String value = dotenv.get(key);

        if (value == null || value.isBlank()) {
            System.out.println("Debug: env value not found: " + key);
            return "";
        }

        System.out.println("Debug: read env value: " + key);
        return value;
    }
}