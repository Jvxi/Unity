package com.jvxi.unity.novel.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.jvxi.unity.novel.model.AiSettings;
import com.jvxi.unity.novel.model.Chapter;
import com.jvxi.unity.novel.model.CharacterProfile;
import com.jvxi.unity.novel.model.ForeshadowingItem;
import com.jvxi.unity.novel.model.OnboardingState;
import com.jvxi.unity.novel.model.OutlineNode;
import com.jvxi.unity.novel.model.Project;
import com.jvxi.unity.novel.model.ProjectMeta;

@Component
public class ProjectFactory {
    /** 新建书籍 / 新用户：空白项目，不含示例大纲与章节 */
    public Project createBlankProject() {
        ProjectMeta meta = new ProjectMeta(
            "",
            "",
            "",
            "",
            "",
            "",
            List.of(),
            List.of(),
            true,
            "qidian",
            "male",
            "xuanyi"
        );

        AiSettings aiSettings = new AiSettings(
            false,
            "openai-compatible",
            "https://api.openai.com/v1",
            "",
            "gpt-4o-mini",
            0.7,
            1800,
            0,
            ""
        );

        OnboardingState onboarding = new OnboardingState(false, List.of(), List.of());

        return new Project(
            meta,
            aiSettings,
            onboarding,
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            Instant.now().toString()
        );
    }

    /** 演示用完整样例（仅旧数据迁移等场景保留） */
    public Project createDefaultProject() {
        OutlineNode node1 = new OutlineNode(
            id(),
            1,
            "委托引子",
            "主角接到失踪案委托，线索指向封存多年的事故区。",
            "建立案件主线和悬疑氛围。",
            "委托人隐瞒关键信息，主角必须先判断可信度。",
            List.of("主角决定接手调查", "封存事故区被明确提及"),
            List.of("提前揭露幕后黑手", "完整解释历史事故真相")
        );

        OutlineNode node2 = new OutlineNode(
            id(),
            2,
            "档案裂缝",
            "主角潜入档案馆，发现事故名单存在篡改痕迹。",
            "升级调查难度并引入同伴冲突。",
            "需要在不惊动监控的情况下取证。",
            List.of("发现名单被篡改", "同伴提供旧报纸线索"),
            List.of("本章直接锁定元凶", "反派正式现身")
        );

        CharacterProfile character1 = new CharacterProfile(
            id(),
            "沈雾",
            "主角 / 调查写手",
            "擅长从碎片信息里重建事实，性格克制。",
            "查清失踪案和历史事故的关联。",
            "害怕再次卷入权力集团冲突。",
            "与同伴互相试探又逐步建立信任。"
        );

        CharacterProfile character2 = new CharacterProfile(
            id(),
            "陆沉",
            "记者同伴",
            "行动果断，掌握旧报社残缺资料。",
            "借调查还原当年被撤稿的真相。",
            "不愿公开全部消息来源。",
            "和主角常有分歧，但关键时刻互补。"
        );

        ForeshadowingItem clue1 = new ForeshadowingItem(
            id(),
            "潮湿录音笔",
            "委托人始终紧握一支进过水的录音笔。",
            "录音中藏有失踪前求救片段。",
            "第3章以后",
            "planned",
            "前期只做动作细节，不直接说明。"
        );

        Chapter chapter1 = new Chapter(
            id(),
            1,
            "雾港委托",
            "主角在旧码头见到委托人，确认失踪地点与事故封锁区重合。",
            "完成案情引入并让主角做出行动决策。",
            List.of(node1.id()),
            List.of(character1.id()),
            List.of(clue1.id()),
            List.of("委托人提到封锁区", "主角发现委托人有所隐瞒", "主角决定继续调查"),
            List.of("提前揭露幕后身份", "直接解释历史事故全貌"),
            "正文保持悬疑推进，不写设定说明。",
            ""
        );

        Chapter chapter2 = new Chapter(
            id(),
            2,
            "被涂改的名字",
            "主角和同伴潜入档案馆，发现事故名单异常。",
            "推进主线并强化合作张力。",
            List.of(node2.id()),
            List.of(character1.id(), character2.id()),
            List.of(),
            List.of("同伴拿出旧报纸", "两人确认名单被篡改", "意识到有人持续清理痕迹"),
            List.of("直接锁定元凶", "反派公开对峙"),
            "重点写行动与信息落差。",
            ""
        );

        ProjectMeta meta = new ProjectMeta(
            "雾港回声",
            "失踪案牵出十年前事故，主角被迫揭开海港城市共同掩埋的秘密。",
            "男频 · 悬疑",
            "失踪案牵出十年前事故，主角被迫进入一座城市共同掩埋的秘密。",
            "克制、阴冷、持续施压",
            "连载长篇，单章 2500-4000 字",
            List.of(
                "输出仅包含小说正文，不输出解释、注释或列表。",
                "人物行为必须符合既有动机和限制。",
                "冲突推进优先通过行动、对话和观察体现。"
            ),
            List.of(
                "核心场景是常年潮湿多雾的海港城市。",
                "十年前事故的公开结论不可信。",
                "权力集团会同时清理档案和舆论痕迹。"
            ),
            true,
            "qidian",
            "male",
            "xuanyi"
        );

        AiSettings aiSettings = new AiSettings(
            false,
            "openai-compatible",
            "https://api.openai.com/v1",
            "",
            "gpt-4o-mini",
            0.7,
            1800,
            0,
            ""
        );

        OnboardingState onboarding = new OnboardingState(false, List.of(), List.of());

        return new Project(
            meta,
            aiSettings,
            onboarding,
            List.of(node1, node2),
            List.of(character1, character2),
            List.of(clue1),
            List.of(chapter1, chapter2),
            Instant.now().toString()
        );
    }

    private String id() {
        return UUID.randomUUID().toString();
    }
}

