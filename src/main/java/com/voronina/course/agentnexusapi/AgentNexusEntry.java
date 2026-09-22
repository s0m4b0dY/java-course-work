package com.voronina.course.agentnexusapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.voronina.course.ApiObject;

import java.util.List;
import java.util.stream.Collectors;

public class AgentNexusEntry implements ApiObject {
  public static final String[] CSV_HEADERS = {
      "search_need",
      "slug",
      "name",
      "category",
      "summary",
      "match_score",
      "matched_on",
      "capabilities",
      "endpoint",
      "auth_mode",
      "input_format",
      "output_format",
      "rate_limit",
      "pricing",
      "docs_url",
      "verified",
      "reliability_score",
      "uptime",
      "samples",
      "avg_latency_ms",
      "last_probe_ok",
      "last_probe_at"
  };

  @SerializedName("search_need")
  private String searchNeed;
  private String slug;
  private String name;
  private String category;
  private String summary;

  @SerializedName("match_score")
  private double matchScore;

  @SerializedName("matched_on")
  private JsonElement matchedOn;

  private List<String> capabilities;
  private Call call;
  private Trust trust;

  // The service can add reporting metadata. We preserve it in JSON output,
  // but it is not useful enough to flatten into CSV columns.
  private JsonElement report;

  public static class Call {
    private String endpoint;

    @SerializedName("auth_mode")
    private String authMode;

    @SerializedName("auth_params")
    private JsonElement authParams;

    @SerializedName("input_format")
    private String inputFormat;

    @SerializedName("output_format")
    private String outputFormat;

    @SerializedName("rate_limit")
    private JsonElement rateLimit;

    private JsonElement pricing;
    private JsonElement example;

    @SerializedName("docs_url")
    private String docsUrl;

    public String getEndpoint() { return endpoint; }
    public String getAuthMode() { return authMode; }
    public JsonElement getAuthParams() { return authParams; }
    public String getInputFormat() { return inputFormat; }
    public String getOutputFormat() { return outputFormat; }
    public JsonElement getRateLimit() { return rateLimit; }
    public JsonElement getPricing() { return pricing; }
    public JsonElement getExample() { return example; }
    public String getDocsUrl() { return docsUrl; }
  }

  public static class Trust {
    private boolean verified;

    @SerializedName("reliability_score")
    private double reliabilityScore;

    private double uptime;
    private int samples;

    @SerializedName("avg_latency_ms")
    private double avgLatencyMs;

    @SerializedName("last_probe_ok")
    private boolean lastProbeOk;

    @SerializedName("last_probe_at")
    private String lastProbeAt;

    public boolean isVerified() { return verified; }
    public double getReliabilityScore() { return reliabilityScore; }
    public double getUptime() { return uptime; }
    public int getSamples() { return samples; }
    public double getAvgLatencyMs() { return avgLatencyMs; }
    public boolean isLastProbeOk() { return lastProbeOk; }
    public String getLastProbeAt() { return lastProbeAt; }
  }

  @Override
  public String[] toCsvFields() {
    String capabilitiesText = capabilities != null
        ? capabilities.stream().map(AgentNexusEntry::safe).collect(Collectors.joining(";"))
        : "";

    return new String[] {
        safe(searchNeed),
        safe(slug),
        safe(name),
        safe(category),
        safe(summary),
        String.valueOf(matchScore),
        jsonText(matchedOn),
        capabilitiesText,
        call != null ? safe(call.getEndpoint()) : "",
        call != null ? safe(call.getAuthMode()) : "",
        call != null ? safe(call.getInputFormat()) : "",
        call != null ? safe(call.getOutputFormat()) : "",
        call != null ? jsonText(call.getRateLimit()) : "",
        call != null ? jsonText(call.getPricing()) : "",
        call != null ? safe(call.getDocsUrl()) : "",
        trust != null ? String.valueOf(trust.isVerified()) : "",
        trust != null ? String.valueOf(trust.getReliabilityScore()) : "",
        trust != null ? String.valueOf(trust.getUptime()) : "",
        trust != null ? String.valueOf(trust.getSamples()) : "",
        trust != null ? String.valueOf(trust.getAvgLatencyMs()) : "",
        trust != null ? String.valueOf(trust.isLastProbeOk()) : "",
        trust != null ? safe(trust.getLastProbeAt()) : ""
    };
  }

  @Override
  public String[] csvHeaders() {
    return CSV_HEADERS;
  }

  @Override
  public Gson toGson() {
    return new GsonBuilder().serializeNulls().create();
  }

  public static AgentNexusEntry fromJson(String json) {
    return new GsonBuilder().create().fromJson(json, AgentNexusEntry.class);
  }

  public String toJson() {
    return new GsonBuilder().serializeNulls().create().toJson(this);
  }

  public String getSearchNeed() { return searchNeed; }
  public void setSearchNeed(String searchNeed) { this.searchNeed = searchNeed; }
  public String getSlug() { return slug; }
  public String getName() { return name; }
  public String getCategory() { return category; }
  public String getSummary() { return summary; }
  public double getMatchScore() { return matchScore; }
  public JsonElement getMatchedOn() { return matchedOn; }
  public List<String> getCapabilities() { return capabilities; }
  public Call getCall() { return call; }
  public Trust getTrust() { return trust; }
  public JsonElement getReport() { return report; }

  private static String safe(String value) {
    return value != null ? value : "";
  }

  private static String jsonText(JsonElement value) {
    if (value == null || value.isJsonNull()) {
      return "";
    }
    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
      return value.getAsString();
    }
    return value.toString();
  }
}
