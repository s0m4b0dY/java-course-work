package com.voronina.course.agentnexusapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentNexusEntryTest {
  @Test
  void jsonAndCsvConversionWorks() {
    String json = """
        {
          "slug":"resend",
          "name":"Resend",
          "category":"api",
          "summary":"Transactional email API",
          "match_score":12.5,
          "matched_on":["capability","summary"],
          "capabilities":["send email","webhook"],
          "call":{
            "endpoint":"https://api.resend.com/emails",
            "auth_mode":"bearer",
            "auth_params":["Authorization"],
            "input_format":"json",
            "output_format":"json",
            "rate_limit":"2 req/s",
            "pricing":{"tier":"free"},
            "example":{"method":"POST"},
            "docs_url":"https://resend.com/docs"
          },
          "trust":{
            "verified":true,
            "reliability_score":100,
            "uptime":99.99,
            "samples":50,
            "avg_latency_ms":120.5,
            "last_probe_ok":true,
            "last_probe_at":"2026-09-22T10:00:00Z"
          },
          "report":{"available":true}
        }
        """;

    AgentNexusEntry entry = AgentNexusEntry.fromJson(json);
    entry.setSearchNeed("send a transactional email");
    String[] fields = entry.toCsvFields();

    assertEquals("send a transactional email", entry.getSearchNeed());
    assertEquals("resend", entry.getSlug());
    assertEquals("Resend", entry.getName());
    assertEquals("api", entry.getCategory());
    assertEquals("Transactional email API", entry.getSummary());
    assertEquals(12.5, entry.getMatchScore());
    assertEquals(2, entry.getCapabilities().size());
    assertEquals("https://api.resend.com/emails", entry.getCall().getEndpoint());
    assertEquals("bearer", entry.getCall().getAuthMode());
    assertEquals("json", entry.getCall().getInputFormat());
    assertEquals("json", entry.getCall().getOutputFormat());
    assertTrue(entry.getTrust().isVerified());
    assertEquals(100.0, entry.getTrust().getReliabilityScore());
    assertEquals(99.99, entry.getTrust().getUptime());
    assertEquals(50, entry.getTrust().getSamples());
    assertEquals(120.5, entry.getTrust().getAvgLatencyMs());
    assertTrue(entry.getTrust().isLastProbeOk());
    assertEquals("2026-09-22T10:00:00Z", entry.getTrust().getLastProbeAt());
    assertNotNull(entry.getReport());

    assertEquals("send a transactional email", fields[0]);
    assertEquals("resend", fields[1]);
    assertEquals("Resend", fields[2]);
    assertEquals("[\"capability\",\"summary\"]", fields[6]);
    assertEquals("send email;webhook", fields[7]);
    assertEquals("https://api.resend.com/emails", fields[8]);
    assertEquals("2 req/s", fields[12]);
    assertEquals("{\"tier\":\"free\"}", fields[13]);
    assertEquals("true", fields[15]);
    assertArrayEquals(AgentNexusEntry.CSV_HEADERS, entry.csvHeaders());
    assertNotNull(entry.toGson());
    assertTrue(entry.toJson().contains("Resend"));
    assertTrue(entry.toJson().contains("search_need"));
  }

  @Test
  void missingNestedObjectsProduceEmptyCsvFields() {
    AgentNexusEntry entry = AgentNexusEntry.fromJson("""
        {
          "slug":"simple",
          "name":"Simple",
          "category":"cli",
          "summary":null,
          "match_score":1
        }
        """);

    String[] fields = entry.toCsvFields();

    assertEquals("", fields[0]);
    assertEquals("simple", fields[1]);
    assertEquals("", fields[4]);
    assertEquals("", fields[6]);
    assertEquals("", fields[7]);
    assertEquals("", fields[8]);
    assertEquals("", fields[15]);
    assertEquals(AgentNexusEntry.CSV_HEADERS.length, fields.length);
  }

  @Test
  void primitiveMatchedOnAndPricingAreFlattenedNicely() {
    AgentNexusEntry entry = AgentNexusEntry.fromJson("""
        {
          "slug":"simple",
          "matched_on":"summary",
          "call":{
            "rate_limit":5,
            "pricing":"free"
          }
        }
        """);

    String[] fields = entry.toCsvFields();

    assertEquals("summary", fields[6]);
    assertEquals("5", fields[12]);
    assertEquals("free", fields[13]);
  }
}
