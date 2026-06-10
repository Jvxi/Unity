package com.jvxi.unity.novel.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.jvxi.unity.novel.model.PublishPlatformInfo;

@Component
public class PublishPlatformCatalog {
    private static final String DEFAULT_PLATFORM_ID = "qidian";
    private static final Set<String> ALLOWED_PLATFORM_IDS = Set.of("fanqie", "qidian", "qq");

    private static final List<PublishPlatformInfo> PLATFORMS = List.of(
        new PublishPlatformInfo(
            "fanqie",
            "番茄小说",
            "快节奏、强钩子，适合移动端碎片化阅读。",
            List.of(
                "开篇 300 字内进入冲突或异常事件。",
                "段落宜短，减少大段背景铺陈。",
                "禁止“帮助读者理解”类旁白，直接写角色所见所感。"
            )
        ),
        new PublishPlatformInfo(
            "qidian",
            "起点中文网",
            "男频长篇连载，强调爽点节奏与章节钩子。",
            List.of(
                "章节结尾保留悬念或情绪高点，避免总结式收尾。",
                "对话与动作推进优先，减少作者旁白式说明。",
                "单章建议 2000-4000 字，避免过短断档。"
            )
        ),
        new PublishPlatformInfo(
            "qq",
            "QQ阅读",
            "综合阅读平台，注重可读性与稳定更新。",
            List.of(
                "章节内保持单一主冲突推进，不写剧情结构说明。",
                "禁止“接下来/下一章/读者将会看到”等预告式语句。",
                "正文只写当下场景，不帮读者总结故事线走向。"
            )
        )
    );

    public List<PublishPlatformInfo> listAll() {
        return PLATFORMS;
    }

    public PublishPlatformInfo resolve(String platformId) {
        String normalized = normalizePlatformId(platformId);
        Optional<PublishPlatformInfo> matched = PLATFORMS.stream()
            .filter(platform -> platform.id().equals(normalized))
            .findFirst();
        return matched.orElseGet(() -> PLATFORMS.stream()
            .filter(platform -> platform.id().equals(DEFAULT_PLATFORM_ID))
            .findFirst()
            .orElse(PLATFORMS.getFirst()));
    }

    public String normalizePlatformId(String platformId) {
        String normalized = platformId == null ? "" : platformId.trim().toLowerCase(Locale.ROOT);
        if (ALLOWED_PLATFORM_IDS.contains(normalized)) {
            return normalized;
        }
        return DEFAULT_PLATFORM_ID;
    }
}

