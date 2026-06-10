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
import org.springframework.web.bind.annotation.GetMapping;
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
import com.jvxi.unity.novel.model.OnboardingState;
import com.jvxi.unity.novel.model.Project;
import com.jvxi.unity.novel.service.OpeningQuestionnaireService;
import com.jvxi.unity.novel.service.ProjectNormalizer;
import com.jvxi.unity.novel.service.ProjectStore;
import com.jvxi.unity.novel.service.ProjectValidator;

@RestController
@RequestMapping("/api/novels/onboarding")
public class OnboardingController {
    private final ProjectStore projectStore;
    private final ProjectNormalizer normalizer;
    private final ProjectValidator validator;
    private final OpeningQuestionnaireService openingQuestionnaireService;
    private final ObjectMapper objectMapper;

    public OnboardingController(
        ProjectStore projectStore,
        ProjectNormalizer normalizer,
        ProjectValidator validator,
        OpeningQuestionnaireService openingQuestionnaireService,
        ObjectMapper objectMapper
    ) {
        this.projectStore = projectStore;
        this.normalizer = normalizer;
        this.validator = validator;
        this.openingQuestionnaireService = openingQuestionnaireService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/questions")
    public List<OnboardingQuestion> listQuestions() {
        Project project = normalizer.normalize(projectStore.loadActiveProject().project());
        return project.onboarding().questions();
    }

    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGenerateQuestions(@RequestBody(required = false) AiSettingsRequest request) {
        Project project = projectForRequest(request);
        validator.validateForOnboardingQuestions(project);

        SseEmitter emitter = new SseEmitter(300_000L);
        String userId = UserContext.requireUserId();
        CompletableFuture.runAsync(() -> {
            UserContext.setUserId(userId);
            try {
                List<OnboardingQuestion> collected = new ArrayList<>();
                try {
                    openingQuestionnaireService.streamQuestions(project, question -> {
                        collected.add(question);
                        sendEvent(emitter, "question", question);
                    });
                    collected.sort(Comparator.comparing(OnboardingQuestion::id));
                    OnboardingState onboarding = openingQuestionnaireService.withGeneratedQuestions(collected);
                    Project updated = new Project(
                        project.meta(),
                        project.aiSettings(),
                        onboarding,
                        project.outlineNodes(),
                        project.characters(),
                        project.foreshadowing(),
                        project.chapters(),
                        project.updatedAt()
                    );
                    var saved = projectStore.saveActiveProject(normalizer.normalize(updated));
                    Map<String, Object> done = new HashMap<>();
                    done.put("type", "done");
                    done.put("bookId", saved.bookId());
                    done.put("project", saved.project());
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

    @PostMapping("/generate")
    public Map<String, Object> generateQuestions(@RequestBody(required = false) AiSettingsRequest request) {
        Project project = projectForRequest(request);
        validator.validateForOnboardingQuestions(project);

        List<OnboardingQuestion> questions = openingQuestionnaireService.generateQuestions(project);
        OnboardingState onboarding = openingQuestionnaireService.withGeneratedQuestions(questions);

        Project updated = new Project(
            project.meta(),
            project.aiSettings(),
            onboarding,
            project.outlineNodes(),
            project.characters(),
            project.foreshadowing(),
            project.chapters(),
            project.updatedAt()
        );
        var saved = projectStore.saveActiveProject(normalizer.normalize(updated));
        return Map.of("questions", questions, "bookId", saved.bookId(), "project", saved.project());
    }

    @PostMapping("/submit")
    public Project submitAnswers(@RequestBody Map<String, List<OnboardingAnswer>> payload) {
        Project project = normalizer.normalize(projectStore.loadActiveProject().project());
        validator.validateForOnboardingQuestions(project);

        List<OnboardingQuestion> questions = project.onboarding().questions();
        if (questions == null || questions.size() != 15) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "请先点击「获取问题」，由 AI 生成 15 个问题后再提交。");
        }

        List<OnboardingAnswer> submitted = payload == null ? List.of() : payload.getOrDefault("answers", List.of());
        OnboardingState onboarding = openingQuestionnaireService.validateAndBuildState(submitted, questions);

        Project updated = new Project(
            project.meta(),
            project.aiSettings(),
            onboarding,
            project.outlineNodes(),
            project.characters(),
            project.foreshadowing(),
            project.chapters(),
            project.updatedAt()
        );
        var saved = projectStore.saveActiveProject(normalizer.normalize(updated));
        return saved.project();
    }

    private void sendEvent(SseEmitter emitter, String type, Object payload) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("type", type);
            event.put("question", payload);
            emitter.send(SseEmitter.event().name("message").data(event, MediaType.APPLICATION_JSON));
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "推送问卷进度失败。");
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
}


