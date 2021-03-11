package modules.graphics;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.text.AttributedCharacterIterator;
import javax.imageio.ImageIO;
import core.AttributedStringGenerator;
import core.ResourceHandler;
import net.dv8tion.jda.api.entities.User;

public class ShipGraphics {

    public static InputStream createImageShip(User user1, User user2, int n, int percentage) throws IOException {
        BufferedImage image = ImageIO.read(ResourceHandler.getFileResource("data/resources/ship/" + n + ".png"));
        BufferedImage image1;
        BufferedImage image2;
        try {
            image1 = ImageIO.read(new URL(user1.getEffectiveAvatarUrl()));
            image2 = ImageIO.read(new URL(user2.getEffectiveAvatarUrl()));
        } catch (Throwable e) {
            //Ignore
            return null;
        }
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = GraphicsUtil.createGraphics(result);
        FileReader fReader = new FileReader(ResourceHandler.translateRelativePath("data/resources/ship/pos.txt"));
        BufferedReader reader = new BufferedReader(fReader);

        String text;
        int fsize = 0;
        while (true) {
            text = reader.readLine();
            if (text == null) break;

            String[] temp = text.split(";");
            int[] values = new int[temp.length];
            for (int i = 0; i < temp.length; i++) {
                values[i] = Integer.parseInt(temp[i]);
            }
            if (values[0] == n) {
                g.drawImage(image1, values[1], values[2], values[3], values[3], null);
                g.drawImage(image2, values[4], values[5], values[6], values[6], null);
                fsize = (int) (values[7] * 1.5);
            }
        }

        g.drawImage(image, 0, 0, null);
        AttributedStringGenerator fontSimilarity = new AttributedStringGenerator(fsize);
        AttributedCharacterIterator simIterator = fontSimilarity.getIterator(percentage + "%");
        Color mainColor = new Color((int) Math.min((510.0 - (percentage / 100.0 * 255.0 * 2.0)), 255), (int) Math.min((percentage / 100.0 * 255.0 * 2.0), 255), 0);
        FontRenderContext frc = new FontRenderContext(null, true, true);

        int y = (int) (image.getHeight() / 5.0 * 4.0);
        GraphicsUtil.drawStringWithBorder(g, simIterator, fontSimilarity.getStringBounds(percentage + "%", frc), mainColor, image.getWidth() / 2, y - (int) (0.252 * fsize), 4, -1);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(result, "png", os);
        g.dispose();
        return new ByteArrayInputStream(os.toByteArray());
    }

}
