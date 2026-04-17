package com.voronina.course;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.voronina.course.lastmessageapi.LastMessageApi;
import com.voronina.course.randomuserapi.RandomUserApi;
import com.voronina.course.chatsapi.ChatsApi;

public class ConsoleGui {
    public void run() {
        try (Scanner sc = new Scanner(System.in)) {
            System.out.println("Interactive mode: configure data fetch and output");

            // Choose APIs
            System.out.println("Available APIs:");
            System.out.println("  1) LastMessageApi (key: lastmessage)");
            System.out.println("  2) RandomUserApi (key: randomuser)");
            System.out.println("  3) ChatsApi (key: chats)");
            System.out.print("Select APIs to run (comma-separated numbers or 'all') [all]: ");
            String apisLine = sc.nextLine().trim();
            List<String> selectedKeys = new ArrayList<>();
            if (apisLine.isEmpty() || "all".equalsIgnoreCase(apisLine)) {
                selectedKeys.add("lastmessage");
                selectedKeys.add("randomuser");
                selectedKeys.add("chats");
            } else {
                String[] parts = apisLine.split(",");
                for (String p : parts) {
                    p = p.trim();
                    if (p.matches("\\d+")) {
                        int idx = Integer.parseInt(p);
                        switch (idx) {
                            case 1: selectedKeys.add("lastmessage"); break;
                            case 2: selectedKeys.add("randomuser"); break;
                            case 3: selectedKeys.add("chats"); break;
                            default: System.out.println("Unknown selection: " + p); break;
                        }
                    } else {
                        // allow entering keys directly
                        selectedKeys.add(p);
                    }
                }
            }

            // Format
            System.out.print("Output format (json/csv) [json]: ");
            String fmt = sc.nextLine().trim();
            OutputFileFormat format = "csv".equalsIgnoreCase(fmt) ? OutputFileFormat.CSV : OutputFileFormat.JSON;

            // Overwrite or append
            System.out.print("Write mode - create new or append? (new/append) [new]: ");
            String mode = sc.nextLine().trim();
            boolean overwrite = !"append".equalsIgnoreCase(mode);

            // Output file name
            System.out.print("Base output file name [output]: ");
            String outName = sc.nextLine().trim();
            if (outName.isEmpty()) outName = "output";

            // Count
            System.out.print("Objects per API to fetch [50]: ");
            String cnt = sc.nextLine().trim();
            int objectsCount = 50;
            if (!cnt.isEmpty()) {
                try { objectsCount = Integer.parseInt(cnt); } catch (NumberFormatException ex) { System.out.println("Invalid number, using 50"); }
            }

            // Interval
            System.out.print("Interval between requests in milliseconds [0 = no delay]: ");
            String intervalStr = sc.nextLine().trim();
            long intervalMillis = 0;
            if (!intervalStr.isEmpty()) {
                try { intervalMillis = Long.parseLong(intervalStr); } catch (NumberFormatException ex) { System.out.println("Invalid number, using 0"); }
            }

            // Print options
            System.out.print("After run, print output to screen? (all/specific/none) [all]: ");
            String printOpt = sc.nextLine().trim();
            String apiToPrint = ""; // blank means print all in ApiManager
            if ("none".equalsIgnoreCase(printOpt)) apiToPrint = "__NONE__";
            else if ("specific".equalsIgnoreCase(printOpt)) {
                System.out.print("Enter api keys to print (comma-separated, e.g. randomuser,chats or names): ");
                apiToPrint = sc.nextLine().trim();
            } else {
                apiToPrint = "";
            }

            // Build Api list
            List<Api> apis = new ArrayList<>();
            for (String k : selectedKeys) {
                Api api = createApiByKey(k.trim());
                if (api != null) apis.add(api);
                else System.out.println("Warning: unknown api '" + k + "' - skipped");
            }

            if (apis.isEmpty()) {
                System.out.println("No APIs selected. Exiting.");
                return;
            }

            // If user selected 'none' for printing, pass null to ApiManager to avoid printing
            String printArg = apiToPrint.equals("__NONE__") ? null : apiToPrint;

            ApiManager manager = new ApiManager();
            manager.run(apis, format, outName, overwrite, printArg, objectsCount, intervalMillis);
        }
    }

    private Api createApiByKey(String key) {
        if (key == null) return null;
        String k = key.trim().toLowerCase();
        switch (k) {
            case "lastmessage": case "last_message": case "lastmessageapi": case "last": return new LastMessageApi();
            case "randomuser": case "random_user": case "randomuserapi": case "random": return new RandomUserApi();
            case "chats": case "chat": case "chatsapi": return new ChatsApi();
            default: return null;
        }
    }
}
