package com.voronina.course;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

class TestObjects {
  static class SimpleObject implements ApiObject {
    private final String[] headers;
    private final String[] values;

    SimpleObject(String[] headers, String[] values) {
      this.headers = headers;
      this.values = values;
    }

    @Override
    public Gson toGson() {
      return new GsonBuilder().serializeNulls().create();
    }

    @Override
    public String[] toCsvFields() {
      return values;
    }

    @Override
    public String[] csvHeaders() {
      return headers;
    }
  }

  static class SimpleApi implements Api {
    private final String name;
    private final String[] headers;
    private final ApiObject[][] batches;
    private int pos = 0;

    SimpleApi(String name, String[] headers, ApiObject[]... batches) {
      this.name = name;
      this.headers = headers;
      this.batches = batches;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String[] csvHeaders() {
      return headers;
    }

    @Override
    public ApiObject[] fetchData() {
      if (batches.length == 0) {
        return new ApiObject[] { new SimpleObject(headers, new String[] { "value" }) };
      }

      ApiObject[] batch = batches[Math.min(pos, batches.length - 1)];
      pos++;
      return batch;
    }
  }
}
