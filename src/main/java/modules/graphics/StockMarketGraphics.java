package modules.graphics;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.AttributedCharacterIterator;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import core.AttributedStringGenerator;
import core.MainLogger;
import core.utils.FutureUtil;
import modules.fishery.Stock;
import modules.fishery.StockMarket;

public class StockMarketGraphics {

    private static final int BASE_WIDTH = 800;
    private static final int BASE_HEIGHT = 350;
    private static final int BASE_ROUNDED = 30;
    private static final int BORDER_LEFTRIGHT = 60;
    private static final int BORDER_UPDOWN = 60;
    private static final int CIRCLE_SIZE_OUTER = 20;
    private static final int CIRCLE_SIZE = 14;

    private static final Color BACKGROUND_COLOR = new Color(32, 34, 37);

    public static CompletableFuture<InputStream> createImageGraph(Stock stock) {
        return FutureUtil.supplyAsync(() -> {
            try {
                BufferedImage drawImage = new BufferedImage(BASE_WIDTH, BASE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = GraphicsUtil.createGraphics(drawImage);

                drawBackground(g2d);
                boolean positive = isPositive(stock);
                Color color = positive ? new Color(64, 165, 98) : new Color(236, 68, 72);
                drawData(g2d, stock, color);

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

    private static void drawData(Graphics2D g2d, Stock stock, Color color) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        long max = -1;
        long min = -1;
        for (int i = 0; i <= 24 * 7; i++) {
            long value = StockMarket.getValue(stock, -i);
            if (value > max || max == -1) {
                max = value;
            }
            if (value < min || min == -1) {
                min = value;
            }
        }

        g2d.setColor(new Color(80, 80, 80));
        Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                0, new float[]{9}, 0);
        g2d.setStroke(dashed);
        int lineY = (int) mapY(stock, -24 * 7, min, max);
        g2d.drawLine(0, lineY, BASE_WIDTH, lineY);

        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(3));
        int pointer = 0;
        double x;
        do {
            x = mapX(pointer);
            g2d.drawLine(
                    (int) x,
                    (int) mapY(stock, pointer, min, max),
                    (int) mapX(pointer - 1),
                    (int) mapY(stock, pointer - 1, min, max)
            );
            pointer--;
        } while (x >= 0);

        AttributedStringGenerator font = new AttributedStringGenerator(28);
        FontRenderContext frc = new FontRenderContext(null, true, true);

        g2d.setColor(Color.WHITE);
        for (int i = 0; i <= 7; i++) {
            int offset = -i * 24;
            g2d.setColor(BACKGROUND_COLOR);
            g2d.fillOval(
                    (int) mapX(offset) - CIRCLE_SIZE_OUTER / 2,
                    (int) mapY(stock, offset, min, max) - CIRCLE_SIZE_OUTER / 2,
                    CIRCLE_SIZE_OUTER,
                    CIRCLE_SIZE_OUTER
            );
            g2d.setColor(Color.WHITE);
            g2d.fillOval(
                    (int) mapX(offset) - CIRCLE_SIZE / 2,
                    (int) mapY(stock, offset, min, max) - CIRCLE_SIZE / 2,
                    CIRCLE_SIZE,
                    CIRCLE_SIZE
            );

            int offsetSign = StockMarket.getValue(stock, offset) > StockMarket.getValue(stock, offset - 2) ? -1 : 1;
            String text = switch (i) {
                case 0 -> "NOW";
                case 1 -> "24H";
                default -> i + "D";
            };
            AttributedCharacterIterator iterator = GraphicsUtil.getNameIterator(frc, font, text, -1);
            Rectangle2D bounds = font.getStringBounds(iterator, frc);
            GraphicsUtil.drawStringWithBorder(
                    g2d,
                    iterator,
                    bounds,
                    Color.WHITE,
                    BACKGROUND_COLOR,
                    (int) mapX(offset),
                    (int) mapY(stock, offset, min, max) + 25 * offsetSign,
                    4,
                    -1
            );
        }
    }

    private static double mapX(int relative) {
        return BASE_WIDTH - BORDER_LEFTRIGHT + ((double) relative / (24 * 7)) * (BASE_WIDTH - 2 * BORDER_LEFTRIGHT);
    }

    private static double mapY(Stock stock, int relative, long min, long max) {
        double value = (double) StockMarket.getValue(stock, relative);
        return BASE_HEIGHT - BORDER_UPDOWN - ((value - min) / (max - min)) * (BASE_HEIGHT - 2 * BORDER_UPDOWN);
    }

    private static boolean isPositive(Stock stock) {
        return StockMarket.getValue(stock) >= StockMarket.getValue(stock, -24 * 7);
    }

    private static void drawBackground(Graphics2D g2d) {
        g2d.setColor(BACKGROUND_COLOR);
        g2d.fillRect(0, 0, BASE_WIDTH, BASE_HEIGHT);
    }

}
