package com.voronina.course;

import com.google.gson.Gson;

public interface ApiObject {
  Gson toGson();
  String[] toCsvFields();
  String[] csvHeaders();
}
