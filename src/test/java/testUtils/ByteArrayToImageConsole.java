package testUtils;

import io.quarkus.logging.Log;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;

public class ByteArrayToImageConsole {
    public static void renderImage(byte[] imageBytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (Objects.isNull(image)) {
                throw new IOException("Unable to decode image from byte array");
            }

            JFrame frame = new JFrame("Image Viewer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(image.getWidth(), image.getHeight());

            frame.add(new JLabel(new ImageIcon(image)));
            frame.pack();
            frame.setVisible(true);
        } catch (IOException e) {
            Log.errorf("Failed to render image: %s", e.getMessage());
        }
    }
}
