package modules.graphics;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class AIWatermarkGraphics {

    public static String TEXT = "AI Generated";

    public static void addAIWatermark(File file) throws IOException {
        BufferedImage image = ImageIO.read(file);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.setComposite(AlphaComposite.SrcOver);

        FontMetrics metrics = g2d.getFontMetrics();
        int x = 10;
        int y = metrics.getAscent() + 5;

        g2d.setColor(new Color(0, 0, 0, 128));
        g2d.drawString(TEXT, x + 1, y + 1);

        g2d.setColor(new Color(255, 255, 255, 128));
        g2d.drawString(TEXT, x, y);
        g2d.dispose();

        ImageIO.write(image, "png", file);
    }

}
