package com.voronina.course;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApiManager {
    private final int defaultObjectsCount = 50;
    private final String defaultOutputFileName = "output";

    public void run(List<Api> apis,
            OutputFileFormat outputFileFormat,
            String outputFileName,
            boolean overwrite,
            String apiToPrint,
            int objectsCount,
            long intervalMillis) {
        final int perApiTarget = objectsCount > 0 ? objectsCount : defaultObjectsCount;
        final int maxConsecutiveFailures = 5;

        Map<String, List<ApiObject>> collected = new LinkedHashMap<>();

        for (Api api : apis) {
            String apiName = ApiNames.sanitize(api.name());
            List<ApiObject> list = new ArrayList<>(perApiTarget);
            int consecutiveFailures = 0;

            System.out.println("Starting fetch from API: " + api.name() + " (target: " + perApiTarget + " objects)");

            while (list.size() < perApiTarget) {
                try {
                    System.out.println(
                            "Fetching from '" + api.name() + "' (current: " + list.size() + "/" + perApiTarget + ")");

                    ApiObject[] objs = api.fetchData();

                    if (objs == null || objs.length == 0) {
                        consecutiveFailures++;
                        System.out.println("Warning: api '" + api.name() + "' returned no objects (failure #"
                                + consecutiveFailures + ").");
                    } else {
                        consecutiveFailures = 0;
                        System.out.println("Successfully fetched " + objs.length + " objects from '" + api.name() + "'");

                        for (ApiObject o : objs) {
                            if (o == null) {
                                continue;
                            }

                            list.add(o);

                            if (list.size() >= perApiTarget) {
                                break;
                            }
                        }
                    }
                } catch (IOException e) {
                    consecutiveFailures++;
                    System.out.println("Warning: failed to fetch from " + api.name() + ": " + e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("Fatal: interrupted while fetching from '" + api.name() + "'. Aborting.");
                    return;
                } catch (RuntimeException e) {
                    consecutiveFailures++;
                    System.out.println(
                            "Warning: unexpected error while fetching from '" + api.name() + "': " + e.getMessage());
                }

                if (consecutiveFailures >= maxConsecutiveFailures) {
                    System.out.println("Fatal: too many consecutive failures for '" + api.name() + "' (>= "
                            + maxConsecutiveFailures + "). Aborting run.");
                    return;
                }

                if (intervalMillis > 0 && list.size() < perApiTarget) {
                    try {
                        System.out.println("Waiting " + intervalMillis + "ms before next fetch...");
                        Thread.sleep(intervalMillis);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.println("Fatal: interrupted during interval sleep. Aborting.");
                        return;
                    }
                }
            }

            collected.put(apiName, list);
            System.out.println("Collected " + list.size() + " objects from api '" + api.name() + "'.");
        }

        String baseName = outputFileName == null || outputFileName.isBlank() ? defaultOutputFileName : outputFileName;

        OutputWriter writer = OutputWriterFactory.create(outputFileFormat);
        writer.write(collected, baseName, overwrite);
        writer.print(baseName, apiToPrint);
    }
}
