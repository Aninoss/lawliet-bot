package modules.graphics;

import modules.GifSequenceWriter;
import org.javacord.api.entity.user.User;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class TriggerGraphics {

    public static InputStream createImageTriggered(User user) throws IOException, ExecutionException, InterruptedException {
        BufferedImage image = user.getAvatar().asBufferedImage().get();
        double scale = 1.5;
        image = Graphics.getScaledImage(image, (int) (image.getWidth() * scale), (int) (image.getHeight() * scale));

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        FileCacheImageOutputStream ios = new FileCacheImageOutputStream(os, new File("temp"));

        GifSequenceWriter gifSequenceWriter = new GifSequenceWriter(ios, image.getType(), 25, true);

        for(int j=0; j<25; j++) {
            int quake = 5;
            Random r = new Random();
            double xPlus = - quake + r.nextDouble() * quake * 2;
            double yPlus = - quake + r.nextDouble() * quake * 2;

            BufferedImage result = new BufferedImage(image.getWidth(), (int) (image.getHeight() + 114 * (image.getWidth() / 600.0)),
                    BufferedImage.TYPE_INT_ARGB);

            Graphics2D g = result.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(new Color(50, 54, 60, 255));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.drawImage(image, 0, 0, null);
            g.drawImage(image, (int) xPlus, (int) yPlus, null);

            double n = 8;
            double size = 1.0 / 5.0;

            for (int i = 1; i <= n; i++) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.4 * (n - i) / n)));
                g.drawImage(image, (int) (xPlus * (1 + size * 2 * (i / n)) - size * image.getWidth() * (i / n)), (int) (yPlus * (1 + size * 2 * (i / n)) - size * image.getHeight() * (i / n)), (int) (image.getWidth() * (1 + size * 2 * (i / n))), (int) (image.getHeight() * (1 + size * 2 * (i / n))), null);
            }

            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) 1));
            g.setColor(new Color(255, 0, 0, 255 / 4));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());

            g.drawImage(ImageIO.read(new File("recourses/triggeredsign.png")), 0, image.getHeight(), image.getWidth(), (int) (114 * (image.getWidth() / 600.0)), null);
            g.drawImage(ImageIO.read(new File("recourses/triggeredsign.png")), (int) -xPlus, (int) -yPlus + image.getHeight(), image.getWidth(), (int) (114 * (image.getWidth() / 600.0)), null);

            gifSequenceWriter.writeToSequence(result);
            g.dispose();
        }

        gifSequenceWriter.close();
        ios.close();
        os.close();

        return new ByteArrayInputStream(os.toByteArray());
    }

}
