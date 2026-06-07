package com.voronina.course;

public class CliOptions {
  public boolean runAuto = false;
  public OutputFileFormat format = OutputFileFormat.JSON;
  public String outputName = "output";
  public boolean overwrite = true;
  public int objectsCount = 50;
  public int maxConcurrentTasks = 2;
  public long intervalSeconds = 5;
  public String apisArg = "";
  public String apiToPrint = "";

  public static CliOptions parse(String[] args) {
    CliOptions options = new CliOptions();

    if (args == null) {
      return options;
    }

    for (String a : args) {
      if ("--auto".equals(a) || "-a".equals(a)) {
        options.runAuto = true;
      } else if (a.startsWith("--format=")) {
        options.format = "csv".equalsIgnoreCase(valueAfterEquals(a))
            ? OutputFileFormat.CSV
            : OutputFileFormat.JSON;
      } else if (a.startsWith("--output=")) {
        options.outputName = valueAfterEquals(a);
      } else if (a.equals("--append") || a.equals("--no-overwrite")) {
        options.overwrite = false;
      } else if (a.startsWith("--apis=")) {
        options.apisArg = valueAfterEquals(a);
      } else if (a.startsWith("--print-apis=")) {
        options.apiToPrint = valueAfterEquals(a);
      } else if (a.startsWith("--count=")) {
        options.objectsCount = parseInt(valueAfterEquals(a), options.objectsCount);
      } else if (a.startsWith("--threads=") || a.startsWith("-n=")) {
        options.maxConcurrentTasks = parseInt(valueAfterEquals(a), options.maxConcurrentTasks);
      } else if (a.startsWith("--interval=") || a.startsWith("-t=")) {
        options.intervalSeconds = parseLong(valueAfterEquals(a), options.intervalSeconds);
      }
    }

    return options;
  }

  private static String valueAfterEquals(String text) {
    int pos = text.indexOf('=');
    if (pos < 0) {
      return "";
    }
    return text.substring(pos + 1).trim();
  }

  private static int parseInt(String text, int fallback) {
    try {
      return Integer.parseInt(text);
    } catch (NumberFormatException ex) {
      System.out.println("Debug: bad int value '" + text + "', using " + fallback);
      return fallback;
    }
  }

  private static long parseLong(String text, long fallback) {
    try {
      return Long.parseLong(text);
    } catch (NumberFormatException ex) {
      System.out.println("Debug: bad long value '" + text + "', using " + fallback);
      return fallback;
    }
  }
}
