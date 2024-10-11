package modules.graphics;

import commands.Category;
import core.AttributedStringGenerator;
import core.LocalFile;
import core.TextManager;
import core.utils.StringUtil;
import modules.fishery.FisheryPowerUp;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class FisheryGraphics {

    private static final int TEXT_FONT_LARGE = 45;
    private static final int TEXT_FONT_SMALL = 25;
    private static final int TEXT_FONT_EXTRA_SMALL = 20;

    public static InputStream createAccountCard(Locale locale, long[] values, long[] valueChanges,
                                                long rank, long totalRank, long rankChange,
                                                List<FisheryPowerUp> activePowerUps, String subtext
    ) throws IOException {
        BufferedImage backgroundImage = ImageIO.read(new LocalFile(LocalFile.Directory.RESOURCES, subtext != null ? "fishery_account_large.png" : "fishery_account.png"));
        BufferedImage drawImage = new BufferedImage(backgroundImage.getWidth(), backgroundImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = GraphicsUtil.createGraphics(drawImage);

        g2d.drawImage(backgroundImage, 0, 0, backgroundImage.getWidth(), backgroundImage.getHeight(), null);
        drawTexts(g2d, locale, values, valueChanges, rank, totalRank, rankChange, subtext);

        for (int i = 0; i < activePowerUps.size(); i++) {
            drawPowerUp(g2d, activePowerUps.get(i), i);
        }

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(drawImage, "png", os);
            g2d.dispose();
            return new ByteArrayInputStream(os.toByteArray());
        }
    }

    private static void drawTexts(Graphics2D g2d, Locale locale, long[] values, long[] valueChanges,
                                  long rank, long totalRank, long rankChange, String subtext
    ) {
        FontRenderContext frc = new FontRenderContext(null, true, true);
        g2d.setColor(Color.WHITE);

        AttributedStringGenerator fontLarge = new AttributedStringGenerator(TEXT_FONT_LARGE);
        AttributedStringGenerator fontSmall = new AttributedStringGenerator(TEXT_FONT_SMALL);
        AttributedStringGenerator fontExtraSmall = new AttributedStringGenerator(TEXT_FONT_EXTRA_SMALL);

        g2d.drawString(fontLarge.getIterator(TextManager.getString(locale, Category.FISHERY, "fisherycat_fisheryaccount")), 50, 50 + getTextHeight(frc, fontLarge));

        String[] labels = TextManager.getString(locale, Category.FISHERY, "fisherycat_valuelabels").split("\n");
        for (int i = 0; i < labels.length; i++) {
            String label = labels[i];
            drawSlot(g2d, frc, fontSmall, label, values[i], valueChanges[i], i, valueChanges[i] >= 0 ? Color.GREEN : Color.RED);
        }

        String rankChangeString = rankChange != 0 ? (" (" + (rankChange >= 0 ? "+" : "") + StringUtil.numToString(rankChange) + ")") : null;
        drawRank(g2d, frc, fontSmall, fontLarge, TextManager.getString(locale, Category.FISHERY, "fisherycat_serverrank"),
                "#" + StringUtil.numToString(rank), "/ " + StringUtil.numToString(totalRank), rankChangeString, rankChange <= 0 ? Color.GREEN : Color.RED);

        if (subtext != null) {
            g2d.setColor(new Color(0.5f, 0.5f, 0.5f));
            g2d.drawString(fontExtraSmall.getIterator(subtext), 50, 468 + getTextHeight(frc, fontExtraSmall));
            g2d.setColor(Color.WHITE);
        }
    }

    private static void drawSlot(Graphics2D g2d, FontRenderContext frc, AttributedStringGenerator fontSmall, String label,
                                 long value, long valueChange, int i, Color valueChangeColor
    ) {
        drawStringScaledToBounds(g2d, frc, fontSmall, label, 110, 147 + 58 * i + getTextHeight(frc, fontSmall) / 2, 200, false);

        String valueString = StringUtil.numToString(value);
        String valueChangeString = null;

        double width = fontSmall.getStringBounds(valueString, frc).getWidth();
        if (valueChange != 0) {
            valueChangeString = "(" + (valueChange >= 0 ? "+" : "") + StringUtil.numToString(valueChange) + ")";
            width += 10 + fontSmall.getStringBounds(valueChangeString, frc).getWidth();
        }
        double scaleX = Math.min(1.0, 281.0 / width);

        double valueWidth = drawStringScaled(g2d, frc, fontSmall, valueString, 631, 147 + 58 * i + getTextHeight(frc, fontSmall) / 2, scaleX, true);
        if (valueChangeString != null) {
            g2d.setColor(valueChangeColor);
            drawStringScaled(g2d, frc, fontSmall, valueChangeString, (int) (631 - valueWidth - 10 * scaleX), 147 + 58 * i + getTextHeight(frc, fontSmall) / 2, scaleX, true);
            g2d.setColor(Color.WHITE);
        }
    }

    private static void drawRank(Graphics2D g2d, FontRenderContext frc, AttributedStringGenerator fontSmall,
                                 AttributedStringGenerator fontLarge, String label, String rank, String rankTotal,
                                 String rankChange, Color rankChangeColor
    ) {
        g2d.drawString(fontSmall.getIterator(label), 50, 394);

        double width = fontLarge.getStringBounds(rank, frc).getWidth() + 14 + fontSmall.getStringBounds(rankTotal, frc).getWidth();
        if (rankChange != null) {
            width += fontSmall.getStringBounds(rankChange, frc).getWidth();
        }

        double scaleX = Math.min(1.0, 296.0 / width);

        g2d.setColor(new Color(102, 168, 255));
        double rankWidth = drawStringScaled(g2d, frc, fontLarge, rank, 50, 438, scaleX, false);

        g2d.setColor(Color.WHITE);
        double totalRankWidth = drawStringScaled(g2d, frc, fontSmall, rankTotal, (int) (50 + 14 * scaleX + rankWidth), 438, scaleX, false);

        if (rankChange != null) {
            g2d.setColor(rankChangeColor);
            drawStringScaled(g2d, frc, fontSmall, rankChange, (int) (50 + 14 * scaleX + rankWidth + totalRankWidth), 438, scaleX, false);
            g2d.setColor(Color.WHITE);
        }
    }

    private static void drawPowerUp(Graphics2D g2d, FisheryPowerUp powerUp, int i) throws IOException {
        BufferedImage image = ImageIO.read(new LocalFile(LocalFile.Directory.RESOURCES, String.format("powerup/%s.png", powerUp.name().toLowerCase())));
        g2d.drawImage(image, 650 - 64 - (64 + 8) * i, 374,null);
    }

    private static int getTextHeight(FontRenderContext frc, AttributedStringGenerator fontWelcome) {
        return (int) fontWelcome.getStringBounds("O", frc).getHeight();
    }

    private static double drawStringScaledToBounds(Graphics2D g2d, FontRenderContext frc, AttributedStringGenerator font, String text, int x, int y, int maxWidth, boolean alignRight) {
        double scaleX = Math.min(1.0, (double) maxWidth / font.getStringBounds(text, frc).getWidth());
        return drawStringScaled(g2d, frc, font, text, x, y, scaleX, alignRight);
    }

    private static double drawStringScaled(Graphics2D g2d, FontRenderContext frc, AttributedStringGenerator font, String text, int x, int y, double scaleX, boolean alignRight) {
        double width = font.getStringBounds(text, frc).getWidth() * scaleX;
        if (alignRight) {
            x -= width;
        }

        AffineTransform transform = new AffineTransform();
        transform.scale(scaleX, 1.0);
        g2d.setTransform(transform);
        g2d.drawString(font.getIterator(text), (float) (x / scaleX), (float) y);

        transform = new AffineTransform();
        transform.scale(1.0, 1.0);
        g2d.setTransform(transform);
        return width;
    }

}
