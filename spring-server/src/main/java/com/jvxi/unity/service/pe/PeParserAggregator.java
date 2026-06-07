package com.jvxi.unity.service.pe;

import com.jvxi.unity.model.PeInfo;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

/**
 * PE 解析器聚合器：组合三库结果
 */
@Service
public class PeParserAggregator {

    private final Pecoff4jParser pecoff4jParser;
    private final RawBinaryPeParser rawParser;
    private final JsignPeParser jsignParser;

    public PeParserAggregator(Pecoff4jParser pecoff4jParser,
                              RawBinaryPeParser rawParser,
                              JsignPeParser jsignParser) {
        this.pecoff4jParser = pecoff4jParser;
        this.rawParser = rawParser;
        this.jsignParser = jsignParser;
    }

    public PeInfo parse(byte[] data, String fileName) {
        PeInfo primary = null;
        PeInfo fallback = null;
        List<String> sources = new ArrayList<>();

        // 优先使用 pecoff4j
        try {
            primary = pecoff4jParser.parse(data);
            sources.add("pecoff4j");
        } catch (Exception e) {
            // pecoff4j 失败，用 raw 兜底
        }

        // Raw Binary 解析
        try {
            fallback = rawParser.parse(data);
            sources.add("raw-binary");
        } catch (Exception e) {
            // raw 也失败
        }

        // 合并结果
        PeInfo result = merge(primary, fallback);
        if (result == null) {
            throw new RuntimeException("所有 PE 解析器均失败，文件可能不是有效的 PE 格式");
        }
        result.setFileName(fileName);
        result.setFileSize(data.length);

        // jsign 补充签名信息
        try {
            jsignParser.enrichWithSignature(data, result);
            sources.add("jsign");
        } catch (Exception ignored) {}

        result.setParserSources(sources);
        return result;
    }

    private PeInfo merge(PeInfo primary, PeInfo fallback) {
        if (primary == null && fallback == null) return null;
        if (primary == null) return fallback;
        if (fallback == null) return primary;

        // 用 primary 为主，fallback 补充 null 字段
        if (primary.getMachine() == null) primary.setMachine(fallback.getMachine());
        if (primary.getMagic() == null) primary.setMagic(fallback.getMagic());
        if (primary.getSubsystem() == null) primary.setSubsystem(fallback.getSubsystem());
        if (primary.getImageBase() == 0) primary.setImageBase(fallback.getImageBase());
        if (primary.getSections() == null || primary.getSections().isEmpty())
            primary.setSections(fallback.getSections());
        if (primary.getExports() == null || primary.getExports().isEmpty())
            primary.setExports(fallback.getExports());
        if (primary.getImports() == null || primary.getImports().isEmpty())
            primary.setImports(fallback.getImports());
        if (primary.getDataDirectories() == null || primary.getDataDirectories().isEmpty())
            primary.setDataDirectories(fallback.getDataDirectories());
        if (primary.getDllCharacteristics() == null || primary.getDllCharacteristics().isEmpty())
            primary.setDllCharacteristics(fallback.getDllCharacteristics());
        if (primary.getCharacteristics() == null || primary.getCharacteristics().isEmpty())
            primary.setCharacteristics(fallback.getCharacteristics());
        if (primary.getDebugInfo() == null || primary.getDebugInfo().isEmpty())
            primary.setDebugInfo(fallback.getDebugInfo());
        if (primary.getTlsCallbacks() == null || primary.getTlsCallbacks().isEmpty())
            primary.setTlsCallbacks(fallback.getTlsCallbacks());
        if (primary.getCoffSymbols() == null || primary.getCoffSymbols().isEmpty())
            primary.setCoffSymbols(fallback.getCoffSymbols());
        if (primary.getExportCount() == 0) primary.setExportCount(fallback.getExportCount());
        if (primary.getImportCount() == 0) primary.setImportCount(fallback.getImportCount());

        return primary;
    }
}
