package com.jvxi.unity.service.pe;

import com.jvxi.unity.model.PeInfo;
import org.springframework.stereotype.Service;

/**
 * pecoff4j 解析器（库不可用时降级为 raw 解析器代理）
 */
@Service
public class Pecoff4jParser {

    private final RawBinaryPeParser rawParser;

    public Pecoff4jParser(RawBinaryPeParser rawParser) {
        this.rawParser = rawParser;
    }

    public PeInfo parse(byte[] data) {
        // pecoff4j 不在仓库中，降级使用 RawBinaryPeParser
        return rawParser.parse(data);
    }
}
