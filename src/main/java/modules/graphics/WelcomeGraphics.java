package modules.graphics;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.AttributedCharacterIterator;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import core.AttributedStringGenerator;
import core.LocalFile;
import core.MainLogger;
import core.utils.FutureUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

public class WelcomeGraphics {

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

    public static CompletableFuture<InputStream> createImageWelcome(Member member, String welcome) {
        return FutureUtil.supplyAsync(() -> {
            try {
                BufferedImage backgroundImage = getBackgroundImage(member.getGuild());
                BufferedImage avatarImage = getAvatarImage(member.getUser());
                BufferedImage drawImage = new BufferedImage(BASE_WIDTH, BASE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = GraphicsUtil.createGraphics(drawImage);

                drawBackground(g2d, backgroundImage);
                double lumi = drawLumi(g2d, drawImage);
                float shadowOpacity = (float) (SHADOW_OPACITY * lumi);

                drawAvatar(g2d, avatarImage, shadowOpacity);
                drawTexts(g2d, welcome, member, shadowOpacity);

                try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                    ImageIO.write(GraphicsUtil.makeRoundedCorner(drawImage, BASE_ROUNDED), "png", os);
                    g2d.dispose();
                    return new ByteArrayInputStream(os.toByteArray());
                }
            } catch (IOException e) {
                MainLogger.get().error("Exception", e);
            }
            return null;
        });
    }

    private static void drawTexts(Graphics2D g2d, String welcomeText, Member member, float shadowOpacity) {
        final int BORDER = SPACE + TEXT_SPACE;
        FontRenderContext frc = new FontRenderContext(null, true, true);

        AttributedStringGenerator fontWelcome = new AttributedStringGenerator(TEXT_FONT_LARGE);
        AttributedStringGenerator fontName = new AttributedStringGenerator(TEXT_FONT_SMALL);

        AttributedCharacterIterator welcomeIterator = GraphicsUtil.getNameIterator(frc, fontWelcome, welcomeText, BASE_WIDTH - BASE_HEIGHT);
        AttributedCharacterIterator nameIterator = GraphicsUtil.getNameIterator(frc, fontName, member.getEffectiveName(), BASE_WIDTH - BASE_HEIGHT - SPACE);
        Rectangle2D welcomeBounds = fontWelcome.getStringBounds(welcomeIterator, frc);
        Rectangle2D nameBounds = fontName.getStringBounds(nameIterator, frc);

        g2d.setColor(Color.BLACK);
        GraphicsUtil.drawShadow(g2d, SHADOW_SIZE, shadowOpacity, offset -> g2d.drawString(welcomeIterator, getTextX(welcomeBounds.getWidth()) + offset, getTextHeight(frc, fontWelcome) + BORDER + offset));
        GraphicsUtil.drawShadow(g2d, SHADOW_SIZE, shadowOpacity, offset -> g2d.drawString(nameIterator, getTextX(nameBounds.getWidth()) + offset, BASE_HEIGHT - BORDER + offset));

        g2d.setColor(Color.WHITE);
        g2d.drawString(welcomeIterator, getTextX(welcomeBounds.getWidth()), getTextHeight(frc, fontWelcome) + BORDER);
        g2d.drawString(nameIterator, getTextX(nameBounds.getWidth()), BASE_HEIGHT - BORDER);
    }

    private static int getTextHeight(FontRenderContext frc, AttributedStringGenerator fontWelcome) {
        return (int) fontWelcome.getStringBounds("O", frc).getHeight();
    }

    private static int getTextX(double textWidth) {
        return (int) (BASE_HEIGHT - SPACE + (BASE_WIDTH - BASE_HEIGHT + SPACE) / 2.0 - textWidth / 2.0);
    }

    private static void drawAvatar(Graphics2D g2d, BufferedImage avatarImage, float shadowOpacity) {
        final int size = BASE_HEIGHT - SPACE * 2;

        GraphicsUtil.drawShadow(g2d, SHADOW_SIZE, shadowOpacity, offset -> g2d.drawImage(generateAvatarBlock(size, AVATAR_ROUNDED, Color.BLACK), SPACE + offset, SPACE + offset, size, size, null));
        g2d.drawImage(generateAvatarBlock(size, AVATAR_ROUNDED + 2, Color.WHITE), SPACE + 1, SPACE + 1, size - 2, size - 2, null);

        if (avatarImage != null) {
            avatarImage = GraphicsUtil.makeRoundedCorner(avatarImage, AVATAR_ROUNDED, size, size);
            g2d.drawImage(avatarImage, SPACE, SPACE, null);
        }
    }

    private static BufferedImage generateAvatarBlock(int size, int radius, Color color) {
        BufferedImage black = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D black2d = black.createGraphics();
        black2d.setColor(color);
        black2d.fillRect(0, 0, black.getWidth(), black.getHeight());
        black2d.dispose();
        return GraphicsUtil.makeRoundedCorner(black, radius);
    }

    private static double getAverageLuminance(BufferedImage image) {
        double totalLuminance = 0;
        int n = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = image.getWidth() / 3; x < image.getWidth(); x++) {
                int color = image.getRGB(x, y);

                // extract each color component
                int red = (color >>> 16) & 0xFF;
                int green = (color >>> 8) & 0xFF;
                int blue = (color) & 0xFF;

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
            profilePicture = ImageIO.read(new URL(user.getEffectiveAvatarUrl() + "?size=256"));
        } catch (IOException e) {
            //Ignore
        }
        return profilePicture;
    }

    private static BufferedImage getBackgroundImage(Guild guild) throws IOException {
        BufferedImage background = ImageIO.read(getBackgroundFile(guild));
        if (background == null) {
            background = ImageIO.read(getDefaultBackgroundFile());
        }
        return background;
    }

    private static LocalFile getBackgroundFile(Guild guild) {
        LocalFile syncedBackgroundFile = new LocalFile(LocalFile.Directory.WELCOME_BACKGROUNDS, String.format("%d.png", guild.getIdLong()));
        if (syncedBackgroundFile.exists()) {
            return syncedBackgroundFile;
        }

        return getDefaultBackgroundFile();
    }

    private static LocalFile getDefaultBackgroundFile() {
        return new LocalFile(LocalFile.Directory.WELCOME_BACKGROUNDS, "placeholder.png");
    }

}
