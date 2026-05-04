package com.gamilha.utils;

import com.github.sarxos.webcam.Webcam;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CameraUtils {

    /**
     * Captures a single photo from the default webcam and returns it as a byte array (JPEG).
     * @return JPEG bytes or null if capture fails.
     */
    public static byte[] capturePhoto() {
        Webcam webcam = Webcam.getDefault();
        if (webcam != null) {
            try {
                if (!webcam.isOpen()) {
                    webcam.open();
                }
                BufferedImage image = webcam.getImage();
                if (image != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "JPG", baos);
                    return baos.toByteArray();
                }
            } catch (IOException e) {
                System.err.println("Error capturing photo: " + e.getMessage());
            } finally {
                if (webcam.isOpen()) {
                    webcam.close();
                }
            }
        } else {
            System.err.println("No webcam detected.");
        }
        return null;
    }
}
