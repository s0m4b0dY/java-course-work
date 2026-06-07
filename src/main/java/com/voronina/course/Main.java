package com.voronina.course;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        CliOptions options = CliOptions.parse(args);

        if (options.runAuto) {
            List<Api> apis = createApis(options.apisArg);

            if (apis.isEmpty()) {
                System.out.println("No APIs selected. Exiting.");
                return;
            }

            PollingConfig config = new PollingConfig(
                    options.maxConcurrentTasks,
                    options.intervalSeconds,
                    options.objectsCount);

            ThreadSafeOutputWriter writer = new ThreadSafeOutputWriter(
                    apis,
                    options.format,
                    options.outputName,
                    options.overwrite);

            try (ApiPollingManager pollingManager = new ApiPollingManager(
                    apis,
                    config,
                    writer)) {
                Runtime.getRuntime().addShutdownHook(new Thread(pollingManager::stop));

                pollingManager.start();

                if (options.objectsCount > 0) {
                    pollingManager.awaitCompletion();
                    pollingManager.stop();
                    writer.printOutput(options.apiToPrint);
                } else {
                    System.out.println("Infinite polling mode. Press Ctrl+C to stop.");
                }
            }

            return;
        }

        new ConsoleGui().run();
    }

    static List<Api> createApis(String apisArg) {
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
