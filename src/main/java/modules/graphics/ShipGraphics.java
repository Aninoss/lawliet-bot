package modules.graphics;

import core.AttributedStringGenerator;
import org.javacord.api.entity.user.User;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.AttributedCharacterIterator;

public class ShipGraphics {

    public static InputStream createImageShip(User user1, User user2, int n, int perc) throws IOException {
        BufferedImage image = ImageIO.read(new File("data/resources/ship/" + n + ".png"));
        BufferedImage image1;
        BufferedImage image2;
        try {
            image1 = user1.getAvatar().asBufferedImage().get();
            image2 = user2.getAvatar().asBufferedImage().get();
        } catch (Throwable e) {
            //Ignore
            return null;
        }
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = Graphics.createGraphics(result);
        FileReader fReader = new FileReader("data/resources/ship/pos.txt");
        BufferedReader reader = new BufferedReader(fReader);

        String text;
        int fsize = 0;
        while (true) {
            text = reader.readLine();
            if (text == null) break;

            String[] temp = text.split(";");
            int[] wert = new int[temp.length];
            for (int i = 0; i < temp.length; i++) {
                wert[i] = Integer.parseInt(temp[i]);
            }
            if (wert[0] == n) {
                g.drawImage(image1, wert[1], wert[2], wert[3], wert[3], null);
                g.drawImage(image2, wert[4], wert[5], wert[6], wert[6], null);
                fsize = (int) (wert[7] * 1.5);
            }
        }

        g.drawImage(image, 0, 0, null);
        AttributedStringGenerator fontSimilarity = new AttributedStringGenerator(fsize);
        AttributedCharacterIterator simIterator = fontSimilarity.getIterator(perc + "%");
        Color mainColor = new Color((int) Math.min((510.0 - (perc / 100.0 * 255.0 * 2.0)), 255), (int) Math.min((perc / 100.0 * 255.0 * 2.0), 255), 0);
        FontRenderContext frc = new FontRenderContext(null, true, true);

        int y = (int) (image.getHeight() / 5.0 * 4.0);
        Graphics.drawStringWithBorder(g, simIterator, fontSimilarity.getStringBounds(perc + "%", frc),  mainColor,image.getWidth()/2, y - (int) (0.252 * fsize),4,-1);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(result, "png", os);
        g.dispose();
        return new ByteArrayInputStream(os.toByteArray());
    }

}
