package com.voronina.course;

import java.util.List;
import java.util.Map;

public interface OutputWriter {
    void write(Map<String, List<ApiObject>> collected, String outputFileName, boolean overwrite);
    void print(String outputFileName, String apiToPrint);
}
