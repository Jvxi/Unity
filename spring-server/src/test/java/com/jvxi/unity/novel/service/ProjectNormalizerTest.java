package com.jvxi.unity.novel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.model.AiSettings;
import com.jvxi.unity.novel.model.Project;
import com.jvxi.unity.novel.persistence.ProjectJsonMapper;
import org.junit.jupiter.api.Test;

class ProjectNormalizerTest {

    private final ProjectNormalizer normalizer = new ProjectNormalizer(
        new PublishPlatformCatalog(),
        new NovelTypeCatalog(),
        new SystemPromptComposer()
    );
    private final ProjectFactory projectFactory = new ProjectFactory();
    private final ProjectJsonMapper jsonMapper = new ProjectJsonMapper(new ObjectMapper());

    @Test
    void persistenceJsonIsVersionedAndClearsLocalOnlyAiSettings() throws Exception {
        Project project = normalizer.withTransientAiSettings(projectFactory.createBlankProject(), sensitiveSettings());

        Project persisted = normalizer.normalize(project);
        String json = jsonMapper.toJson(persisted);
        JsonNode root = new ObjectMapper().readTree(json);

        assertEquals(1, root.path("schemaVersion").asInt());
        assertEquals(1, root.path("revision").asLong());
        assertTrue(root.hasNonNull("updatedAt"));
        assertTrue(root.hasNonNull("project"));
        assertFalse(persisted.aiSettings().enabled());
        assertEquals("", persisted.aiSettings().baseUrl());
        assertEquals("", persisted.aiSettings().apiKey());
        assertEquals("", persisted.aiSettings().model());
        assertEquals("", persisted.aiSettings().systemPrompt());
        assertFalse(json.contains("sk-test-secret"));
        assertFalse(json.contains("relay.example.com"));
        assertFalse(json.contains("custom-model"));
    }

    @Test
    void projectJsonMapperReadsVersionedAndLegacyProjectJson() throws Exception {
        Project project = normalizer.normalize(projectFactory.createBlankProject());
        String versionedJson = jsonMapper.toJson(project, 7);
        String legacyJson = new ObjectMapper().writeValueAsString(project);

        assertEquals(project.meta().title(), jsonMapper.fromJson(versionedJson).meta().title());
        assertEquals(project.meta().title(), jsonMapper.fromJson(legacyJson).meta().title());
        assertEquals(8, jsonMapper.nextRevision(versionedJson));
        assertEquals(1, jsonMapper.nextRevision(legacyJson));
    }

    @Test
    void transientAiSettingsRemainAvailableForOneRequest() {
        Project project = normalizer.withTransientAiSettings(projectFactory.createBlankProject(), sensitiveSettings());

        assertTrue(project.aiSettings().enabled());
        assertEquals("https://relay.example.com/v1", project.aiSettings().baseUrl());
        assertEquals("sk-test-secret", project.aiSettings().apiKey());
        assertEquals("custom-model", project.aiSettings().model());

        Project sanitized = normalizer.sanitizeForPersistence(project);
        assertEquals("", sanitized.aiSettings().apiKey());
        assertEquals("", sanitized.aiSettings().baseUrl());
        assertEquals("", sanitized.aiSettings().model());
    }

    private AiSettings sensitiveSettings() {
        return new AiSettings(
            true,
            "custom",
            "https://relay.example.com/v1",
            "sk-test-secret",
            "custom-model",
            0.8,
            3000,
            128000,
            "额外系统提示"
        );
    }
}
