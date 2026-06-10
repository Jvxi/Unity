package com.jvxi.unity.novel.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.model.AiSettings;
import com.jvxi.unity.novel.model.Project;
import com.jvxi.unity.novel.model.ProjectImportAnalysis;
import com.jvxi.unity.novel.service.ProjectImportService;
import com.jvxi.unity.novel.service.ProjectNormalizer;
import com.jvxi.unity.novel.service.ProjectStore;

@RestController
@RequestMapping("/api/novels/project/import")
public class ProjectImportController {
    private final ProjectStore projectStore;
    private final ProjectNormalizer normalizer;
    private final ProjectImportService projectImportService;
    private final ObjectMapper objectMapper;

    public ProjectImportController(
        ProjectStore projectStore,
        ProjectNormalizer normalizer,
        ProjectImportService projectImportService,
        ObjectMapper objectMapper
    ) {
        this.projectStore = projectStore;
        this.normalizer = normalizer;
        this.projectImportService = projectImportService;
        this.objectMapper = objectMapper;
    }

    /** @deprecated 请使用 {@link #analyzeOutline} 与 {@link #analyzeChapters} 分拆接口 */
    @Deprecated
    @PostMapping("/analyze")
    public ProjectImportAnalysis analyze(@RequestBody(required = false) Map<String, Object> payload) {
        Project project = projectForPayload(payload);
        String outlineText = stringValue(payload, "outlineText");
        String chaptersText = stringValue(payload, "chaptersText");
        return projectImportService.analyze(project, outlineText, chaptersText);
    }

    @PostMapping("/analyze/outline")
    public ProjectImportAnalysis analyzeOutline(@RequestBody(required = false) Map<String, Object> payload) {
        Project project = projectForPayload(payload);
        String outlineText = stringValue(payload, "outlineText");
        return projectImportService.analyzeOutline(project, outlineText);
    }

    @PostMapping("/analyze/chapters")
    public ProjectImportAnalysis analyzeChapters(@RequestBody(required = false) Map<String, Object> payload) {
        Project project = projectForPayload(payload);
        String chaptersText = stringValue(payload, "chaptersText");
        return projectImportService.analyzeChapters(project, chaptersText);
    }

    private Project projectForPayload(Map<String, Object> payload) {
        Project project = normalizer.normalize(projectStore.loadActiveProject().project());
        Object rawSettings = payload == null ? null : payload.get("aiSettings");
        if (rawSettings == null) {
            return project;
        }
        AiSettings aiSettings = objectMapper.convertValue(rawSettings, AiSettings.class);
        return normalizer.withTransientAiSettings(project, aiSettings);
    }

    private String stringValue(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}


