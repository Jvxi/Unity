package com.jvxi.unity.service;

import com.jvxi.unity.model.ExportInfo;
import com.jvxi.unity.model.PeInfo;
import com.jvxi.unity.model.SectionInfo;
import com.jvxi.unity.model.WorldAnalysisResult;
import com.jvxi.unity.model.WorldArrayCandidate;
import com.jvxi.unity.model.WorldRelatedData;
import com.jvxi.unity.service.pe.RawBinaryPeParser;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 针对游戏 DLL 的世界数组、对象数组、名称池等关键全局数据做优先启发式分析。
 */
@Service
public class WorldArrayAnalysisService {
    private static final int MAX_CANDIDATES = 40;
    private static final int MAX_RELATED_DATA = 80;
    private static final int MAX_STRINGS_PER_CANDIDATE = 6;
    private static final int MIN_POINTER_RUN = 4;
    private static final int MAX_POINTER_RUN = 512;

    private final RawBinaryPeParser rawParser;
    private final StringExtractService stringExtractService;

    public WorldArrayAnalysisService(RawBinaryPeParser rawParser, StringExtractService stringExtractService) {
        this.rawParser = rawParser;
        this.stringExtractService = stringExtractService;
    }

    public WorldAnalysisResult analyze(byte[] data, PeInfo peInfo) {
        WorldAnalysisResult result = new WorldAnalysisResult();
        if (data == null || peInfo == null || peInfo.getSections() == null) {
            result.setSummary("缺少 PE 结构信息，无法分析世界数组。");
            return result;
        }

        boolean is64 = "PE32+".equals(peInfo.getMagic());
        int ptrSize = is64 ? 8 : 4;
        List<StringHit> stringHits = collectStringHits(data, peInfo);
        List<WorldRelatedData> related = new ArrayList<>();
        Map<Long, WorldArrayCandidate> candidates = new LinkedHashMap<>();

        collectExportEvidence(peInfo, related, candidates);
        collectStringEvidence(peInfo, stringHits, related);
        collectPointerArrayCandidates(data, peInfo, is64, ptrSize, stringHits, candidates);

        List<WorldArrayCandidate> sortedCandidates = candidates.values().stream()
            .sorted(Comparator
                .comparingDouble(WorldArrayCandidate::getConfidence).reversed()
                .thenComparingLong(WorldArrayCandidate::getRvaValue))
            .limit(MAX_CANDIDATES)
            .toList();
        result.setWorldArrayCandidates(sortedCandidates);
        result.setRelatedData(related.stream().limit(MAX_RELATED_DATA).toList());
        result.setPriorityHints(buildPriorityHints(sortedCandidates, related));
        result.setSummary(buildSummary(sortedCandidates, related));
        return result;
    }

    private void collectExportEvidence(
            PeInfo peInfo,
            List<WorldRelatedData> related,
            Map<Long, WorldArrayCandidate> candidates) {
        if (peInfo.getExports() == null) return;

        for (ExportInfo exp : peInfo.getExports()) {
            String name = Optional.ofNullable(exp.getName()).orElse("");
            Keyword keyword = classifyKeyword(name);
            if (keyword == null) continue;

            related.add(relatedData(
                "export_symbol",
                name,
                exp.getRvaValue(),
                peInfo.getImageBase(),
                sectionName(peInfo, exp.getRvaValue()),
                name,
                "导出符号命中 " + keyword.label
            ));

            WorldArrayCandidate candidate = candidateAt(candidates, exp.getRvaValue(), peInfo.getImageBase());
            candidate.setName(name);
            candidate.setKind(keyword.kind);
            candidate.setSectionName(sectionName(peInfo, exp.getRvaValue()));
            candidate.setDetectionMethod(mergeMethod(candidate.getDetectionMethod(), "EXPORT_SYMBOL"));
            candidate.setConfidence(Math.max(candidate.getConfidence(), Math.min(0.98, keyword.weight + 0.18)));
            addEvidence(candidate, "导出符号: " + name);
        }
    }

    private void collectStringEvidence(PeInfo peInfo, List<StringHit> stringHits, List<WorldRelatedData> related) {
        for (StringHit hit : stringHits) {
            Keyword keyword = classifyKeyword(hit.value());
            if (keyword == null) continue;
            related.add(relatedData(
                "string",
                keyword.label,
                hit.rva(),
                peInfo.getImageBase(),
                sectionName(peInfo, hit.rva()),
                hit.value(),
                "字符串命中 " + keyword.label
            ));
        }
    }

