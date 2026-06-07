package com.voronina.course;

import java.util.List;

public interface OutputWriter extends AutoCloseable {
  void writeBatch(String source, List<ApiObject> objects);

  void printOutput(String apiToPrint);

  @Override
  default void close() {
    // no-op by default
  }
}
