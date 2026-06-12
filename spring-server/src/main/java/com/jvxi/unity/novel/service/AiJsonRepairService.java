package com.jvxi.unity.novel.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AiJsonRepairService {
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;

    public AiJsonRepairService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode readTree(String raw) throws JsonProcessingException {
        String payload = extractJsonPayload(raw);
        try {
            return objectMapper.readTree(normalizeJsonText(payload));
        } catch (JsonProcessingException exception) {
            return objectMapper.readTree(repairJson(payload));
        }
    }

    public String extractJsonPayload(String raw) {
        String trimmed = sanitizeRaw(raw);
        if (trimmed.isBlank()) {
            return "{}";
        }

        Matcher matcher = JSON_BLOCK.matcher(trimmed);
        if (matcher.find()) {
            trimmed = sanitizeRaw(matcher.group(1));
        }

        BalancedJson objectJson = findBalancedJson(trimmed, '{', '}');
        BalancedJson arrayJson = findBalancedJson(trimmed, '[', ']');
        if (objectJson != null && arrayJson != null) {
            return objectJson.start() <= arrayJson.start() ? objectJson.value() : arrayJson.value();
        }
        if (objectJson != null) {
            return objectJson.value();
        }
        if (arrayJson != null) {
            return arrayJson.value();
        }

        int firstObject = trimmed.indexOf('{');
        int firstArray = trimmed.indexOf('[');
        int start = firstJsonStart(firstObject, firstArray);
        return start >= 0 ? trimmed.substring(start).trim() : trimmed;
    }

    public String normalizeJsonText(String json) {
        return sanitizeRaw(json)
            .replaceAll(",\\s*}", "}")
            .replaceAll(",\\s*]", "]");
    }

    public String repairJson(String json) {
        String repaired = normalizeJsonText(json);
        if (repaired.isBlank()) {
            return "{}";
        }

        String closed = closeUnbalancedJson(repaired);
        return closed
            .replaceAll(",\\s*}", "}")
            .replaceAll(",\\s*]", "]");
    }

    private BalancedJson findBalancedJson(String text, char open, char close) {
        int start = text.indexOf(open);
        if (start < 0) {
            return null;
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = start; index < text.length(); index++) {
            char current = text.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }

            if (current == '"') {
                inString = true;
            } else if (current == open) {
                depth++;
            } else if (current == close) {
                depth--;
                if (depth == 0) {
                    return new BalancedJson(start, text.substring(start, index + 1).trim());
                }
            }
        }
        return null;
    }

    private String closeUnbalancedJson(String text) {
        StringBuilder builder = new StringBuilder(text);
        Deque<Character> stack = new ArrayDeque<>();
        boolean inString = false;
        boolean escaped = false;

        for (int index = 0; index < builder.length(); index++) {
            char current = builder.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }

            if (current == '"') {
                inString = true;
            } else if (current == '{') {
                stack.push('}');
            } else if (current == '[') {
                stack.push(']');
            } else if ((current == '}' || current == ']') && !stack.isEmpty() && stack.peek() == current) {
                stack.pop();
            }
        }

        if (inString) {
            builder.append('"');
        }
        while (!stack.isEmpty()) {
            builder.append(stack.pop());
        }
        return builder.toString();
    }

    private int firstJsonStart(int firstObject, int firstArray) {
        if (firstObject < 0) {
            return firstArray;
        }
        if (firstArray < 0) {
            return firstObject;
        }
        return Math.min(firstObject, firstArray);
    }

    private String sanitizeRaw(String raw) {
        return raw == null
            ? ""
            : raw
                .replace('\uFEFF', ' ')
                .replace('\u201c', '"')
                .replace('\u201d', '"')
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .trim();
    }

    private record BalancedJson(int start, String value) {
    }
}