    private void collectPointerArrayCandidates(
            byte[] data,
            PeInfo peInfo,
            boolean is64,
            int ptrSize,
            List<StringHit> stringHits,
            Map<Long, WorldArrayCandidate> candidates) {
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        for (SectionInfo section : dataSections(peInfo)) {
            long start = section.getVirtualAddress();
            long end = start + Math.max(section.getVirtualSize(), section.getRawSize());
            int run = 0;
            long runStart = 0;

            for (long rva = start; rva <= end - ptrSize; rva += ptrSize) {
                long fileOff = rawParser.rvaToFileOff(data, rva);
                if (!fits(fileOff, ptrSize, data.length)) {
                    addPointerRunCandidate(peInfo, stringHits, candidates, runStart, run, section);
                    run = 0;
                    continue;
                }

                long pointer = readPointer(buf, (int) fileOff, is64);
                long pointedRva = pointerToRva(pointer, peInfo.getImageBase());
                if (pointsToUsefulRuntimeData(peInfo, pointedRva)) {
                    if (run == 0) runStart = rva;
                    run++;
                    if (run >= MAX_POINTER_RUN) {
                        addPointerRunCandidate(peInfo, stringHits, candidates, runStart, run, section);
                        run = 0;
                    }
                } else {
                    addPointerRunCandidate(peInfo, stringHits, candidates, runStart, run, section);
                    run = 0;
                }
            }
            addPointerRunCandidate(peInfo, stringHits, candidates, runStart, run, section);
        }
    }

    private void addPointerRunCandidate(
            PeInfo peInfo,
            List<StringHit> stringHits,
            Map<Long, WorldArrayCandidate> candidates,
            long runStart,
            int run,
            SectionInfo section) {
        if (run < MIN_POINTER_RUN) return;

        List<StringHit> nearby = nearbyStrings(stringHits, runStart, 0x600);
        double keywordScore = nearby.stream()
            .map(StringHit::value)
            .map(this::classifyKeyword)
            .filter(item -> item != null)
            .mapToDouble(item -> item.weight)
            .max()
            .orElse(0.0);

        double confidence = Math.min(0.95, 0.42 + Math.min(run, 64) * 0.006 + keywordScore * 0.35);
        if (confidence < 0.48) return;

        WorldArrayCandidate candidate = candidateAt(candidates, runStart, peInfo.getImageBase());
        candidate.setName(candidate.getName() != null ? candidate.getName() : inferCandidateName(nearby, "PointerArray"));
        candidate.setKind(candidate.getKind() != null ? candidate.getKind() : inferKind(nearby, "pointer_array"));
        candidate.setSectionName(section.getName());
        candidate.setDetectionMethod(mergeMethod(candidate.getDetectionMethod(), "POINTER_ARRAY_SCAN"));
        candidate.setPointerCount(Math.max(candidate.getPointerCount(), run));
        candidate.setConfidence(Math.max(candidate.getConfidence(), confidence));
        addEvidence(candidate, "数据段连续指针数组: " + run + " 项");
        for (StringHit hit : nearby.stream().limit(MAX_STRINGS_PER_CANDIDATE).toList()) {
            addRelatedString(candidate, hit.value());
        }
    }

    private List<StringHit> collectStringHits(byte[] data, PeInfo peInfo) {
        List<StringHit> hits = new ArrayList<>();
        addStringHits(hits, stringExtractService.extract(data, 4, "ascii", null), peInfo);
        addStringHits(hits, stringExtractService.extract(data, 4, "unicode", null), peInfo);
        hits.sort(Comparator.comparingLong(StringHit::rva));
        return hits;
    }

    private void addStringHits(List<StringHit> hits, StringExtractService.ExtractResult result, PeInfo peInfo) {
        if (result == null || result.getStrings() == null) return;
        for (StringExtractService.StringEntry entry : result.getStrings()) {
            long rva = fileOffsetToRva(peInfo, entry.getOffset());
            hits.add(new StringHit(entry.getOffset(), rva, entry.getValue(), entry.getEncoding()));
        }
    }

    private long fileOffsetToRva(PeInfo peInfo, long fileOffset) {
        if (peInfo == null || peInfo.getSections() == null) return fileOffset;
        for (SectionInfo section : peInfo.getSections()) {
            long rawSize = section.getRawSize();
            if (rawSize <= 0) continue;
            long rawStart = section.getRawPointer();
            long rawEnd = rawStart + rawSize;
            if (fileOffset >= rawStart && fileOffset < rawEnd) {
                return section.getVirtualAddress() + (fileOffset - rawStart);
            }
        }
        return fileOffset;
    }

