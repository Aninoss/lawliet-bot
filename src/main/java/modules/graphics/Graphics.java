package modules.graphics;

import core.AttributedStringGenerator;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.text.AttributedCharacterIterator;

public class Graphics {

    public static BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = output.createGraphics();

        // This is what we want, but it only does hard-clipping, i.e. aliasing
        // g2.setClip(new RoundRectangle2D ...)

        // so instead fake soft-clipping by first drawing the desired clip shape
        // in fully opaque white with antialiasing enabled...
        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));

        // ... then compositing the image on top,
        // using the white shape from above as alpha source
        g2.setComposite(AlphaComposite.SrcAtop);
        g2.drawImage(image, 0, 0, null);

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

    public static void drawRectShadow(Graphics2D g2d, int x, int y, int width, int height) {
        final double SHADOW_NUM = 5;
        final double MULTI = 1.03;
        for(double i = 0; i < SHADOW_NUM; i++) {
            g2d.setColor(new Color(0, 0, 0, (int) (10.0 * (i + 1) / SHADOW_NUM)));
            g2d.fillRect((int) (x - (width * (MULTI - 1) / 2) + (SHADOW_NUM - i)),  (int) (y - (height * (MULTI - 1) / 2) + (SHADOW_NUM - i)), (int) (width * MULTI), (int) (height * MULTI));
        }
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

}

