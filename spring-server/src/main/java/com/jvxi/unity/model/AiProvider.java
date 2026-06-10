package com.jvxi.unity.model;

import java.util.List;

public class AiProvider {
    private String id;
    private String name;
    private String apiUrl;
    private String apiFormat;
    private Integer maxTokens;
    private List<AiModel> models;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public String getApiFormat() { return apiFormat; }
    public void setApiFormat(String apiFormat) { this.apiFormat = apiFormat; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public List<AiModel> getModels() { return models; }
    public void setModels(List<AiModel> models) { this.models = models; }
}
