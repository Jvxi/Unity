package com.jvxi.unity.novel.service.context;

import com.jvxi.unity.novel.model.memory.MemoryPack;
import com.jvxi.unity.novel.model.story.ChapterBrief;
import com.jvxi.unity.novel.model.story.MasterSetting;
import com.jvxi.unity.novel.service.memory.MemoryService;
import com.jvxi.unity.novel.service.story.StorySystemService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ContextService {

    private final MemoryService memoryService;
    private final StorySystemService storySystemService;
    private final ContextRanker contextRanker;
    private final WritingGuidanceBuilder writingGuidanceBuilder;

    public ContextService(
            MemoryService memoryService,
            StorySystemService storySystemService,
            ContextRanker contextRanker,
            WritingGuidanceBuilder writingGuidanceBuilder
    ) {
        this.memoryService = memoryService;
        this.storySystemService = storySystemService;
        this.contextRanker = contextRanker;
        this.writingGuidanceBuilder = writingGuidanceBuilder;
    }

    /**
     * 组装上下文包
     */
    public Map<String, Object> assembleContext(String bookId, int chapterNumber, String taskType) {
        Map<String, Object> context = new HashMap<>();

        // 1. 故事合同
        Optional<MasterSetting> masterSetting = storySystemService.getMasterSetting(bookId);
        masterSetting.ifPresent(setting -> context.put("story_contracts", Map.of("master", setting)));

        Optional<ChapterBrief> chapterBrief = storySystemService.getChapterBrief(bookId, chapterNumber);
        chapterBrief.ifPresent(brief -> context.put("chapter_brief", brief));

        // 2. 记忆包
        MemoryPack memoryPack = memoryService.buildMemoryPack(bookId, chapterNumber, taskType);
        context.put("memory_pack", memoryPack);

        // 3. 写作指导
        String genre = masterSetting.map(s -> (String) s.route().get("primary_genre")).orElse("default");
        Map<String, Object> writingGuidance = writingGuidanceBuilder.buildGenreGuidance(genre, chapterNumber);
        context.put("writing_guidance", writingGuidance);

        // 4. 写作检查清单
        Map<String, Object> checklist = writingGuidanceBuilder.buildWritingChecklist(genre, chapterNumber);
        context.put("writing_checklist", checklist);

        // 5. 动态预算
        Map<String, Double> dynamicBudget = contextRanker.getDynamicBudget(chapterNumber);
        context.put("dynamic_budget", dynamicBudget);

        // 6. 元数据
        context.put("metadata", Map.of(
                "chapter", chapterNumber,
                "task_type", taskType,
                "genre", genre,
                "assembled_at", System.currentTimeMillis()
        ));

        return context;
    }

    /**
     * 获取写作指导
     */
    public Map<String, Object> getWritingGuidance(String bookId, int chapterNumber) {
        Optional<MasterSetting> masterSetting = storySystemService.getMasterSetting(bookId);
        String genre = masterSetting.map(s -> (String) s.route().get("primary_genre")).orElse("default");

        Map<String, Object> guidance = writingGuidanceBuilder.buildGenreGuidance(genre, chapterNumber);
        Map<String, Object> methodology = writingGuidanceBuilder.buildMethodologyCard(genre, chapterNumber);
        Map<String, Object> checklist = writingGuidanceBuilder.buildWritingChecklist(genre, chapterNumber);

        return Map.of(
                "guidance", guidance,
                "methodology", methodology,
                "checklist", checklist
        );
    }
}

