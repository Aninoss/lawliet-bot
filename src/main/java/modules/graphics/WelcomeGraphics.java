package modules.graphics;

import core.AttributedStringGenerator;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.text.AttributedCharacterIterator;

public class WelcomeGraphics {

    private final static Logger LOGGER = LoggerFactory.getLogger(WelcomeGraphics.class);

    private static final int BASE_WIDTH = 800;
    private static final int BASE_HEIGHT = 270;
    private static final int BASE_ROUNDED = 30;
    private static final int SPACE = 30;
    private static final int TEXT_SPACE = 54;
    private static final int TEXT_FONT_LARGE = 48;
    private static final int TEXT_FONT_SMALL = 38;
    private static final int AVATAR_ROUNDED = 30;
    private static final int SHADOW_SIZE = 10;
    private static final double SHADOW_OPACITY = 0.18;

    public static InputStream createImageWelcome(User user, Server server, String welcome) {
        try {
            BufferedImage backgroundImage = getBackgroundImage(server);
            BufferedImage avatarImage = getAvatarImage(user);
            BufferedImage drawImage = new BufferedImage(BASE_WIDTH, BASE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = Graphics.createGraphics(drawImage);

            drawBackground(g2d, backgroundImage);
            double lumi = drawLumi(g2d, drawImage);
            float shadowOpacity = (float) (SHADOW_OPACITY * lumi);

            drawAvatar(g2d, avatarImage, shadowOpacity);
            drawTexts(g2d, welcome, server, user, shadowOpacity);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(Graphics.makeRoundedCorner(drawImage, BASE_ROUNDED), "png", os);
            g2d.dispose();
            return new ByteArrayInputStream(os.toByteArray());
        } catch (IOException e) {
            LOGGER.error("Exception", e);
        }
        return null;
    }

    private static void drawTexts(Graphics2D g2d, String welcomeText, Server server, User user, float shadowOpacity) {
        final int BORDER = SPACE + TEXT_SPACE;
        FontRenderContext frc = new FontRenderContext(null, true, true);

        AttributedStringGenerator fontWelcome = new AttributedStringGenerator(TEXT_FONT_LARGE);
        AttributedStringGenerator fontName = new AttributedStringGenerator(TEXT_FONT_SMALL);

        AttributedCharacterIterator welcomeIterator = Graphics.getNameIterator(frc, fontWelcome, welcomeText, BASE_WIDTH - BASE_HEIGHT);
        AttributedCharacterIterator nameIterator = Graphics.getNameIterator(frc, fontName, user.getDisplayName(server), BASE_WIDTH - BASE_HEIGHT - SPACE);
        Rectangle2D welcomeBounds = fontWelcome.getStringBounds(welcomeIterator, frc);
        Rectangle2D nameBounds = fontName.getStringBounds(nameIterator, frc);

        g2d.setColor(Color.BLACK);
        Graphics.drawShadow(g2d, SHADOW_SIZE, shadowOpacity, offset -> g2d.drawString(welcomeIterator, getTextX(welcomeBounds.getWidth()) + offset, getTextHeight(frc, fontWelcome) + BORDER + offset));
        Graphics.drawShadow(g2d, SHADOW_SIZE, shadowOpacity, offset -> g2d.drawString(nameIterator, getTextX(nameBounds.getWidth()) + offset, BASE_HEIGHT - BORDER + offset));

        g2d.setColor(Color.WHITE);
        g2d.drawString(welcomeIterator, getTextX(welcomeBounds.getWidth()), getTextHeight(frc, fontWelcome) + BORDER);
        g2d.drawString(nameIterator, getTextX(nameBounds.getWidth()), BASE_HEIGHT - BORDER);
    }

    private static int getTextHeight(FontRenderContext frc, AttributedStringGenerator fontWelcome) {
        return (int)fontWelcome.getStringBounds("O", frc).getHeight();
    }

    private static int getTextX(double textWidth) {
        return (int)(BASE_HEIGHT - SPACE + (BASE_WIDTH - BASE_HEIGHT + SPACE) / 2.0 - textWidth / 2.0);
    }

    private static void drawAvatar(Graphics2D g2d, BufferedImage avatarImage, float shadowOpacity) {
        final int size = BASE_HEIGHT - SPACE * 2;

        Graphics.drawShadow(g2d, SHADOW_SIZE, shadowOpacity, offset -> g2d.drawImage(generateAvatarBlock(size, AVATAR_ROUNDED, Color.BLACK), SPACE + offset, SPACE + offset, size, size, null));
        g2d.drawImage(generateAvatarBlock(size, AVATAR_ROUNDED + 2, Color.WHITE), SPACE + 1, SPACE + 1, size - 2, size - 2, null);

        if (avatarImage != null) {
            avatarImage = Graphics.makeRoundedCorner(avatarImage, AVATAR_ROUNDED, size, size);
            g2d.drawImage(avatarImage, SPACE, SPACE, null);
        }
    }

    private static BufferedImage generateAvatarBlock(int size, int radius, Color color) {
        BufferedImage black = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D black2d = black.createGraphics();
        black2d.setColor(color);
        black2d.fillRect(0, 0, black.getWidth(), black.getHeight());
        black2d.dispose();
        return Graphics.makeRoundedCorner(black, radius);
    }

    private static double getAverageLuminance(BufferedImage image) {
        double totalLuminance = 0;
        int n = 0;
        for (int y = 0; y < image.getHeight(); y++)
        {
            for (int x = image.getWidth() / 3; x < image.getWidth(); x++)
            {
                int color = image.getRGB(x, y);

                // extract each color component
                int red   = (color >>> 16) & 0xFF;
                int green = (color >>>  8) & 0xFF;
                int blue  = (color) & 0xFF;

                // calc luminance in range 0.0 to 1.0; using SRGB luminance constants
                totalLuminance += Math.sqrt((red * 0.2126f + green * 0.7152f + blue * 0.0722f) / 255);
                n++;
            }
        }

        return totalLuminance / (double) n;
    }

    private static double drawLumi(Graphics2D g2d, BufferedImage drawImage) {
        double lumi = getAverageLuminance(drawImage);
        if (lumi >= 0.4) {
            g2d.setColor(new Color(0, 0, 0, (float) (lumi - 0.4)));
            g2d.fillRect(0, 0, BASE_WIDTH, BASE_HEIGHT);
        }
        return lumi;
    }

    private static void drawBackground(Graphics2D g2d, BufferedImage backgroundImage) {
        double scaledHeight = ((double) BASE_WIDTH / (double) backgroundImage.getWidth()) * (double) backgroundImage.getHeight();
        int yShift = (int) -Math.round(scaledHeight - BASE_HEIGHT) / 2;
        g2d.drawImage(backgroundImage, 0, yShift, BASE_WIDTH, (int) scaledHeight, null);
    }

    private static BufferedImage getAvatarImage(User user) {
        BufferedImage profilePicture = null;
        try {
            profilePicture = ImageIO.read(new URL(user.getAvatar().getUrl().toString() + "?size=256"));
        } catch (IOException e) {
            //Ignore
        }
        return profilePicture;
    }

    private static BufferedImage getBackgroundImage(Server server) throws IOException {
        BufferedImage base = ImageIO.read(getBackgroundFile(server));
        if (base == null) base = ImageIO.read(getDefaultBackgroundFile());
        return base;
    }

    private static File getBackgroundFile(Server server) {
        File backgroundFile = getDefaultBackgroundFile();
        String customBackgroundPath = "data/welcome_backgrounds/" + server.getIdAsString() + ".png";
        if (new File(customBackgroundPath).exists())
            backgroundFile = new File(customBackgroundPath);

        return backgroundFile;
    }

    private static File getDefaultBackgroundFile() {
        return new File("data/welcome_backgrounds/placeholder.png");
    }

}
