package com.voronina.course;

import java.io.IOException;

public interface Api {
  String name();
  ApiObject[] fetchData() throws IOException, InterruptedException;
}
