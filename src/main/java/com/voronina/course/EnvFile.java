package com.voronina.course;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class EnvFile {
    public static String get(String key) {
        String realEnv = System.getenv(key);
        if (realEnv != null && !realEnv.isBlank()) {
            System.out.println("Debug: read " + key + " from system environment");
            return realEnv;
        }

        Path envPath = Paths.get(".env");
        if (!Files.exists(envPath)) {
            System.out.println("Debug: .env file not found in current folder");
            return "";
        }

        try {
            List<String> lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                String temp = line.trim();

                if (temp.isEmpty() || temp.startsWith("#")) {
                    continue;
                }

                int pos = temp.indexOf('=');
                if (pos < 0) {
                    continue;
                }

                String name = temp.substring(0, pos).trim();
                String value = temp.substring(pos + 1).trim();

                if (name.equals(key)) {
                    value = removeQuotes(value);
                    System.out.println("Debug: read " + key + " from .env file");
                    return value;
                }
            }
        } catch (IOException ex) {
            System.out.println("Warning: cannot read .env file: " + ex.getMessage());
        }

        return "";
    }

    private static String removeQuotes(String value) {
        if (value == null) {
            return "";
        }

        if (value.length() >= 2) {
            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                (value.startsWith("'") && value.endsWith("'"))) {
                return value.substring(1, value.length() - 1);
            }
        }

        return value;
    }
}
