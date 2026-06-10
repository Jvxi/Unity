package com.jvxi.unity.novel.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.auth.UserContext;
import com.jvxi.unity.novel.exception.ApiException;
import com.jvxi.unity.novel.model.AiSettings;
import com.jvxi.unity.novel.model.OnboardingAnswer;
import com.jvxi.unity.novel.model.OnboardingQuestion;
import com.jvxi.unity.novel.model.OutlineBootstrapProposal;
import com.jvxi.unity.novel.model.Project;
import com.jvxi.unity.novel.model.ProjectEnvelope;
import com.jvxi.unity.novel.service.OutlineBootstrapService;
import com.jvxi.unity.novel.service.ProjectNormalizer;
import com.jvxi.unity.novel.service.ProjectStore;
import com.jvxi.unity.novel.service.ProjectValidator;

@RestController
@RequestMapping("/api/novels/outline-bootstrap")
public class OutlineBootstrapController {
    private final ProjectStore projectStore;
    private final ProjectNormalizer normalizer;
    private final ProjectValidator validator;
    private final OutlineBootstrapService outlineBootstrapService;
    private final ObjectMapper objectMapper;

    public OutlineBootstrapController(
        ProjectStore projectStore,
        ProjectNormalizer normalizer,
        ProjectValidator validator,
        OutlineBootstrapService outlineBootstrapService,
        ObjectMapper objectMapper
    ) {
        this.projectStore = projectStore;
        this.normalizer = normalizer;
        this.validator = validator;
        this.outlineBootstrapService = outlineBootstrapService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/questions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamQuestions(@RequestBody(required = false) AiSettingsRequest request) {
        Project project = projectForRequest(request);
        validator.validateForOutlineBootstrap(project);

        SseEmitter emitter = new SseEmitter(300_000L);
        String userId = UserContext.requireUserId();
        CompletableFuture.runAsync(() -> {
            UserContext.setUserId(userId);
            try {
                List<OnboardingQuestion> collected = new ArrayList<>();
                try {
                    outlineBootstrapService.streamQuestions(project, question -> {
                        collected.add(question);
                        sendEvent(emitter, "question", question);
                    });
                    collected.sort(Comparator.comparing(OnboardingQuestion::id));
                    Map<String, Object> done = new HashMap<>();
                    done.put("type", "done");
                    done.put("questions", collected);
                    sendJsonEvent(emitter, done);
                    emitter.complete();
                } catch (Exception exception) {
                    try {
                        Map<String, Object> error = Map.of(
                            "type", "error",
                            "message", exception.getMessage() == null ? "生成失败" : exception.getMessage()
                        );
                        sendJsonEvent(emitter, error);
                    } catch (IOException ignored) {
                        // Ignore secondary failure.
                    }
                    emitter.complete();
                }
            } finally {
                UserContext.clear();
            }
        });
        return emitter;
    }

    @PostMapping("/proposals")
    public Map<String, Object> generateProposals(@RequestBody(required = false) OutlineProposalRequest payload) {
        Project project = payload == null ? projectForRequest(null) : projectForRequest(new AiSettingsRequest(payload.aiSettings()));
        validator.validateForOutlineBootstrap(project);

        List<OnboardingAnswer> answers = payload == null || payload.answers() == null ? List.of() : payload.answers();
        List<OutlineBootstrapProposal> proposals = outlineBootstrapService.generateProposals(project, answers);
        return Map.of("proposals", proposals);
    }

    @PostMapping("/apply")
    public ProjectEnvelope applyProposal(@RequestBody Map<String, Object> payload) {
        Project project = normalizer.normalize(projectStore.loadActiveProject().project());
        validator.validateForOutlineBootstrap(project);

        OutlineBootstrapProposal proposal = objectMapper.convertValue(
            payload == null ? Map.of() : payload.getOrDefault("proposal", Map.of()),
            OutlineBootstrapProposal.class
        );
        Project updated = outlineBootstrapService.applyProposal(project, proposal);
        return projectStore.saveActiveProject(normalizer.normalize(updated));
    }

    private void sendEvent(SseEmitter emitter, String type, Object question) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", type);
            event.put("question", question);
            emitter.send(SseEmitter.event().name("message").data(event, MediaType.APPLICATION_JSON));
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "推送进度失败。");
        }
    }

    private void sendJsonEvent(SseEmitter emitter, Map<String, Object> payload) throws IOException {
        emitter.send(SseEmitter.event().name("message").data(payload, MediaType.APPLICATION_JSON));
    }

    private Project projectForRequest(AiSettingsRequest request) {
        Project project = normalizer.normalize(projectStore.loadActiveProject().project());
        if (request == null || request.aiSettings() == null) {
            return project;
        }
        return normalizer.withTransientAiSettings(project, request.aiSettings());
    }

    private record AiSettingsRequest(AiSettings aiSettings) {
    }

    private record OutlineProposalRequest(AiSettings aiSettings, List<OnboardingAnswer> answers) {
    }
}


