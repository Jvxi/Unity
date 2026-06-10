package com.jvxi.unity.novel.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.jvxi.unity.novel.model.NovelAudienceInfo;
import com.jvxi.unity.novel.model.NovelTypeCatalogResponse;
import com.jvxi.unity.novel.model.NovelTypeInfo;

@Component
public class NovelTypeCatalog {
    private static final String DEFAULT_AUDIENCE = "male";
    private static final String DEFAULT_TYPE = "xuanyi";

    private static final List<NovelAudienceInfo> AUDIENCES = List.of(
        new NovelAudienceInfo("male", "男频", "以升级爽感、格局扩张、力量体系与冒险探索为主的创作方向。"),
        new NovelAudienceInfo("female", "女频", "以情感关系、人物成长、情绪张力与人设魅力为主的创作方向。")
    );

    private static final List<NovelTypeInfo> TYPES = List.of(
        maleType("xuanhuan", "玄幻", "世界观宏大，力量体系清晰，强调升级与机缘。", List.of(
            "力量体系前后一致，升级节奏要有反馈。",
            "地图与势力层级逐步展开，避免开局泄露终极设定。"
        )),
        maleType("qihuan", "奇幻", "偏西方魔法或异世界冒险，强调世界异质性与探索。", List.of(
            "异世界规则尽早通过事件呈现，不做设定讲座。",
            "冒险目标明确，每章推进探索或冲突。"
        )),
        maleType("wuxia", "武侠", "江湖恩怨、门派势力、侠义与武功招式。", List.of(
            "武功表现要有画面感，少用抽象威力形容。",
            "江湖规则与人情债要落到具体事件。"
        )),
        maleType("xianxia", "仙侠", "修真问道、宗门势力、法宝灵兽与长生主题。", List.of(
            "修真资源与突破要有代价，避免无成本升级。",
            "宗门/势力冲突通过人物选择体现。"
        )),
        maleType("dushi", "都市", "现代背景下的职场、生活、异能或神豪题材。", List.of(
            "现代场景细节真实，对话符合人物身份。",
            "冲突来自现实压力或社会关系，不靠旁白解释。"
        )),
        maleType("lishi", "历史", "朝代背景、权谋战争、考据与人物命运。", List.of(
            "时代细节服务剧情，避免百科式背景堆砌。",
            "权谋推进靠行动与后果，不写局势总结。"
        )),
        maleType("junshi", "军事", "战争、特种兵、军旅或战略题材。", List.of(
            "战术细节可信，指挥逻辑清晰。",
            "战局变化通过现场感知呈现。"
        )),
        maleType("youxi", "游戏", "网游、电竞或游戏世界穿越。", List.of(
            "系统/数值规则稳定，避免临时改设定。",
            "副本或赛事要有明确目标与代价。"
        )),
        maleType("kehuan", "科幻", "未来科技、星际文明、机甲或末世。", List.of(
            "科技设定服务于冲突，不做技术说明书。",
            "未知威胁通过发现过程逐步揭示。"
        )),
        maleType("xuanyi", "悬疑", "推理、犯罪、灵异或心理惊悚。", List.of(
            "线索公平呈现，误导要可追溯。",
            "氛围靠细节堆叠，不提前剧透真相。"
        )),
        maleType("tiyu", "体育", "竞技项目、训练成长与比赛胜负。", List.of(
            "比赛节奏有起伏，训练成果要可见。",
            "专业细节准确，避免空泛热血口号。"
        )),
        maleType("xianshi", "现实", "贴近生活的现实题材与社会议题。", List.of(
            "人物动机源于现实压力，情绪克制真实。",
            "不写说教式主题总结。"
        )),
        femaleType("yanqing", "言情", "以感情发展为主线的综合言情。", List.of(
            "情感推进靠互动与误会/和解，不靠旁白点评。",
            "人设一致，避免工具化配角。"
        )),
        femaleType("gudai_yanqing", "古代言情", "古代背景下的爱情与礼制冲突。", List.of(
            "礼制/身份限制要影响选择，不做背景板。",
            "情感张力来自处境与价值观碰撞。"
        )),
        femaleType("xiandai_yanqing", "现代言情", "现代都市背景的爱情与事业交织。", List.of(
            "职场/生活细节真实，情感线与生活线互相影响。",
            "对话体现性格差异，避免玛丽苏式旁白。"
        )),
        femaleType("huanxiang_yanqing", "幻想言情", "异世界/修真/神话背景下的言情。", List.of(
            "世界观规则与感情线绑定，设定为关系服务。",
            "冒险与情感节点交替推进。"
        )),
        femaleType("langman_qingchun", "浪漫青春", "校园或青春成长中的初恋与成长。", List.of(
            "青春细节具体，情绪表达自然。",
            "成长通过事件体现，不写青春总结。"
        )),
        femaleType("xianxia_qiyuan", "仙侠奇缘", "女频仙侠情感，宗门/修仙+感情线。", List.of(
            "修真规则稳定，情感与大道选择形成张力。",
            "感情发展要有事件触发，避免空相思。"
        )),
        femaleType("xuanyi_tuili", "悬疑推理", "女频向推理、悬疑与情感结合。", List.of(
            "线索与情感线并行，不牺牲逻辑换悬念。",
            "危险场景写感受，不写剧情走向预告。"
        )),
        femaleType("kehuan_kongjian", "科幻空间", "星际、末世、机甲背景下的女频故事。", List.of(
            "科技/末世规则清晰，人物关系驱动选择。",
            "危机通过行动应对，不做设定讲解。"
        )),
        femaleType("youxi_jingji", "游戏竞技", "电竞、网游背景下的竞技与情感。", List.of(
            "比赛场面有节奏，团队互动推动剧情。",
            "专业术语适度，照顾非读者体验。"
        )),
        femaleType("qingxiaoshuo", "轻小说", "轻松向、二次元感或吐槽向叙事。", List.of(
            "语气轻快但不破第四面墙。",
            "梗与反差服务人物，不喧宾夺主。"
        ))
    );

