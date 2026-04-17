package com.voronina.course;

import java.io.FileNotFoundException;
import java.io.IOException;

import com.voronina.course.lastmessageapi.LastMessageApi;
import com.voronina.course.randomuserapi.RandomUserApi;
import com.voronina.course.chatsapi.ChatsApi;
import java.util.ArrayList;
import java.util.List;

public class Main {
    ApiManager apiManager = new ApiManager();
    public static void main(String[] args) throws FileNotFoundException, IOException {
        boolean runAuto = false;
        // defaults
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
                String f = a.substring("--format=".length()).trim().toUpperCase();
                if ("CSV".equals(f)) format = OutputFileFormat.CSV;
                else format = OutputFileFormat.JSON;
            } else if (a.startsWith("--output=")) {
                outputName = a.substring("--output=".length()).trim();
            } else if (a.equals("--append") || a.equals("--no-overwrite")) {
                overwrite = false;
            } else if (a.startsWith("--api=")) {
                apiToPrint = a.substring("--api=".length()).trim();
            } else if (a.startsWith("--count=")) {
                try { objectsCount = Integer.parseInt(a.substring("--count=".length()).trim()); } catch (NumberFormatException ex) { }
            } else if (a.startsWith("--interval=")) {
                try { intervalMillis = Long.parseLong(a.substring("--interval=".length()).trim()); } catch (NumberFormatException ex) { }
            }
        }

        if (runAuto) {
            List<Api> apis = new ArrayList<>();
            if (apisArg == null || apisArg.isBlank()) {
                apis.add(new LastMessageApi());
                apis.add(new RandomUserApi());
                apis.add(new ChatsApi());
            } else {
                String[] keys = apisArg.split(",");
                for (String k : keys) {
                    Api api = createApiByKey(k.trim());
                    if (api != null) apis.add(api);
                    else System.out.println("Warning: unknown api key '" + k + "' - skipping");
                }
            }

            ApiManager manager = new ApiManager();
            manager.run(apis, format, outputName, overwrite, apiToPrint, objectsCount, intervalMillis);
            return;
        }

        // fallback to console gui
        ConsoleGui gui = new ConsoleGui();
        gui.run();
    }

    private static Api createApiByKey(String key) {
        if (key == null) return null;
        String k = key.trim().toLowerCase();
        switch (k) {
            case "lastmessage":
            case "last_message":
            case "lastmessageapi":
            case "lastmessage_api":
            case "last":
                return new LastMessageApi();
            case "randomuser":
            case "random_user":
            case "randomuserapi":
            case "random":
                return new RandomUserApi();
            case "chats":
            case "chat":
            case "chatsapi":
            case "chats_api":
                return new ChatsApi();
            default:
                return null;
        }
    }
}