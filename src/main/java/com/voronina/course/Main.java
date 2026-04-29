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
        String apiToPrint = "";
        int objectsCount = 50;
        long intervalMillis = 0;
        String apisArg = "";

        for (String a : args) {
            if ("--auto".equals(a) || "-a".equals(a)) runAuto = true;
            else if (a.startsWith("--format=")) {
                format = "csv".equalsIgnoreCase(a.substring("--format=".length()).trim())
                    ? OutputFileFormat.CSV : OutputFileFormat.JSON;
            } else if (a.startsWith("--output=")) {
                outputName = a.substring("--output=".length()).trim();
            } else if (a.equals("--append") || a.equals("--no-overwrite")) {
                overwrite = false;
            } else if (a.startsWith("--api=")) {
                apiToPrint = a.substring("--api=".length()).trim();
            } else if (a.startsWith("--apis=")) {
                apisArg = a.substring("--apis=".length()).trim();
            } else if (a.startsWith("--count=")) {
                try { objectsCount = Integer.parseInt(a.substring("--count=".length()).trim()); } catch (NumberFormatException ex) { }
            } else if (a.startsWith("--interval=")) {
                try { intervalMillis = Long.parseLong(a.substring("--interval=".length()).trim()); } catch (NumberFormatException ex) { }
            }
        }

        if (runAuto) {
            List<Api> apis = new ArrayList<>();
            if (apisArg == null || apisArg.isBlank()) {
                // all registered APIs
                for (ApiRegistry.ApiEntry e : ApiRegistry.all().values()) {
                    apis.add(e.factory().create());
                }
            } else {
                for (String k : apisArg.split(",")) {
                    Api api = ApiRegistry.create(k.trim());
                    if (api != null) apis.add(api);
                    else System.out.println("Warning: unknown api key '" + k + "' - skipping");
                }
            }
            new ApiManager().run(apis, format, outputName, overwrite, apiToPrint, objectsCount, intervalMillis);
            return;
        }

        new ConsoleGui().run();
    }
}