    public NovelTypeCatalogResponse catalog() {
        return new NovelTypeCatalogResponse(AUDIENCES, TYPES);
    }

    public List<NovelTypeInfo> listTypesForAudience(String audienceChannel) {
        String audience = normalizeAudience(audienceChannel);
        return TYPES.stream()
            .filter(type -> type.audienceChannel().equals(audience))
            .toList();
    }

    public NovelTypeInfo resolveType(String audienceChannel, String novelTypeId) {
        String audience = normalizeAudience(audienceChannel);
        String typeId = normalizeNovelType(audience, novelTypeId);
        return TYPES.stream()
            .filter(type -> type.id().equals(typeId))
            .findFirst()
            .orElseGet(() -> TYPES.stream()
                .filter(type -> type.id().equals(DEFAULT_TYPE))
                .findFirst()
                .orElseThrow());
    }

    public String normalizeAudience(String audienceChannel) {
        String normalized = audienceChannel == null ? "" : audienceChannel.trim().toLowerCase(Locale.ROOT);
        if (Set.of("male", "female").contains(normalized)) {
            return normalized;
        }
        return DEFAULT_AUDIENCE;
    }

    public String normalizeNovelType(String audienceChannel, String novelTypeId) {
        String audience = normalizeAudience(audienceChannel);
        String normalized = novelTypeId == null ? "" : novelTypeId.trim().toLowerCase(Locale.ROOT);
        boolean exists = TYPES.stream()
            .anyMatch(type -> type.audienceChannel().equals(audience) && type.id().equals(normalized));
        if (exists) {
            return normalized;
        }
        return listTypesForAudience(audience).stream()
            .map(NovelTypeInfo::id)
            .findFirst()
            .orElse(DEFAULT_TYPE);
    }

    public String formatGenreLabel(String audienceChannel, String novelTypeId) {
        NovelAudienceInfo audience = AUDIENCES.stream()
            .filter(entry -> entry.id().equals(normalizeAudience(audienceChannel)))
            .findFirst()
            .orElse(AUDIENCES.getFirst());
        NovelTypeInfo type = resolveType(audienceChannel, novelTypeId);
        return audience.label() + " · " + type.label();
    }

    public String inferAudienceFromLegacyGenre(String legacyGenre) {
        String genre = legacyGenre == null ? "" : legacyGenre;
        if (genre.contains("言情") || genre.contains("青春") || genre.contains("女")) {
            return "female";
        }
        return DEFAULT_AUDIENCE;
    }

    public String inferTypeFromLegacyGenre(String legacyGenre, String audienceChannel) {
        String genre = legacyGenre == null ? "" : legacyGenre;
        if (genre.contains("玄幻")) {
            return "xuanhuan";
        }
        if (genre.contains("仙侠")) {
            return audienceChannel.equals("female") ? "xianxia_qiyuan" : "xianxia";
        }
        if (genre.contains("悬疑") || genre.contains("推理")) {
            return audienceChannel.equals("female") ? "xuanyi_tuili" : "xuanyi";
        }
        if (genre.contains("都市")) {
            return "dushi";
        }
        if (genre.contains("科幻")) {
            return audienceChannel.equals("female") ? "kehuan_kongjian" : "kehuan";
        }
        if (genre.contains("言情")) {
            return "yanqing";
        }
        if (genre.contains("武侠")) {
            return "wuxia";
        }
        if (genre.contains("历史")) {
            return "lishi";
        }
        if (genre.contains("游戏")) {
            return audienceChannel.equals("female") ? "youxi_jingji" : "youxi";
        }
        return normalizeNovelType(audienceChannel, "");
    }

    private static NovelTypeInfo maleType(
        String id,
        String label,
        String description,
        List<String> writingHints
    ) {
        return new NovelTypeInfo(id, label, "male", description, writingHints);
    }

    private static NovelTypeInfo femaleType(
        String id,
        String label,
        String description,
        List<String> writingHints
    ) {
        return new NovelTypeInfo(id, label, "female", description, writingHints);
    }
}

