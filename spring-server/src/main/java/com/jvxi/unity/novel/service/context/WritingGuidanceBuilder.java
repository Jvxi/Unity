package com.jvxi.unity.novel.service.context;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class WritingGuidanceBuilder {

    /**
     * 构建题材特定写作指导
     */
    public Map<String, Object> buildGenreGuidance(String genre, int chapterNumber) {
        return switch (genre) {
            case "玄幻" -> buildXuanhuanGuidance(chapterNumber);
            case "都市" -> buildUrbanGuidance(chapterNumber);
            case "仙侠" -> buildXianxiaGuidance(chapterNumber);
            case "科幻" -> buildSciFiGuidance(chapterNumber);
            case "悬疑" -> buildMysteryGuidance(chapterNumber);
            case "言情" -> buildRomanceGuidance(chapterNumber);
            case "历史" -> buildHistoricalGuidance(chapterNumber);
            case "军事" -> buildMilitaryGuidance(chapterNumber);
            case "游戏" -> buildGameGuidance(chapterNumber);
            case "体育" -> buildSportsGuidance(chapterNumber);
            case "灵异" -> buildHorrorGuidance(chapterNumber);
            default -> buildDefaultGuidance(genre, chapterNumber);
        };
    }

    /**
     * 构建方法论策略卡
     */
    public Map<String, Object> buildMethodologyCard(String genre, int chapterNumber) {
        return Map.of(
                "genre", genre,
                "chapter", chapterNumber,
                "strategies", getStrategies(genre),
                "techniques", getTechniques(genre),
                "common_mistakes", getCommonMistakes(genre)
        );
    }

    /**
     * 构建写作检查清单
     */
    public Map<String, Object> buildWritingChecklist(String genre, int chapterNumber) {
        return Map.of(
                "pre_write", List.of(
                        "确认章节目标明确",
                        "检查大纲锚点覆盖",
                        "确认角色状态一致",
                        "检查时间线连续性"
                ),
                "during_write", List.of(
                        "保持视角一致",
                        "控制节奏张弛",
                        "埋设伏笔钩子",
                        "避免旁白解说"
                ),
                "post_write", List.of(
                        "检查设定一致性",
                        "验证逻辑自洽",
                        "优化对话自然度",
                        "确认字数达标"
                )
        );
    }

    // ============ 私有辅助方法 ============

    private Map<String, Object> buildXuanhuanGuidance(int chapterNumber) {
        return Map.of(
                "genre", "玄幻",
                "focus", "升级打怪、势力争斗、宝物争夺",
                "pacing", chapterNumber < 30 ? "快节奏" : "张弛有度",
                "key_elements", List.of("金手指", "升级体系", "反派势力", "机缘宝物"),
                "avoid", List.of("过度描写日常", "无意义对话", "重复战斗模式")
        );
    }

    private Map<String, Object> buildUrbanGuidance(int chapterNumber) {
        return Map.of(
                "genre", "都市",
                "focus", "人物关系、事业线、情感线",
                "pacing", "生活流与冲突交替",
                "key_elements", List.of("职场斗争", "情感纠葛", "家庭关系", "社会现实"),
                "avoid", List.of("过度装逼", "逻辑漏洞", "脱离现实")
        );
    }

    private Map<String, Object> buildXianxiaGuidance(int chapterNumber) {
        return Map.of(
                "genre", "仙侠",
                "focus", "修仙悟道、宗门争斗、天道法则",
                "pacing", "修炼与战斗交替",
                "key_elements", List.of("修炼体系", "法宝神通", "宗门势力", "天劫渡劫"),
                "avoid", List.of("修炼过程冗长", "境界划分混乱", "战力崩坏")
        );
    }

    private Map<String, Object> buildSciFiGuidance(int chapterNumber) {
        return Map.of(
                "genre", "科幻",
                "focus", "科技设定、未来世界、人类命运",
                "pacing", "悬念驱动",
                "key_elements", List.of("科技体系", "世界观设定", "人性探讨", "未来想象"),
                "avoid", List.of("科技术语堆砌", "逻辑硬伤", "设定矛盾")
        );
    }

    private Map<String, Object> buildMysteryGuidance(int chapterNumber) {
        return Map.of(
                "genre", "悬疑",
                "focus", "谜题设置、线索埋设、真相揭露",
                "pacing", "层层递进",
                "key_elements", List.of("核心谜题", "线索链", "嫌疑人", "反转"),
                "avoid", List.of("线索过于明显", "逻辑漏洞", "结局仓促")
        );
    }

    private Map<String, Object> buildRomanceGuidance(int chapterNumber) {
        return Map.of(
                "genre", "言情",
                "focus", "情感发展、误会冲突、甜蜜互动",
                "pacing", "甜虐交替",
                "key_elements", List.of("感情线", "误会", "第三者", "甜蜜时刻"),
                "avoid", List.of("过度狗血", "角色降智", "情节拖沓")
        );
    }

    private Map<String, Object> buildHistoricalGuidance(int chapterNumber) {
        return Map.of(
                "genre", "历史",
                "focus", "历史背景、权谋斗争、人物命运",
                "pacing", "史诗感",
                "key_elements", List.of("历史事件", "权谋斗争", "人物成长", "时代背景"),
                "avoid", List.of("历史硬伤", "现代思维", "情节戏说")
        );
    }

    private Map<String, Object> buildMilitaryGuidance(int chapterNumber) {
        return Map.of(
                "genre", "军事",
                "focus", "战争场面、战术策略、军人情感",
                "pacing", "紧张激烈",
                "key_elements", List.of("战斗场面", "战术运用", "战友情谊", "家国情怀"),
                "avoid", List.of("战斗描写单调", "军事常识错误", "过度个人英雄主义")
        );
    }

    private Map<String, Object> buildGameGuidance(int chapterNumber) {
        return Map.of(
                "genre", "游戏",
                "focus", "游戏系统、升级打怪、团队合作",
                "pacing", "任务驱动",
                "key_elements", List.of("游戏系统", "装备技能", "团队配合", "BOSS战"),
                "avoid", List.of("数据堆砌", "游戏术语过多", "情节单一")
        );
    }

    private Map<String, Object> buildSportsGuidance(int chapterNumber) {
        return Map.of(
                "genre", "体育",
                "focus", "比赛训练、成长励志、团队精神",
                "pacing", "比赛与日常交替",
                "key_elements", List.of("比赛场面", "训练过程", "团队合作", "个人成长"),
                "avoid", List.of("比赛描写单调", "专业术语过多", "情节老套")
        );
    }

    private Map<String, Object> buildHorrorGuidance(int chapterNumber) {
        return Map.of(
                "genre", "灵异",
                "focus", "恐怖氛围、悬念设置、真相揭露",
                "pacing", "渐进式恐怖",
                "key_elements", List.of("恐怖元素", "悬念设置", "心理描写", "真相揭露"),
                "avoid", List.of("过度血腥", "逻辑漏洞", "结局仓促")
        );
    }

    private Map<String, Object> buildDefaultGuidance(String genre, int chapterNumber) {
        return Map.of(
                "genre", genre,
                "focus", "故事推进、人物发展、冲突解决",
                "pacing", "根据情节需要调整",
                "key_elements", List.of("情节推进", "人物塑造", "冲突设置", "伏笔埋设"),
                "avoid", List.of("情节拖沓", "人物扁平", "逻辑漏洞")
        );
    }

    private List<String> getStrategies(String genre) {
        return switch (genre) {
            case "玄幻" -> List.of("升级打怪策略", "势力争斗策略", "宝物获取策略");
            case "都市" -> List.of("职场晋升策略", "人际关系策略", "危机处理策略");
            case "言情" -> List.of("感情推进策略", "误会设置策略", "甜蜜时刻策略");
            default -> List.of("情节推进策略", "人物发展策略", "冲突解决策略");
        };
    }

    private List<String> getTechniques(String genre) {
        return switch (genre) {
            case "玄幻" -> List.of("战斗描写技巧", "升级体系设计", "反派塑造技巧");
            case "都市" -> List.of("对话描写技巧", "心理描写技巧", "场景描写技巧");
            case "言情" -> List.of("情感描写技巧", "误会设置技巧", "甜蜜互动技巧");
            default -> List.of("叙事技巧", "描写技巧", "对话技巧");
        };
    }

    private List<String> getCommonMistakes(String genre) {
        return switch (genre) {
            case "玄幻" -> List.of("战力崩坏", "升级过快", "反派智商下线");
            case "都市" -> List.of("过度装逼", "逻辑漏洞", "脱离现实");
            case "言情" -> List.of("过度狗血", "角色降智", "情节拖沓");
            default -> List.of("情节拖沓", "人物扁平", "逻辑漏洞");
        };
    }
}

