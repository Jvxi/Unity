package com.jvxi.unity.model;

import java.util.List;

public class AiProvider {
    private String id;
    private String name;
    private String apiUrl;
    private List<AiModel> models;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public List<AiModel> getModels() { return models; }
    public void setModels(List<AiModel> models) { this.models = models; }
}