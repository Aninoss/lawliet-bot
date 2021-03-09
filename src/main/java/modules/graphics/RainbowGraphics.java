package modules.graphics;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.imageio.ImageIO;
import core.ResourceHandler;
import net.dv8tion.jda.api.entities.User;

public class RainbowGraphics {

    public static InputStream createImageRainbow(User user, long opacity) throws IOException {
        BufferedImage image = ImageIO.read(new URL(user.getEffectiveAvatarUrl()));
        double scale = 1.5;
        image = GraphicsUtil.getScaledImage(image, (int) (image.getWidth() * scale), (int) (image.getHeight() * scale));
        BufferedImage rainbow = ImageIO.read(ResourceHandler.getFileResource("data/resources/rainbow.png"));
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, null);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (opacity / 100.0)));
        g.drawImage(rainbow, 0, 0, image.getWidth(), image.getHeight(), null);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(result, "png", os);
        g.dispose();
        return new ByteArrayInputStream(os.toByteArray());
    }

}
