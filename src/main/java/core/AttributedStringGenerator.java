package core;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.List;
import java.util.Optional;

public class AttributedStringGenerator {

    private final List<Font> fonts;

    public AttributedStringGenerator(int size) {
        fonts = FontContainer.getInstance().getFontList(size);
    }

    public AttributedCharacterIterator getIterator(String str) {
        AttributedString attributedString = new AttributedString(str);
        if (str.length() > 0) {
            int i = 0;
            while (i < str.length()) {
                Optional<FontBounds> fontBoundsOpt = getFontBounds(str.substring(i));
                if (fontBoundsOpt.isPresent()) {
                    FontBounds fontBounds = fontBoundsOpt.get();
                    attributedString.addAttribute(TextAttribute.FONT, fontBounds.font, i, i + fontBounds.length);
                    i += fontBounds.length;
                } else {
                    i++;
                }
            }
        }

        return attributedString.getIterator();
    }

    private Optional<FontBounds> getFontBounds(String str) {
        for (Font font : fonts) {
            int upTo = font.canDisplayUpTo(str);
            if (upTo == -1) {
                return Optional.of(new FontBounds(font, str.length()));
            } else if (upTo > 0) {
                return Optional.of(new FontBounds(font, upTo));
            }
        }

        return Optional.empty();
    }

    public Rectangle2D getStringBounds(String str, FontRenderContext frc) {
        TextLayout tl = new TextLayout(getIterator(str), frc);
        return tl.getBounds();
    }

    public Rectangle2D getStringBounds(AttributedCharacterIterator attributedCharacterIterator, FontRenderContext frc) {
        TextLayout tl = new TextLayout(attributedCharacterIterator, frc);
        return tl.getBounds();
    }


    private static class FontBounds {

        final Font font;
        final int length;

        public FontBounds(Font font, int length) {
            this.font = font;
            this.length = length;
        }

    }

}
