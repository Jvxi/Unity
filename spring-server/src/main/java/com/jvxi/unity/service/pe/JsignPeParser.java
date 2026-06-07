package com.jvxi.unity.service.pe;

import com.jvxi.unity.model.PeInfo;
import org.springframework.stereotype.Service;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * jsign 替代解析器：通过原始二进制检测数字签名
 */
@Service
public class JsignPeParser {

    public void enrichWithSignature(byte[] data, PeInfo info) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            int peOffset = buf.getInt(0x3C);
            int optStart = peOffset + 4 + 20;
            int magic = buf.getShort(optStart) & 0xFFFF;
            boolean is64 = (magic == 0x20B);

            // Certificate Table 是第 5 个 Data Directory (index 4)
            int ddBase = is64 ? optStart + 112 : optStart + 96;
            int certRva = buf.getInt(ddBase + 4 * 8);     // RVA (对于 Certificate Table 实际是文件偏移)
            int certSize = buf.getInt(ddBase + 4 * 8 + 4);

            if (certRva > 0 && certSize > 0 && certRva + certSize <= data.length) {
                info.setHasCertificate(true);
                info.setCertificateInfo("Signed (Authenticode), Certificate at file offset 0x"
                    + Integer.toHexString(certRva) + ", size=" + certSize + " bytes");
            } else {
                info.setHasCertificate(false);
            }
        } catch (Exception e) {
            info.setHasCertificate(false);
        }
    }
}
