package com.voronina.course;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        boolean runAuto = false;

        OutputFileFormat format = OutputFileFormat.JSON;
        String outputName = "output";
        boolean overwrite = true;

        int objectsCount = 50;
        int maxConcurrentTasks = 2;
        long intervalSeconds = 5;

        String apisArg = "";
        String apiToPrint = "";

        for (String a : args) {
            if ("--auto".equals(a) || "-a".equals(a)) {
                runAuto = true;
            } else if (a.startsWith("--format=")) {
                format = "csv".equalsIgnoreCase(a.substring("--format=".length()).trim())
                        ? OutputFileFormat.CSV
                        : OutputFileFormat.JSON;
            } else if (a.startsWith("--output=")) {
                outputName = a.substring("--output=".length()).trim();
            } else if (a.equals("--append") || a.equals("--no-overwrite")) {
                overwrite = false;
            } else if (a.startsWith("--apis=")) {
                apisArg = a.substring("--apis=".length()).trim();
            } else if (a.startsWith("--print-apis=")) {
                apiToPrint = a.substring("--print-apis=".length()).trim();
            } else if (a.startsWith("--count=")) {
                try {
                    objectsCount = Integer.parseInt(a.substring("--count=".length()).trim());
                } catch (NumberFormatException ignored) {
                }
            } else if (a.startsWith("--threads=") || a.startsWith("-n=")) {
                String value = a.contains("=") ? a.substring(a.indexOf('=') + 1).trim() : "";
                try {
                    maxConcurrentTasks = Integer.parseInt(value);
                } catch (NumberFormatException ignored) {
                }
            } else if (a.startsWith("--interval=") || a.startsWith("-t=")) {
                String value = a.contains("=") ? a.substring(a.indexOf('=') + 1).trim() : "";
                try {
                    intervalSeconds = Long.parseLong(value);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (runAuto) {
            List<Api> apis = createApis(apisArg);

            if (apis.isEmpty()) {
                System.out.println("No APIs selected. Exiting.");
                return;
            }

            PollingConfig config = new PollingConfig(
                    maxConcurrentTasks,
                    intervalSeconds,
                    objectsCount);

            ThreadSafeOutputWriter writer = new ThreadSafeOutputWriter(
                    apis,
                    format,
                    outputName,
                    overwrite);

            ApiPollingManager pollingManager = new ApiPollingManager(
                    apis,
                    config,
                    writer);

            Runtime.getRuntime().addShutdownHook(new Thread(pollingManager::stop));

            pollingManager.start();

            if (objectsCount > 0) {
                pollingManager.awaitCompletion();
                pollingManager.stop();
                writer.printOutput(apiToPrint);
            } else {
                System.out.println("Infinite polling mode. Press Ctrl+C to stop.");
            }

            return;
        }

        new ConsoleGui().run();
    }

    private static List<Api> createApis(String apisArg) {
        List<Api> apis = new ArrayList<>();

        if (apisArg == null || apisArg.isBlank()) {
            for (ApiRegistry.ApiEntry e : ApiRegistry.all().values()) {
                apis.add(e.factory().create());
            }
        } else {
            for (String key : apisArg.split(",")) {
                Api api = ApiRegistry.create(key.trim());

                if (api != null) {
                    apis.add(api);
                } else {
                    System.out.println("Warning: unknown api key '" + key + "' - skipping");
                }
            }
        }

        return apis;
    }
}