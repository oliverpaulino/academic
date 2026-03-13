package com.events.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class QRService {

    private static final int QR_SIZE = 300;

    public static byte[] generateQR(Long eventId, Long userId, String token) {
        String content = String.format("EVENT:%d|USER:%d|TOKEN:%s", eventId, userId, token);
        return generateQRFromString(content);
    }

    public static byte[] generateQRFromString(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H,
                    EncodeHintType.MARGIN, 1,
                    EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return baos.toByteArray();
        } catch (WriterException | IOException e) {
            throw new RuntimeException("QR generation failed", e);
        }
    }

    public static String extractToken(String qrContent) {
        if (qrContent == null)
            return null;
        for (String part : qrContent.split("\\|")) {
            if (part.startsWith("TOKEN:"))
                return part.substring(6);
        }
        return null;
    }
}
