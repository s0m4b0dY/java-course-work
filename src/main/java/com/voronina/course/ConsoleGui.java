package com.voronina.course;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ConsoleGui {
    public void run() {
        try (Scanner sc = new Scanner(System.in)) {
            System.out.println("Interactive mode: configure data fetch and output");

            // Choose APIs from registry
            Map<String, ApiRegistry.ApiEntry> registry = ApiRegistry.all();
            List<String> keys = new ArrayList<>(registry.keySet());

            System.out.println("Available APIs:");
            for (int i = 0; i < keys.size(); i++) {
                ApiRegistry.ApiEntry e = registry.get(keys.get(i));
                System.out.printf("  %d) %s (key: %s)%n", i + 1, e.displayName(), e.key());
            }
            System.out.print("Select APIs to run (comma-separated numbers or keys, or 'all') [all]: ");
            String apisLine = sc.nextLine().trim();

            List<String> selectedKeys = new ArrayList<>();
            if (apisLine.isEmpty() || "all".equalsIgnoreCase(apisLine)) {
                selectedKeys.addAll(keys);
            } else {
                for (String part : apisLine.split(",")) {
                    part = part.trim();
                    if (part.matches("\\d+")) {
                        int idx = Integer.parseInt(part) - 1;
                        if (idx >= 0 && idx < keys.size()) selectedKeys.add(keys.get(idx));
                        else System.out.println("Unknown selection: " + (idx + 1));
                    } else {
                        selectedKeys.add(part.toLowerCase());
                    }
                }
            }

            // Format
            System.out.print("Output format (json/csv) [json]: ");
            String fmt = sc.nextLine().trim();
            OutputFileFormat format = "csv".equalsIgnoreCase(fmt) ? OutputFileFormat.CSV : OutputFileFormat.JSON;

            // Overwrite or append
            System.out.print("Write mode - create new or append? (new/append) [new]: ");
            boolean overwrite = !"append".equalsIgnoreCase(sc.nextLine().trim());

            // Output file name
            System.out.print("Base output file name [output]: ");
            String outName = sc.nextLine().trim();
            if (outName.isEmpty()) outName = "output";

            // Count
            System.out.print("Objects per API to fetch [50]: ");
            String cnt = sc.nextLine().trim();
            int objectsCount = 50;
            if (!cnt.isEmpty()) {
                try { objectsCount = Integer.parseInt(cnt); }
                catch (NumberFormatException ex) { System.out.println("Invalid number, using 50"); }
            }

            // Interval
            System.out.print("Interval between requests in milliseconds [0 = no delay]: ");
            String intervalStr = sc.nextLine().trim();
            long intervalMillis = 0;
            if (!intervalStr.isEmpty()) {
                try { intervalMillis = Long.parseLong(intervalStr); }
                catch (NumberFormatException ex) { System.out.println("Invalid number, using 0"); }
            }

            // Print options
            System.out.print("After run, print output to screen? (all/specific/none) [all]: ");
            String printOpt = sc.nextLine().trim();
            String apiToPrint;
            if ("none".equalsIgnoreCase(printOpt)) {
                apiToPrint = null;
            } else if ("specific".equalsIgnoreCase(printOpt)) {
                System.out.print("Enter api keys to print (comma-separated): ");
                apiToPrint = sc.nextLine().trim();
            } else {
                apiToPrint = "";
            }

            // Build Api list via registry
            List<Api> apis = new ArrayList<>();
            System.out.println("Creating API instances...");
            for (String k : selectedKeys) {
                Api api = ApiRegistry.create(k);
                if (api != null) apis.add(api);
                else System.out.println("Warning: unknown api '" + k + "' - skipped");
            }

            if (apis.isEmpty()) {
                System.out.println("No APIs selected. Exiting.");
                return;
            }

            System.out.println("Running API manager...");
            new ApiManager().run(apis, format, outName, overwrite, apiToPrint, objectsCount, intervalMillis);
        }
    }
}