    private List<StringHit> nearbyStrings(List<StringHit> hits, long rva, long radius) {
        long min = Math.max(0, rva - radius);
        long max = rva + radius;
        return hits.stream()
            .filter(hit -> hit.rva() >= min && hit.rva() <= max)
            .filter(hit -> classifyKeyword(hit.value()) != null)
            .limit(16)
            .toList();
    }

    private Keyword classifyKeyword(String value) {
        if (value == null || value.isBlank()) return null;
        String text = value.toLowerCase(Locale.ROOT);
        if (text.contains("gworld") || text.equals("world") || text.contains("uworld") || text.contains("persistentlevel")) {
            return new Keyword("world_array", "世界/World", 0.92);
        }
        if (text.contains("worldcontext") || text.contains("world_context") || text.contains("world context")) {
            return new Keyword("world_context", "WorldContext", 0.88);
        }
        if (text.contains("guobjectarray") || text.contains("gobjects") || text.contains("uobjectarray")
            || text.contains("globalobject") || text.contains("objectarray")) {
            return new Keyword("object_array", "对象数组", 0.86);
        }
        if (text.contains("gname") || text.contains("gnames") || text.contains("fnamepool")
            || text.contains("namepool") || text.contains("nametable")) {
            return new Keyword("name_pool", "名称池", 0.82);
        }
        if (text.contains("ulevel") || text.contains("levelarray") || text.contains("actorarray") || text.contains("actors")) {
            return new Keyword("level_actor_array", "关卡/Actor 数组", 0.76);
        }
        if (text.contains("gameinstance") || text.contains("engine") || text.contains("viewportclient")) {
            return new Keyword("engine_runtime", "引擎运行时对象", 0.66);
        }
        if (text.contains("unityengine") || text.contains("gameobject") || text.contains("transform") || text.contains("scene")) {
            return new Keyword("unity_runtime", "Unity 运行时数据", 0.62);
        }
        return null;
    }

    private List<SectionInfo> dataSections(PeInfo peInfo) {
        List<SectionInfo> sections = new ArrayList<>();
        for (SectionInfo section : peInfo.getSections()) {
            String name = Optional.ofNullable(section.getName()).orElse("").toLowerCase(Locale.ROOT);
            String chars = Optional.ofNullable(section.getCharacteristics()).orElse("");
            boolean readable = chars.contains("READ");
            boolean executable = chars.contains("EXECUTE");
            boolean likelyData = name.contains("data") || name.contains("rdata") || name.contains("bss") || name.contains("const");
            if (readable && !executable && likelyData) {
                sections.add(section);
            }
        }
        return sections;
    }

    private boolean pointsToUsefulRuntimeData(PeInfo peInfo, long rva) {
        if (rva <= 0) return false;
        for (SectionInfo section : peInfo.getSections()) {
            String chars = Optional.ofNullable(section.getCharacteristics()).orElse("");
            String name = Optional.ofNullable(section.getName()).orElse("").toLowerCase(Locale.ROOT);
            if (!chars.contains("READ") || name.contains("reloc") || name.contains("rsrc")) continue;
            long start = section.getVirtualAddress();
            long end = start + Math.max(section.getVirtualSize(), section.getRawSize());
            if (rva >= start && rva < end) return true;
        }
        return false;
    }

    private WorldArrayCandidate candidateAt(Map<Long, WorldArrayCandidate> candidates, long rva, long imageBase) {
        return candidates.computeIfAbsent(rva, key -> {
            WorldArrayCandidate candidate = new WorldArrayCandidate();
            candidate.setRvaValue(key);
            candidate.setRva(hex(key));
            candidate.setVaValue(imageBase + key);
            candidate.setVa(hex(candidate.getVaValue()));
            return candidate;
        });
    }

    private WorldRelatedData relatedData(
            String kind,
            String name,
            long rva,
            long imageBase,
            String sectionName,
            String value,
            String note) {
        WorldRelatedData data = new WorldRelatedData();
        data.setKind(kind);
        data.setName(name);
        data.setRvaValue(rva);
        data.setRva(hex(rva));
        data.setVaValue(imageBase + rva);
        data.setVa(hex(data.getVaValue()));
        data.setSectionName(sectionName);
        data.setValue(value);
        data.setNote(note);
        return data;
    }

