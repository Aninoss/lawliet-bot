package modules.graphics;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.AttributedCharacterIterator;
import java.util.function.Consumer;
import core.AttributedStringGenerator;

public class GraphicsUtil {

    public static BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
        return makeRoundedCorner(image, cornerRadius, image.getWidth(), image.getHeight());
    }

    public static BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius, int w, int h) {
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = output.createGraphics();

        RenderingHints rh = new RenderingHints(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHints(rh);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.setComposite(AlphaComposite.Src);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.drawImage(image, 0, 0, w, h,null);

        g2.dispose();

        return output;
    }

    public static AttributedCharacterIterator getNameIterator(FontRenderContext frc, AttributedStringGenerator fontName, String text, int maxWidth) {
        char ddd = '…';
        AttributedCharacterIterator nameIterator = fontName.getIterator(text);
        while(fontName.getStringBounds(text + ddd, frc).getWidth() > maxWidth) {
            text = text.substring(0, text.length() - 1);
            nameIterator = fontName.getIterator(text + ddd);
        }

        return nameIterator;
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

    public static void drawStringWithBorder(Graphics2D g2d, AttributedCharacterIterator iterator, Rectangle2D bounds, Color color, int x, int y, int thickness, double maxWidth) {
        g2d.setColor(Color.BLACK);
        double n = 8;
        for(double i = 0; i < n; i++) {
            double a = (int)((i/n)*360);
            double xT = (Math.cos(Math.toRadians(a)) * thickness);
            double yT = (Math.sin(Math.toRadians(a)) * thickness);

            double distanceFactor;
            distanceFactor = 1;

            int xNew = (int)Math.round(xT*distanceFactor);
            int yNew = (int)Math.round(yT*distanceFactor);

            drawStringCenter(g2d,iterator, bounds, x + xNew,y + yNew, maxWidth, 1);
        }

        g2d.setColor(color);
        drawStringCenter(g2d, iterator, bounds, x, y, maxWidth, 1);
    }

    public static BufferedImage getScaledImage(Image srcImg, int w, int h){
        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TRANSLUCENT);
        Graphics2D g2 = resizedImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(srcImg, 0, 0, w, h, null);
        g2.dispose();
        return resizedImg;
    }

    public static Graphics2D createGraphics(BufferedImage drawPanel) {
        Graphics2D g2d = drawPanel.createGraphics();
        RenderingHints rh = new RenderingHints(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHints(rh);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        return g2d;
    }

    public static void drawShadow(Graphics2D g2d, int size, float opacity, Consumer<Integer> drawable) {
        Composite compositeCache = g2d.getComposite();

        for(int i = 0; i < (double) size; i++) {
            float alpha = (25.0f * ((i+1) / (float) size) * opacity);
            AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 100.0f);
            g2d.setComposite(ac);
            drawable.accept(size - i);
        }
        g2d.setComposite(compositeCache);
    }

}

