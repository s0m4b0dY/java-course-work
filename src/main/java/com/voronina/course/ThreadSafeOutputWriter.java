package com.voronina.course;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadSafeOutputWriter implements OutputWriter {
  private final OutputWriter realWriter;
  private final ReentrantLock lock = new ReentrantLock();

  public ThreadSafeOutputWriter(
      List<Api> apis,
      OutputFileFormat format,
      String baseFileName,
      boolean overwrite) {

    OutputFileFormat realFormat = format != null ? format : OutputFileFormat.JSON;

    if (realFormat == OutputFileFormat.CSV) {
      realWriter = new CsvOutputFileWriter(apis, baseFileName, overwrite);
    } else {
      realWriter = new JsonOutputFileWriter(apis, baseFileName, overwrite);
    }
  }

  @Override
  public void writeBatch(String source, List<ApiObject> objects) {
    lock.lock();
    try {
      realWriter.writeBatch(source, objects);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void printOutput(String apiToPrint) {
    lock.lock();
    try {
      realWriter.printOutput(apiToPrint);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void close() {
    lock.lock();
    try {
      realWriter.close();
    } finally {
      lock.unlock();
    }
  }
}
