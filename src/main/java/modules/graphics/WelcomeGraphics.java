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
import java.text.AttributedCharacterIterator;
import java.util.concurrent.ExecutionException;

public class WelcomeGraphics {

    private final static Logger LOGGER = LoggerFactory.getLogger(WelcomeGraphics.class);
    private static final int BASE_WIDTH = 400, BASE_HEIGHT = 135;

    public static InputStream createImageWelcome(User user, Server server, String welcome) {
        try {
            BufferedImage backgroundImage = getBackgroundImage(server);
            BufferedImage avatarImage = getAvatarImage(user);
            BufferedImage drawImage = new BufferedImage(BASE_WIDTH, BASE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = Graphics.createGraphics(drawImage);

            drawBackground(g2d, backgroundImage);
            drawLumi(g2d, drawImage);
            drawAvatar(g2d, avatarImage);
            drawTexts(g2d, welcome, server, user);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(drawImage, "png", os);
            return new ByteArrayInputStream(os.toByteArray());
        } catch (IOException e) {
            LOGGER.error("Exception", e);
        }
        return null;
    }

    private static void drawTexts(Graphics2D g2d, String welcomeText, Server server, User user) {
        final int BORDER = 42;
        FontRenderContext frc = new FontRenderContext(null, true, true);

        AttributedStringGenerator fontWelcome = new AttributedStringGenerator(25);
        AttributedStringGenerator fontName = new AttributedStringGenerator(20);

        AttributedCharacterIterator welcomeIterator = Graphics.getNameIterator(frc, fontWelcome, welcomeText, BASE_WIDTH - BASE_HEIGHT);
        AttributedCharacterIterator nameIterator = Graphics.getNameIterator(frc, fontName, user.getDisplayName(server), BASE_WIDTH - BASE_HEIGHT - 15);
        Rectangle2D welcomeBounds = fontWelcome.getStringBounds(welcomeIterator, frc);
        Rectangle2D nameBounds = fontName.getStringBounds(nameIterator, frc);

        g2d.setColor(Color.WHITE);
        g2d.drawString(welcomeIterator, getTextX(welcomeBounds.getWidth()), getTextHeight(frc, fontWelcome) + BORDER);
        g2d.drawString(nameIterator, getTextX(nameBounds.getWidth()), BASE_HEIGHT - BORDER);
    }

    private static int getTextHeight(FontRenderContext frc, AttributedStringGenerator fontWelcome) {
        return (int)fontWelcome.getStringBounds("O", frc).getHeight();
    }

    private static int getTextX(double textWidth) {
        return (int)(BASE_HEIGHT - 15 + (BASE_WIDTH - BASE_HEIGHT + 15) / 2.0 - textWidth / 2.0);
    }

    private static void drawAvatar(Graphics2D g2d, BufferedImage avatarImage) {
        final int size = BASE_HEIGHT - 30;
        final int radius = 12;

        BufferedImage black = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D black2d = black.createGraphics();
        black2d.setColor(Color.BLACK);
        black2d.fillRect(0, 0, black.getWidth(), black.getHeight());
        black = Graphics.makeRoundedCorner(black, radius);
        g2d.drawImage(black, 15, 15, size, size, null);

        if (avatarImage != null) {
            avatarImage = Graphics.makeRoundedCorner(avatarImage, radius);
            g2d.drawImage(avatarImage, 15, 15, size, size, null);
        }
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

    private static void drawLumi(Graphics2D g2d, BufferedImage drawImage) {
        double lumi = getAverageLuminance(drawImage);
        if (lumi >= 0.4) {
            g2d.setColor(new Color(0, 0, 0, (float) (lumi - 0.4)));
            g2d.fillRect(0, 0, BASE_WIDTH, BASE_HEIGHT);
        }
    }

    private static void drawBackground(Graphics2D g2d, BufferedImage backgroundImage) {
        double scaledHeight = ((double) BASE_WIDTH / (double) backgroundImage.getWidth()) * (double) backgroundImage.getHeight();
        int yShift = (int) -Math.round(scaledHeight - BASE_HEIGHT) / 2;
        g2d.drawImage(backgroundImage, 0, yShift, BASE_WIDTH, (int) scaledHeight, null);
    }

    private static BufferedImage getAvatarImage(User user) {
        BufferedImage profilePicture = null;
        try {
            profilePicture = user.getAvatar().asBufferedImage().get();
        } catch (InterruptedException | ExecutionException e) {
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
