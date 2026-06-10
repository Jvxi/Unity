package com.jvxi.unity.novel.persistence;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.exception.ApiException;
import com.jvxi.unity.novel.model.Project;

@Component
public class ProjectJsonMapper {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private final ObjectMapper objectMapper;

    public ProjectJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(Project project) {
        return toJson(project, 1);
    }

    public String toJson(Project project, long revision) {
        try {
            PersistedProjectEnvelope envelope = new PersistedProjectEnvelope(
                CURRENT_SCHEMA_VERSION,
                Math.max(1, revision),
                project == null ? "" : project.updatedAt(),
                project
            );
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "无法序列化书籍数据。");
        }
    }

    public Project fromJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode projectNode = root.path("project");
            if (!projectNode.isMissingNode() && !projectNode.isNull()) {
                return objectMapper.treeToValue(projectNode, Project.class);
            }
            return objectMapper.treeToValue(root, Project.class);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "无法解析书籍数据。");
        }
    }

    public long nextRevision(String existingJson) {
        if (existingJson == null || existingJson.isBlank()) {
            return 1;
        }
        try {
            JsonNode root = objectMapper.readTree(existingJson);
            long current = root.path("revision").asLong(0);
            return current <= 0 ? 1 : current + 1;
        } catch (JsonProcessingException ignored) {
            return 1;
        }
    }

    private record PersistedProjectEnvelope(
        int schemaVersion,
        long revision,
        String updatedAt,
        Project project
    ) {
    }
}
