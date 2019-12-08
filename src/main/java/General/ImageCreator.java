package General;

import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import javax.imageio.ImageIO;
import javax.imageio.stream.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.awt.AlphaComposite;

public class ImageCreator {
    public static InputStream createImageTriggered(User user) throws IOException, ExecutionException, InterruptedException {
        BufferedImage image = user.getAvatar().asBufferedImage().get();
        double scale = 1.5;
        image = getScaledImage(image, (int) (image.getWidth() * scale), (int) (image.getHeight() * scale));

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
        }

        gifSequenceWriter.close();
        ios.close();
        os.close();

        return new ByteArrayInputStream(os.toByteArray());
    }

    public static InputStream createImageRainbow(User user) throws ExecutionException, InterruptedException, IOException {
        BufferedImage image = user.getAvatar().asBufferedImage().get();
        double scale = 1.5;
        image = getScaledImage(image, (int) (image.getWidth() * scale), (int) (image.getHeight() * scale));
        BufferedImage rainbow = ImageIO.read(new File("recourses/rainbow.png"));
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, null);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.5)));
        g.drawImage(rainbow, 0, 0, image.getWidth(), image.getHeight(), null);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(result, "png", os);
        return new ByteArrayInputStream(os.toByteArray());
    }

    public static InputStream createImageShip(Locale locale, User user1, User user2, int n, int perc) throws IOException {
        BufferedImage image = ImageIO.read(new File("recourses/ship/" + n + ".png"));
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

        Graphics2D g = result.createGraphics();
        RenderingHints rh = new RenderingHints(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHints(rh);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        FileReader fReader = new FileReader(new File("recourses/ship/pos.txt"));
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
                fsize = (int) (wert[7] * 1.25);
            }
        }

        g.drawImage(image, 0, 0, null);
        Font fontTop = new Font("Impact", Font.PLAIN, (int) (fsize / 1.25));
        Font font = new Font("Impact", Font.PLAIN, fsize);
        Color mainColor = new Color((int) Math.min((510.0 - (perc / 100.0 * 255.0 * 2.0)), 255), (int) Math.min((perc / 100.0 * 255.0 * 2.0), 255), 0);
        Color mainColorShadow = new Color((int) (Math.min((510.0 - (perc / 100.0 * 255.0 * 2.0)), 255) / 2), (int) (Math.min((perc / 100.0 * 255.0 * 2.0), 255) / 2), 0);
        final double SHADOW_HEIGHT = 3;

        drawStringWithBorder(g, fontTop, TextManager.getString(locale,TextManager.COMMANDS,"ship_match"), Color.WHITE,image.getWidth()/2,(int) (image.getHeight() / 5.0 * 1.0),3,-1);
        drawStringWithBorder(g, font, perc + "%", mainColor, image.getWidth()/2, (int) (image.getHeight() / 5.0 * 4.0),3,-1);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(result, "png", os);
        return new ByteArrayInputStream(os.toByteArray());
    }

    public static InputStream createImageWelcome(User user, Server server, String welcome) {
        try {
            final int BASE_WIDTH = 400, BASE_HEIGHT = 135;

            BufferedImage base = ImageIO.read(getBackgroundFile(server));
            if (base == null) base = ImageIO.read(getDefaultBackgroundFile());

            BufferedImage profilePicture = null;
            try {
                profilePicture = user.getAvatar().asBufferedImage().get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            BufferedImage result = new BufferedImage(BASE_WIDTH, BASE_HEIGHT, BufferedImage.TYPE_INT_ARGB);

            Graphics2D g2d = result.createGraphics();

            RenderingHints rh = new RenderingHints(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHints(rh);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            double scaledHeight = ((double) BASE_WIDTH / (double) base.getWidth()) * (double) base.getHeight();
            int yShift = (int) -Math.round(scaledHeight - BASE_HEIGHT) / 2;
            g2d.drawImage(base, 0, yShift, BASE_WIDTH, (int) scaledHeight, null);

            g2d.setColor(new Color(0, 0, 0, 120));
            g2d.fillRect(0, 0, BASE_WIDTH, BASE_HEIGHT);

            g2d.setColor(Color.BLACK);
            g2d.fillRect(15, 15, BASE_HEIGHT - 30, BASE_HEIGHT - 30);

            drawRectShadow(g2d, 15, 15, BASE_HEIGHT - 30, BASE_HEIGHT - 30);
            if (profilePicture != null) g2d.drawImage(profilePicture, 15, 15, BASE_HEIGHT - 30, BASE_HEIGHT - 30, null);

            AttributedStringGenerator attributedStringGenerator = new AttributedStringGenerator(22);
            Font fontWelcome = new Font("Oswald", Font.PLAIN, 28);

            int drawX = BASE_HEIGHT - 15 + (BASE_WIDTH - BASE_HEIGHT + 15) / 2;
            int maxWidth = BASE_WIDTH - BASE_HEIGHT + 15 - 30;

            FontRenderContext frc =
                    new FontRenderContext(null, true, false);

            AttributedCharacterIterator aci = attributedStringGenerator.getIterator(user.getDisplayName(server));
            Rectangle2D bounds = attributedStringGenerator.getStringBounds(aci, frc);

            double textHeight0 = bounds.getHeight();
            double textHeight1 = fontWelcome.getStringBounds(welcome, frc).getHeight();
            double textHeightTotal = textHeight0 + textHeight1 + 15;
            int y0 = (int) (BASE_HEIGHT / 2 - textHeightTotal / 2 + textHeight0 / 2) + 5;
            int y1 = (int) (BASE_HEIGHT / 2 + textHeightTotal / 2 - textHeight1 / 2) + 5;

            drawStringShadow(g2d, fontWelcome, welcome, drawX, y0,  maxWidth, 1);
            drawStringShadow(g2d, aci, bounds, drawX, y1, maxWidth, 1);

            g2d.setColor(Color.WHITE);
            drawStringCenter(g2d, fontWelcome, welcome, drawX, y0, maxWidth, 1);
            drawStringCenter(g2d, aci, bounds, drawX, y1, maxWidth, 1);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(result, "png", os);
            return new ByteArrayInputStream(os.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static File getBackgroundFile(Server server) {
        File backgroundFile = getDefaultBackgroundFile();
        String customBackgroundPath = "data/welcome_backgrounds/" + server.getIdAsString() + ".png";
        if (new File(customBackgroundPath).exists()) backgroundFile = new File(customBackgroundPath);

        return backgroundFile;
    }

    private static File getDefaultBackgroundFile() {
        return new File("data/welcome_backgrounds/placeholder.png");
    }

    private static void drawRectShadow(Graphics2D g2d, int x, int y, int width, int height) {
        final double SHADOW_NUM = 5;
        final double MULTI = 1.03;
        for(double i=0; i < SHADOW_NUM; i++) {
            g2d.setColor(new Color(0, 0, 0, (int) (20.0 * (i+1) / SHADOW_NUM)));
            g2d.fillRect((int) (x - (width * (MULTI - 1) / 2) + (SHADOW_NUM - i)),  (int) (y - (height * (MULTI - 1) / 2) + (SHADOW_NUM - i)), (int) (width * MULTI), (int) (height * MULTI));
        }
    }

    private static AttributedCharacterIterator convertStringToAttributedCharacterIterator(Font font, String str) {
        AttributedString astr = new AttributedString(str);
        astr.addAttribute(TextAttribute.FONT, font, 0, str.length());
        return astr.getIterator();
    }

    private static void drawStringShadow(Graphics2D g2d, Font font, String string, int x, int y, double maxWidth, double width) {
        FontRenderContext frc =
                new FontRenderContext(null, true, false);
        drawStringShadow(g2d, convertStringToAttributedCharacterIterator(font, string), font.getStringBounds(string, frc), x, y, maxWidth, width);
    }

    private static void drawStringShadow(Graphics2D g2d, AttributedCharacterIterator attributedCharacterIterator, Rectangle2D bounds, int x, int y, double maxWidth, double width) {
        final double SHADOW_NUM = 5;
        for(double i=0; i < SHADOW_NUM; i++) {
            g2d.setColor(new Color(0, 0, 0, (int) (30.0 * ((i+1) / SHADOW_NUM))));
            drawStringCenter(g2d, attributedCharacterIterator, bounds, (int) (x + (SHADOW_NUM - i)), (int) (y + (SHADOW_NUM - i)), maxWidth, width);
        }
    }

    private static void drawStringCenter(Graphics2D g2d, Font font, String string, int x, int y, double maxWidth, double width) {
        FontRenderContext frc =
                new FontRenderContext(null, true, false);
        drawStringCenter(g2d, convertStringToAttributedCharacterIterator(font, string), font.getStringBounds(string, frc), x, y, maxWidth, width);
    }

    private static void drawStringCenter(Graphics2D g2d, AttributedCharacterIterator aci, Rectangle2D bounds, int x, int y, double maxWidth, double width) {
        g2d = (Graphics2D) g2d.create();

        double stringHeight = bounds.getHeight();
        double stringWidth = bounds.getWidth() * width;
        double scale;

        scale = Math.min(1, maxWidth / stringWidth);
        if (maxWidth < 0) scale = 1;

        AffineTransform trans = new AffineTransform();
        trans.scale(scale * width, 1);
        trans.translate(x * ((1.0 / scale) - 1),0);
        g2d.setTransform(trans);

        g2d.drawString(aci, (int)((x - stringWidth / 2.0) / width), (int)(y + stringHeight / 2.0));
        g2d.dispose();
    }

    private static void drawStringWithBorder(Graphics2D g2d, Font font, String string, Color color, int x, int y, int thickness, double maxWidth) {
        g2d.setColor(Color.BLACK);
        double n = 8;
        boolean blockBorder = false;
        for(double i=0; i<n; i++) {
            double a = (int)((i/n)*360);
            double xT = (Math.cos(Math.toRadians(a)) * thickness);
            double yT = (Math.sin(Math.toRadians(a)) * thickness);

            double distanceFactor;
            if (blockBorder) distanceFactor = 3/(Math.max(Math.abs(xT),Math.abs(yT)));
            else  distanceFactor = 1;

            int xNew = (int)Math.round(xT*distanceFactor);
            int yNew = (int)Math.round(yT*distanceFactor);

            drawStringCenter(g2d,font,string, x+xNew,y+yNew, maxWidth, 1);
        }

        g2d.setColor(color);
        drawStringCenter(g2d,font,string, x,y, maxWidth, 1);
    }

    private static BufferedImage getScaledImage(Image srcImg, int w, int h){
        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TRANSLUCENT);
        Graphics2D g2 = resizedImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, w, h, null);
        g2.dispose();
        return resizedImg;
    }

}

