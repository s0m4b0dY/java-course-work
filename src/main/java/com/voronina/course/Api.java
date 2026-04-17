package com.voronina.course;

import java.io.IOException;

public interface Api {
  ApiObject fetchData() throws IOException, InterruptedException;
}
