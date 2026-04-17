package com.voronina.course;

import org.apache.commons.csv.CSVRecord;

import com.google.gson.Gson;

public interface ApiObject {
  Gson toGson();
  String[] toCsvFields();
  String[] csvHeaders();
}
