package com.gamilha.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.Map;

public final class QrCodeUtil {

    private QrCodeUtil() {
    }

    public static WritableImage generate(String content, int size) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    size,
                    size,
                    Map.of(EncodeHintType.MARGIN, 1)
            );

            WritableImage image = new WritableImage(matrix.getWidth(), matrix.getHeight());
            PixelWriter writer = image.getPixelWriter();

            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    writer.setColor(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            return image;
        } catch (WriterException e) {
            throw new RuntimeException("Impossible de generer le code QR.", e);
        }
    }
}
