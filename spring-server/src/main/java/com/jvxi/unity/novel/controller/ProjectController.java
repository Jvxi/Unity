package com.jvxi.unity.novel.controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jvxi.unity.novel.auth.UserContext;
import com.jvxi.unity.novel.model.AiSettings;
import com.jvxi.unity.novel.model.ChapterGenerationResponse;
import com.jvxi.unity.novel.model.Project;
import com.jvxi.unity.novel.model.ProjectEnvelope;
import com.jvxi.unity.novel.model.NovelTypeCatalogResponse;
import com.jvxi.unity.novel.model.PublishPlatformInfo;
import com.jvxi.unity.novel.service.GenerationService;
import com.jvxi.unity.novel.service.NovelTypeCatalog;
import com.jvxi.unity.novel.service.ProjectNormalizer;
import com.jvxi.unity.novel.service.ProjectStore;
import com.jvxi.unity.novel.service.PublishPlatformCatalog;
import com.jvxi.unity.novel.service.TokenUsageService;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/novels")
public class ProjectController {
    private final ProjectStore projectStore;
    private final ProjectNormalizer normalizer;
    private final GenerationService generationService;
    private final PublishPlatformCatalog publishPlatformCatalog;
    private final NovelTypeCatalog novelTypeCatalog;
    private final TokenUsageService tokenUsageService;

    public ProjectController(
        ProjectStore projectStore,
        ProjectNormalizer normalizer,
        GenerationService generationService,
        PublishPlatformCatalog publishPlatformCatalog,
        NovelTypeCatalog novelTypeCatalog,
        TokenUsageService tokenUsageService
    ) {
        this.projectStore = projectStore;
        this.normalizer = normalizer;
        this.generationService = generationService;
        this.publishPlatformCatalog = publishPlatformCatalog;
        this.novelTypeCatalog = novelTypeCatalog;
        this.tokenUsageService = tokenUsageService;
    }

    @GetMapping("/health")
    public Map<String, Boolean> health() {
        return Map.of("ok", true);
    }

    @GetMapping("/token-usage")
    public Map<String, Object> tokenUsage() {
        ProjectEnvelope envelope = projectStore.loadActiveProject();
        AiSettings settings = envelope.project().aiSettings();
        return tokenUsageForSettings(settings);
    }

    @PostMapping("/token-usage")
    public Map<String, Object> tokenUsage(@RequestBody(required = false) AiSettingsRequest request) {
        AiSettings settings = request == null ? null : request.aiSettings();
        return tokenUsageForSettings(normalizer.normalizeAiSettingsOnly(settings));
    }

    private Map<String, Object> tokenUsageForSettings(AiSettings settings) {
        String model = settings.model();

        TokenUsageService.TokenUsageSnapshot snapshot = tokenUsageService.getUsageWithRemoteQuota(settings);

        return Map.of(
            "promptTokens", snapshot.promptTokens(),
            "completionTokens", snapshot.completionTokens(),
            "totalTokens", snapshot.totalTokens(),
            "maxTokens", snapshot.maxTokens(),
            "remainingTokens", snapshot.remainingTokens(),
            "usagePercent", Math.round(snapshot.usagePercent() * 100.0) / 100.0,
            "model", model == null ? "" : model
        );
    }

    @GetMapping("/project")
    public ProjectEnvelope getProject() {
        return projectStore.loadActiveProject();
    }

    @PutMapping("/project")
    public ProjectEnvelope saveProject(@RequestBody Project project) {
        Project normalized = normalizer.normalize(project);
        ProjectEnvelope saved = projectStore.saveActiveProject(normalized);
        return new ProjectEnvelope(saved.bookId(), normalizer.normalize(saved.project()));
    }

    @GetMapping("/platforms")
    public List<PublishPlatformInfo> listPlatforms() {
        return publishPlatformCatalog.listAll();
    }

    @GetMapping("/novel-types")
    public NovelTypeCatalogResponse listNovelTypes() {
        return novelTypeCatalog.catalog();
    }

    @PostMapping("/chapters/{chapterId}/generate")
    public ChapterGenerationResponse generateChapter(
            @PathVariable String chapterId,
            @RequestBody(required = false) AiSettingsRequest request) {
        return generationService.generateChapter(chapterId, request == null ? null : request.aiSettings());
    }

    @PostMapping(value = "/chapters/review/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamReviewChapters(@RequestBody(required = false) AiSettingsRequest request) {
        SseEmitter emitter = new SseEmitter(600_000L);
        String userId = UserContext.requireUserId();
        AiSettings aiSettings = request == null ? null : request.aiSettings();
        CompletableFuture.runAsync(() -> {
            UserContext.setUserId(userId);
            try {
                generationService.streamReviewChapters(emitter, aiSettings);
            } finally {
                UserContext.clear();
            }
        });
        return emitter;
    }

    @PostMapping(value = "/chapters/{chapterId}/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGenerateChapter(
            @PathVariable String chapterId,
            @RequestParam(name = "continue", required = false, defaultValue = "false") boolean continueMode,
            @RequestBody(required = false) AiSettingsRequest request) {
        SseEmitter emitter = new SseEmitter(240_000L);
        String userId = UserContext.requireUserId();
        AiSettings aiSettings = request == null ? null : request.aiSettings();
        CompletableFuture.runAsync(() -> {
            UserContext.setUserId(userId);
            try {
                generationService.streamGenerateChapter(chapterId, emitter, continueMode, aiSettings);
            } finally {
                UserContext.clear();
            }
        });
        return emitter;
    }

    @PostMapping("/ai/test")
    public Map<String, Object> testAiConnection(@RequestBody AiSettings aiSettings) {
        return generationService.testConnection(aiSettings);
    }

    @GetMapping("/system-prompt/preview")
    public Map<String, String> previewSystemPrompt() {
        Project project = normalizer.normalize(projectStore.loadActiveProject().project());
        return Map.of("prompt", generationService.previewSystemPrompt(project));
    }

    private record AiSettingsRequest(AiSettings aiSettings) {
    }
}


