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
        drawAccountTexts(g2d, locale, values, valueChanges, rank, totalRank, rankChange, subtext);

        for (int i = 0; i < activePowerUps.size(); i++) {
            drawPowerUp(g2d, activePowerUps.get(i), i);
        }

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(drawImage, "png", os);
            g2d.dispose();
            return new ByteArrayInputStream(os.toByteArray());
        }
    }

    private static void drawAccountTexts(Graphics2D g2d, Locale locale, long[] values, long[] valueChanges,
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
            drawAccountSlot(g2d, frc, fontSmall, label, values[i], valueChanges[i], i, valueChanges[i] >= 0 ? Color.GREEN : Color.RED);
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

    private static void drawAccountSlot(Graphics2D g2d, FontRenderContext frc, AttributedStringGenerator fontSmall, String label,
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

    public static InputStream createGearCard(Locale locale, long[] levels, long[] values, String roleName, long coinGiftLimit, boolean powerUpBonus) throws IOException {
        BufferedImage backgroundImage = ImageIO.read(new LocalFile(LocalFile.Directory.RESOURCES,"fishery_gear_card.png"));
        BufferedImage drawImage = new BufferedImage(backgroundImage.getWidth(), backgroundImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = GraphicsUtil.createGraphics(drawImage);

        g2d.drawImage(backgroundImage, 0, 0, backgroundImage.getWidth(), backgroundImage.getHeight(), null);
        drawGearTexts(g2d, locale, levels, values, roleName, coinGiftLimit, powerUpBonus);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(drawImage, "png", os);
            g2d.dispose();
            return new ByteArrayInputStream(os.toByteArray());
        }
    }

    private static void drawGearTexts(Graphics2D g2d, Locale locale, long[] levels, long[] values, String roleName, long coinGiftLimit, boolean powerUpBonus) throws IOException {
        FontRenderContext frc = new FontRenderContext(null, true, true);
        g2d.setColor(Color.WHITE);

        AttributedStringGenerator fontLarge = new AttributedStringGenerator(TEXT_FONT_LARGE);
        AttributedStringGenerator fontSmall = new AttributedStringGenerator(TEXT_FONT_SMALL);
        AttributedStringGenerator fontExtraSmall = new AttributedStringGenerator(TEXT_FONT_EXTRA_SMALL);

        g2d.drawString(fontLarge.getIterator(TextManager.getString(locale, Category.FISHERY, "fisherycat_fisherygear")), 50, 50 + getTextHeight(frc, fontLarge));

        String[] labels = TextManager.getString(locale, Category.FISHERY, "fisherycat_gearvaluelabels").split("\n");
        for (int i = 0; i < labels.length; i++) {
            String label = labels[i];
            String valueString = i == 4 ? roleName : "+" + StringUtil.numToString(values[i]);
            if ((i == 0 || i == 2) && powerUpBonus) {
                valueString += " (+" + StringUtil.numToString(Math.round(values[i] * 0.25)) + ")";
            }

            drawGearSlot(g2d, locale, frc, fontSmall, fontExtraSmall, label, i, StringUtil.numToString(levels[i]), valueString);
        }

        String coinGiftLabel = TextManager.getString(locale, Category.FISHERY, "fisherycat_gear_giftlimit");
        g2d.setColor(new Color(0.5f, 0.5f, 0.5f));
        g2d.drawString(fontExtraSmall.getIterator(coinGiftLabel), 50, 526 + getTextHeight(frc, fontExtraSmall));
        g2d.setColor(Color.WHITE);
        int labelWidth = (int) fontExtraSmall.getStringBounds(coinGiftLabel, frc).getWidth();

        BufferedImage coinImage = ImageIO.read(new LocalFile(LocalFile.Directory.RESOURCES,"coin.png"));
        g2d.drawImage(coinImage, 65 + labelWidth, 521, null);
        g2d.drawString(fontExtraSmall.getIterator(coinGiftLimit != 0 ? StringUtil.numToString(coinGiftLimit) : "∞"), 98 + labelWidth, 526 + getTextHeight(frc, fontExtraSmall));
    }

    private static void drawGearSlot(Graphics2D g2d, Locale locale, FontRenderContext frc, AttributedStringGenerator fontSmall,
                                     AttributedStringGenerator fontExtraSmall, String label, int i,
                                     String level, String value) {
        int xAdd = (i / 4) * 368;
        int yAdd = (i % 4) * 98;

        drawStringScaledToBounds(g2d, frc, fontSmall, label, 111 + xAdd, 147 + yAdd + getTextHeight(frc, fontSmall) / 2, 168, false);

        String levelString = TextManager.getString(locale, Category.FISHERY, "fisherycat_level", level);
        double levelWidth = fontSmall.getStringBounds(levelString, frc).getWidth();
        double levelScaleX = Math.min(1.0, 75.0 / levelWidth);

        g2d.setColor(Color.BLACK);
        drawStringScaled(g2d, frc, fontSmall, levelString, (int) (350 + xAdd - Math.min(75.0, levelWidth) / 2.0), 147 + yAdd + getTextHeight(frc, fontSmall) / 2, levelScaleX, false);
        g2d.setColor(Color.WHITE);

        double valueWidth = fontExtraSmall.getStringBounds(value, frc).getWidth();
        if (i != 4) {
            valueWidth += 10 + fontExtraSmall.getStringBounds(TextManager.getString(locale, Category.FISHERY, "fisherycat_gearsubtitle_" + i), frc).getWidth();
        }
        double valueScaleX = Math.min(1.0, (279.0 + (i == 4 ? 33.0 : 0.0)) / valueWidth);

        double valueNumberWidth = drawStringScaled(g2d, frc, fontExtraSmall, value, 129 + xAdd - (i == 4 ? 33 : 0), 181 + yAdd + getTextHeight(frc, fontExtraSmall), valueScaleX, false);
        if (i != 4) {
            g2d.setColor(new Color(0.5f, 0.5f, 0.5f));
            drawStringScaled(g2d, frc, fontExtraSmall, TextManager.getString(locale, Category.FISHERY, "fisherycat_gearsubtitle_" + i),
                    (int) (129 + xAdd + valueNumberWidth + 10.0 * valueScaleX), 181 + yAdd + getTextHeight(frc, fontExtraSmall), valueScaleX, false
            );
            g2d.setColor(Color.WHITE);
        }
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
