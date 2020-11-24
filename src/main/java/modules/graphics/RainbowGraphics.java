package modules.graphics;

import org.javacord.api.entity.user.User;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.ExecutionException;

public class RainbowGraphics {

    public static InputStream createImageRainbow(User user, long opacity) throws ExecutionException, InterruptedException, IOException {
        BufferedImage image = user.getAvatar().asBufferedImage().get();
        double scale = 1.5;
        image = Graphics.getScaledImage(image, (int) (image.getWidth() * scale), (int) (image.getHeight() * scale));
        BufferedImage rainbow = ImageIO.read(new File("recourses/rainbow.png"));
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, null);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (opacity / 100.0)));
        g.drawImage(rainbow, 0, 0, image.getWidth(), image.getHeight(), null);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(result, "png", os);
        return new ByteArrayInputStream(os.toByteArray());
    }

}