    private List<String> buildPriorityHints(List<WorldArrayCandidate> candidates, List<WorldRelatedData> related) {
        List<String> hints = new ArrayList<>();
        if (!candidates.isEmpty()) {
            WorldArrayCandidate top = candidates.get(0);
            hints.add("优先检查 " + safe(top.getName(), top.getKind()) + " @ " + top.getRva()
                + "，置信度 " + String.format(Locale.ROOT, "%.2f", top.getConfidence()));
        }
        boolean hasWorld = candidates.stream().anyMatch(item -> "world_array".equals(item.getKind()) || "world_context".equals(item.getKind()));
        boolean hasObject = candidates.stream().anyMatch(item -> "object_array".equals(item.getKind()));
        boolean hasName = candidates.stream().anyMatch(item -> "name_pool".equals(item.getKind()));
        if (hasWorld) hints.add("世界数组/WorldContext 命中，应优先结合关卡、Actor、GameInstance 相关字符串验证。");
        if (hasObject) hints.add("对象数组候选存在，可与 UObject/类名字符串和虚表候选交叉验证。");
        if (hasName) hints.add("名称池候选存在，可用于辅助恢复对象名、类名和符号语义。");
        if (hints.isEmpty() && !related.isEmpty()) hints.add("存在世界/对象/名称池相关字符串，但未形成高置信指针数组。");
        if (hints.isEmpty()) hints.add("未发现明显世界数组证据，可依赖虚表、导出和字符串继续人工分析。");
        return hints;
    }

    private String buildSummary(List<WorldArrayCandidate> candidates, List<WorldRelatedData> related) {
        if (candidates.isEmpty()) {
            return related.isEmpty()
                ? "未发现明显世界数组、对象数组或名称池证据。"
                : "发现相关字符串/符号证据，但暂未定位到稳定指针数组候选。";
        }
        long high = candidates.stream().filter(item -> item.getConfidence() >= 0.75).count();
        return "发现 " + candidates.size() + " 个世界/对象/名称池相关候选，其中高置信候选 " + high + " 个。";
    }

    private String sectionName(PeInfo peInfo, long rva) {
        if (peInfo.getSections() == null) return "";
        for (SectionInfo section : peInfo.getSections()) {
            long start = section.getVirtualAddress();
            long end = start + Math.max(section.getVirtualSize(), section.getRawSize());
            if (rva >= start && rva < end) return section.getName();
        }
        return "";
    }

    private String inferCandidateName(List<StringHit> nearby, String fallback) {
        return nearby.stream()
            .map(StringHit::value)
            .filter(value -> classifyKeyword(value) != null)
            .findFirst()
            .orElse(fallback);
    }

    private String inferKind(List<StringHit> nearby, String fallback) {
        return nearby.stream()
            .map(StringHit::value)
            .map(this::classifyKeyword)
            .filter(item -> item != null)
            .map(item -> item.kind)
            .findFirst()
            .orElse(fallback);
    }

    private String mergeMethod(String existing, String next) {
        if (existing == null || existing.isBlank()) return next;
        return existing.contains(next) ? existing : existing + "+" + next;
    }

    private void addEvidence(WorldArrayCandidate candidate, String evidence) {
        if (candidate.getEvidence() == null) candidate.setEvidence(new ArrayList<>());
        if (!candidate.getEvidence().contains(evidence)) candidate.getEvidence().add(evidence);
    }

    private void addRelatedString(WorldArrayCandidate candidate, String value) {
        if (value == null || value.isBlank()) return;
        if (candidate.getRelatedStrings() == null) candidate.setRelatedStrings(new ArrayList<>());
        if (!candidate.getRelatedStrings().contains(value)) candidate.getRelatedStrings().add(value);
    }

    private long readPointer(ByteBuffer buf, int off, boolean is64) {
        return is64 ? buf.getLong(off) : (buf.getInt(off) & 0xFFFFFFFFL);
    }

    private long pointerToRva(long value, long imageBase) {
        return value >= imageBase ? value - imageBase : value;
    }

    private boolean fits(long off, long size, int length) {
        return off >= 0 && size >= 0 && off <= Integer.MAX_VALUE && off + size <= length;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String hex(long value) {
        return "0x" + Long.toHexString(value);
    }

    private record StringHit(long offset, long rva, String value, String encoding) {
    }

    private record Keyword(String kind, String label, double weight) {
    }
}
