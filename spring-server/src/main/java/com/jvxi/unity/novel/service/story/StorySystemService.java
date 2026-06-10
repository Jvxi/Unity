package com.jvxi.unity.novel.service.story;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvxi.unity.novel.model.story.*;
import com.jvxi.unity.novel.persistence.entity.StoryMasterSettingEntity;
import com.jvxi.unity.novel.persistence.entity.StoryVolumeBriefEntity;
import com.jvxi.unity.novel.persistence.entity.StoryChapterBriefEntity;
import com.jvxi.unity.novel.persistence.entity.StoryReviewContractEntity;
import com.jvxi.unity.novel.persistence.repository.StoryMasterSettingRepository;
import com.jvxi.unity.novel.persistence.repository.StoryVolumeBriefRepository;
import com.jvxi.unity.novel.persistence.repository.StoryChapterBriefRepository;
import com.jvxi.unity.novel.persistence.repository.StoryReviewContractRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StorySystemService {

    private static final Logger log = LoggerFactory.getLogger(StorySystemService.class);

    private final StoryMasterSettingRepository masterSettingRepository;
    private final StoryVolumeBriefRepository volumeBriefRepository;
    private final StoryChapterBriefRepository chapterBriefRepository;
    private final StoryReviewContractRepository reviewContractRepository;
    private final ObjectMapper objectMapper;

    // 题材路由数据缓存
    private List<Map<String, String>> routeRows;
    private Map<String, Map<String, String>> reasoningCache;

    public StorySystemService(
            StoryMasterSettingRepository masterSettingRepository,
            StoryVolumeBriefRepository volumeBriefRepository,
            StoryChapterBriefRepository chapterBriefRepository,
            StoryReviewContractRepository reviewContractRepository,
            ObjectMapper objectMapper
    ) {
        this.masterSettingRepository = masterSettingRepository;
        this.volumeBriefRepository = volumeBriefRepository;
        this.chapterBriefRepository = chapterBriefRepository;
        this.reviewContractRepository = reviewContractRepository;
        this.objectMapper = objectMapper;
        this.reasoningCache = new HashMap<>();
    }

    /**
     * 题材路由：根据书名、题材、关键词匹配CSV规则
     */
    public Map<String, Object> routeGenre(String bookId, String query, String genre) {
        List<Map<String, String>> rows = loadRouteRows();
        String queryText = normalizeText(query + " " + (genre != null ? genre : ""));

        // 1. 尝试关键词匹配
        for (Map<String, String> row : rows) {
            List<String> aliases = splitMultiValue(row.get("关键词"));
            aliases.addAll(splitMultiValue(row.get("意图与同义词")));
            aliases.addAll(splitMultiValue(row.get("题材别名")));

            boolean matched = aliases.stream()
                    .anyMatch(alias -> alias != null && !alias.isEmpty() && queryText.contains(normalizeText(alias)));

            if (matched) {
                return buildRouteResult(row, "keyword_or_alias_match", genre);
            }
        }

        // 2. 尝试显式题材匹配
        if (genre != null && !genre.isEmpty()) {
            for (Map<String, String> row : rows) {
                List<String> genreValues = splitMultiValue(row.get("适用题材"));
                genreValues.addAll(splitMultiValue(row.get("题材/流派")));

                boolean matched = genreValues.stream()
                        .anyMatch(g -> normalizeText(g).equals(normalizeText(genre)));

                if (matched) {
                    return buildRouteResult(row, "explicit_genre_fallback", genre);
                }
            }
        }

        // 3. 默认路由（玄幻）
        log.warn("无法匹配题材路由，使用默认玄幻题材: query={}, genre={}", query, genre);
        Map<String, String> defaultRow = new HashMap<>();
        defaultRow.put("题材/流派", "玄幻");
        defaultRow.put("核心调性", "热血、爽快、成长");
        defaultRow.put("节奏策略", "快节奏、高潮迭起");
        defaultRow.put("推荐基础检索表", "桥段套路|爽点与节奏");
        defaultRow.put("推荐动态检索表", "写作技法|场景写法");
        return buildRouteResult(defaultRow, "default_fallback", genre);
    }

    /**
     * 生成主设定合同
     */
    @SuppressWarnings("unchecked")
    @Transactional
    public MasterSetting generateMasterSetting(String bookId, String query, String genre) {
        // 检查是否已存在
        Optional<StoryMasterSettingEntity> existing = masterSettingRepository.findByBookId(bookId);
        if (existing.isPresent()) {
            return deserializeMasterSetting(existing.get());
        }

        // 执行题材路由
        Map<String, Object> route = routeGenre(bookId, query, genre);

        // 构建主设定合同
        Map<String, Object> routeMeta = (Map<String, Object>) route.get("meta");
        MasterSetting masterSetting = new MasterSetting(
                new ContractMeta("master_setting"),
                routeMeta,
                Map.of(
                        "core_tone", route.get("core_tone"),
                        "pacing_strategy", route.get("pacing_strategy")
                ),
                (List<Map<String, Object>>) route.getOrDefault("base_context", List.of()),
                (List<Map<String, Object>>) route.getOrDefault("source_trace", List.of()),
                Map.of(
                        "locked", List.of("route.primary_genre", "master_constraints.core_tone"),
                        "append_only", List.of("anti_patterns"),
                        "override_allowed", List.of()
                )
        );

        // 持久化
        StoryMasterSettingEntity entity = new StoryMasterSettingEntity(bookId);
        entity.setRouteJson(serialize(routeMeta));
        entity.setMasterConstraintsJson(serialize(masterSetting.masterConstraints()));
        entity.setBaseContextJson(serialize(masterSetting.baseContext()));
        entity.setOverridePolicyJson(serialize(masterSetting.overridePolicy()));
        entity.setSourceTraceJson(serialize(masterSetting.sourceTrace()));
        masterSettingRepository.save(entity);

        log.info("生成主设定合同: bookId={}, genre={}", bookId, genre);
        return masterSetting;
    }

    /**
     * 获取主设定合同
     */
    public Optional<MasterSetting> getMasterSetting(String bookId) {
        return masterSettingRepository.findByBookId(bookId)
                .map(this::deserializeMasterSetting);
    }

    /**
     * 生成卷级合同
     */
    @Transactional
    public VolumeBrief generateVolumeBrief(String bookId, int volumeNumber) {
        // 检查是否已存在
        Optional<StoryVolumeBriefEntity> existing = volumeBriefRepository.findByBookIdAndVolumeNumber(bookId, volumeNumber);
        if (existing.isPresent()) {
            return deserializeVolumeBrief(existing.get());
        }

        // 获取主设定合同
        MasterSetting masterSetting = getMasterSetting(bookId)
                .orElseThrow(() -> new IllegalStateException("主设定合同不存在，请先生成主设定合同"));

        // 构建卷级合同
        VolumeBrief volumeBrief = new VolumeBrief(
                new ContractMeta("volume_brief"),
                Map.of("volume_number", volumeNumber, "master_setting_ref", masterSetting.route()),
                List.of(), // selectedTropes - 需要从CSV检索
                Map.of(), // selectedPacing
                List.of(), // selectedScenes
                List.of(), // antiPatterns
                List.of(), // systemConstraints
                new OverrideBundle()
        );

        // 持久化
        StoryVolumeBriefEntity entity = new StoryVolumeBriefEntity(bookId, volumeNumber);
        entity.setVolumeGoalJson(serialize(volumeBrief.volumeGoal()));
        entity.setSelectedTropesJson(serialize(volumeBrief.selectedTropes()));
        entity.setSelectedPacingJson(serialize(volumeBrief.selectedPacing()));
        entity.setSelectedScenesJson(serialize(volumeBrief.selectedScenes()));
        entity.setAntiPatternsJson(serialize(volumeBrief.antiPatterns()));
        entity.setSystemConstraintsJson(serialize(volumeBrief.systemConstraints()));
        entity.setOverridesJson(serialize(volumeBrief.overrides()));
        volumeBriefRepository.save(entity);

        log.info("生成卷级合同: bookId={}, volume={}", bookId, volumeNumber);
        return volumeBrief;
    }

    /**
     * 获取卷级合同
     */
    public Optional<VolumeBrief> getVolumeBrief(String bookId, int volumeNumber) {
        return volumeBriefRepository.findByBookIdAndVolumeNumber(bookId, volumeNumber)
                .map(this::deserializeVolumeBrief);
    }

    /**
     * 生成章节级合同
     */
    @Transactional
    public ChapterBrief generateChapterBrief(String bookId, int chapterNumber, Map<String, Object> chapterDirective) {
        // 检查是否已存在
        Optional<StoryChapterBriefEntity> existing = chapterBriefRepository.findByBookIdAndChapterNumber(bookId, chapterNumber);
        if (existing.isPresent()) {
            return deserializeChapterBrief(existing.get());
        }

        // 获取主设定合同
        MasterSetting masterSetting = getMasterSetting(bookId)
                .orElseThrow(() -> new IllegalStateException("主设定合同不存在，请先生成主设定合同"));

        // 构建章节级合同
        ChapterBrief chapterBrief = new ChapterBrief(
                new ContractMeta("chapter_brief"),
                Map.of("chapter_focus", suggestChapterFocus(chapterDirective)),
                chapterDirective != null ? chapterDirective : Map.of(),
                List.of(), // dynamicContext - 需要从CSV检索
                List.of(), // sourceTrace
                Map.of() // reasoning
        );

        // 持久化
        StoryChapterBriefEntity entity = new StoryChapterBriefEntity(bookId, chapterNumber);
        entity.setChapterDirectiveJson(serialize(chapterBrief.chapterDirective()));
        entity.setDynamicContextJson(serialize(chapterBrief.dynamicContext()));
        entity.setReasoningJson(serialize(chapterBrief.reasoning()));
        entity.setOverrideAllowedJson(serialize(chapterBrief.overrideAllowed()));
        entity.setSourceTraceJson(serialize(chapterBrief.sourceTrace()));
        chapterBriefRepository.save(entity);

        log.info("生成章节级合同: bookId={}, chapter={}", bookId, chapterNumber);
        return chapterBrief;
    }

    /**
     * 获取章节级合同
     */
    public Optional<ChapterBrief> getChapterBrief(String bookId, int chapterNumber) {
        return chapterBriefRepository.findByBookIdAndChapterNumber(bookId, chapterNumber)
                .map(this::deserializeChapterBrief);
    }

    /**
     * 生成审查合同
     */
    @Transactional
    public ReviewContract generateReviewContract(String bookId, int chapterNumber) {
        // 检查是否已存在
        Optional<StoryReviewContractEntity> existing = reviewContractRepository.findByBookIdAndChapterNumber(bookId, chapterNumber);
        if (existing.isPresent()) {
            return deserializeReviewContract(existing.get());
        }

        // 获取主设定合同
        MasterSetting masterSetting = getMasterSetting(bookId)
                .orElseThrow(() -> new IllegalStateException("主设定合同不存在，请先生成主设定合同"));

        // 构建审查合同
        ReviewContract reviewContract = new ReviewContract(
                new ContractMeta("review_contract"),
                List.of("设定一致性", "时间线连续性", "角色行为逻辑"), // mustCheck
                List.of("大纲锚点覆盖", "禁写内容检查"), // blockingRules
                List.of(), // genreSpecificRisks - 需要从CSV检索
                List.of(), // antiPatterns
                List.of(), // systemConstraints
                Map.of("completeness_threshold", 0.8, "consistency_threshold", 0.9), // reviewThresholds
                new OverrideBundle()
        );

        // 持久化
        StoryReviewContractEntity entity = new StoryReviewContractEntity(bookId, chapterNumber);
        entity.setMustCheckJson(serialize(reviewContract.mustCheck()));
        entity.setBlockingRulesJson(serialize(reviewContract.blockingRules()));
        entity.setGenreSpecificRisksJson(serialize(reviewContract.genreSpecificRisks()));
        entity.setAntiPatternsJson(serialize(reviewContract.antiPatterns()));
        entity.setReviewThresholdsJson(serialize(reviewContract.reviewThresholds()));
        entity.setOverridesJson(serialize(reviewContract.overrides()));
        reviewContractRepository.save(entity);

        log.info("生成审查合同: bookId={}, chapter={}", bookId, chapterNumber);
        return reviewContract;
    }

    /**
     * 获取审查合同
     */
    public Optional<ReviewContract> getReviewContract(String bookId, int chapterNumber) {
        return reviewContractRepository.findByBookIdAndChapterNumber(bookId, chapterNumber)
                .map(this::deserializeReviewContract);
    }

    // ============ 私有辅助方法 ============

    private List<Map<String, String>> loadRouteRows() {
        if (routeRows != null) {
            return routeRows;
        }

        // 从classpath加载CSV
        try (InputStream is = getClass().getResourceAsStream("/references/csv/题材与调性推理.csv")) {
            if (is == null) {
                log.warn("未找到题材与调性推理.csv，使用默认路由数据");
                routeRows = List.of();
                return routeRows;
            }

            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            routeRows = parseCsv(content);
        } catch (IOException e) {
            log.error("加载题材路由CSV失败", e);
            routeRows = List.of();
        }

        return routeRows;
    }

    private List<Map<String, String>> parseCsv(String content) {
        List<Map<String, String>> rows = new ArrayList<>();
        String[] lines = content.split("\n");
        if (lines.length < 2) {
            return rows;
        }

        String[] headers = lines[0].split(",");
        for (int i = 1; i < lines.length; i++) {
            String[] values = lines[i].split(",");
            Map<String, String> row = new HashMap<>();
            for (int j = 0; j < Math.min(headers.length, values.length); j++) {
                row.put(headers[j].trim(), values[j].trim());
            }
            rows.add(row);
        }

        return rows;
    }

    private Map<String, Object> buildRouteResult(Map<String, String> row, String routeSource, String genre) {
        String primaryGenre = row.getOrDefault("题材/流派", genre != null ? genre : "");
        String canonicalGenre = row.getOrDefault("canonical_genre", primaryGenre);

        return Map.of(
                "meta", Map.of(
                        "primary_genre", primaryGenre,
                        "canonical_genre", canonicalGenre,
                        "route_source", routeSource,
                        "genre_filter", canonicalGenre
                ),
                "core_tone", row.getOrDefault("核心调性", ""),
                "pacing_strategy", row.getOrDefault("节奏策略", ""),
                "default_query", row.getOrDefault("默认查询词", "")
        );
    }

    private String suggestChapterFocus(Map<String, Object> chapterDirective) {
        if (chapterDirective == null) {
            return "";
        }
        String goal = (String) chapterDirective.getOrDefault("goal", "");
        return goal != null ? goal : "";
    }

    private String normalizeText(String text) {
        return text != null ? text.trim().toLowerCase() : "";
    }

    private List<String> splitMultiValue(String raw) {
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(raw.split("[|；;]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
            return "{}";
        }
    }

    private MasterSetting deserializeMasterSetting(StoryMasterSettingEntity entity) {
        try {
            Map<String, Object> route = objectMapper.readValue(entity.getRouteJson(), new TypeReference<>() {});
            Map<String, Object> constraints = objectMapper.readValue(entity.getMasterConstraintsJson(), new TypeReference<>() {});
            List<Map<String, Object>> baseContext = objectMapper.readValue(entity.getBaseContextJson(), new TypeReference<>() {});
            Map<String, List<String>> overridePolicy = objectMapper.readValue(entity.getOverridePolicyJson(), new TypeReference<>() {});
            List<Map<String, Object>> sourceTrace = objectMapper.readValue(entity.getSourceTraceJson(), new TypeReference<>() {});

            return new MasterSetting(
                    new ContractMeta("master_setting"),
                    route,
                    constraints,
                    baseContext,
                    sourceTrace,
                    overridePolicy
            );
        } catch (IOException e) {
            log.error("反序列化MasterSetting失败", e);
            return new MasterSetting("unknown");
        }
    }

    private VolumeBrief deserializeVolumeBrief(StoryVolumeBriefEntity entity) {
        try {
            Map<String, Object> volumeGoal = objectMapper.readValue(entity.getVolumeGoalJson(), new TypeReference<>() {});
            List<String> selectedTropes = objectMapper.readValue(entity.getSelectedTropesJson(), new TypeReference<>() {});
            Map<String, Object> selectedPacing = objectMapper.readValue(entity.getSelectedPacingJson(), new TypeReference<>() {});
            List<String> selectedScenes = objectMapper.readValue(entity.getSelectedScenesJson(), new TypeReference<>() {});
            List<String> antiPatterns = objectMapper.readValue(entity.getAntiPatternsJson(), new TypeReference<>() {});
            List<String> systemConstraints = objectMapper.readValue(entity.getSystemConstraintsJson(), new TypeReference<>() {});
            OverrideBundle overrides = objectMapper.readValue(entity.getOverridesJson(), OverrideBundle.class);

            return new VolumeBrief(
                    new ContractMeta("volume_brief"),
                    volumeGoal,
                    selectedTropes,
                    selectedPacing,
                    selectedScenes,
                    antiPatterns,
                    systemConstraints,
                    overrides
            );
        } catch (IOException e) {
            log.error("反序列化VolumeBrief失败", e);
            return new VolumeBrief(entity.getVolumeNumber());
        }
    }

    private ChapterBrief deserializeChapterBrief(StoryChapterBriefEntity entity) {
        try {
            Map<String, Object> chapterDirective = objectMapper.readValue(entity.getChapterDirectiveJson(), new TypeReference<>() {});
            List<Map<String, Object>> dynamicContext = objectMapper.readValue(entity.getDynamicContextJson(), new TypeReference<>() {});
            Map<String, Object> reasoning = objectMapper.readValue(entity.getReasoningJson(), new TypeReference<>() {});
            Map<String, Object> overrideAllowed = objectMapper.readValue(entity.getOverrideAllowedJson(), new TypeReference<>() {});
            List<Map<String, Object>> sourceTrace = objectMapper.readValue(entity.getSourceTraceJson(), new TypeReference<>() {});

            return new ChapterBrief(
                    new ContractMeta("chapter_brief"),
                    overrideAllowed,
                    chapterDirective,
                    dynamicContext,
                    sourceTrace,
                    reasoning
            );
        } catch (IOException e) {
            log.error("反序列化ChapterBrief失败", e);
            return new ChapterBrief(entity.getChapterNumber());
        }
    }

    private ReviewContract deserializeReviewContract(StoryReviewContractEntity entity) {
        try {
            List<String> mustCheck = objectMapper.readValue(entity.getMustCheckJson(), new TypeReference<>() {});
            List<String> blockingRules = objectMapper.readValue(entity.getBlockingRulesJson(), new TypeReference<>() {});
            List<String> genreSpecificRisks = objectMapper.readValue(entity.getGenreSpecificRisksJson(), new TypeReference<>() {});
            List<String> antiPatterns = objectMapper.readValue(entity.getAntiPatternsJson(), new TypeReference<>() {});
            Map<String, Object> reviewThresholds = objectMapper.readValue(entity.getReviewThresholdsJson(), new TypeReference<>() {});
            OverrideBundle overrides = objectMapper.readValue(entity.getOverridesJson(), OverrideBundle.class);

            return new ReviewContract(
                    new ContractMeta("review_contract"),
                    mustCheck,
                    blockingRules,
                    genreSpecificRisks,
                    antiPatterns,
                    List.of(), // systemConstraints
                    reviewThresholds,
                    overrides
            );
        } catch (IOException e) {
            log.error("反序列化ReviewContract失败", e);
            return new ReviewContract(entity.getChapterNumber());
        }
    }
}

