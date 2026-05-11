package com.voronina.course;

import java.io.IOException;

public interface Api {
  String name();

  String[] csvHeaders();

  ApiObject[] fetchData() throws IOException, InterruptedException;
}