package com.jvxi.unity.novel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class AiJsonRepairServiceTest {
    private final AiJsonRepairService service = new AiJsonRepairService(new ObjectMapper());

    @Test
    void extractsMarkdownJsonBlockAndRemovesTrailingCommas() throws Exception {
        JsonNode root = service.readTree("""
            说明文字
            ```json
            {"questions":[{"id":"q01","title":"核心冲突",},],}
            ```
            """);

        assertEquals("核心冲突", root.path("questions").path(0).path("title").asText());
    }

    @Test
    void extractsFirstBalancedJsonObjectFromWrappedText() throws Exception {
        JsonNode root = service.readTree("""
            好的，结果如下：
            {"title":"第一章","summary":"主角入局"} 后续说明不要解析
            """);

        assertEquals("第一章", root.path("title").asText());
        assertEquals("主角入局", root.path("summary").asText());
    }

    @Test
    void normalizesCurlyQuotesBeforeParsing() throws Exception {
        JsonNode root = service.readTree("{“title”:“钩子”,“items”:[“A”,],}");

        assertEquals("钩子", root.path("title").asText());
        assertEquals("A", root.path("items").path(0).asText());
    }

    @Test
    void closesTruncatedObjectWhenPossible() throws Exception {
        JsonNode root = service.readTree("{\"chapter_meta\":{\"hook\":{\"content\":\"新的线索\"");

        assertEquals("新的线索", root.path("chapter_meta").path("hook").path("content").asText());
    }
}
