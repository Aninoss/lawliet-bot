package Core;

import javafx.util.Pair;

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
        AttributedString astr = new AttributedString(str);
        if (!str.isEmpty()) astr.addAttribute(TextAttribute.FONT, fonts.get(0), 0, str.length());

        int begin = 0;
        while(!str.isEmpty()) {
            Pair<Font, Integer> values = getResponsibleFont(str).orElse(new Pair<>(null, 1));
            Font font = values.getKey();
            int n = values.getValue();

            if (font != null) {
                astr.addAttribute(TextAttribute.FONT, font, begin, begin + n);
            }
            begin += n;
            str = str.substring(n);
        }

        return astr.getIterator();
    }

    private Optional<Pair<Font, Integer>> getResponsibleFont(String str) {
        for(Font font: fonts) {
            int upTo = font.canDisplayUpTo(str);
            if (upTo != 0) {
                for(int i = 1; i <= 4; i++) {
                    upTo = font.canDisplayUpTo(str.substring(0, i));
                    if (upTo != 0) return Optional.of(new Pair<>(font, Math.abs(upTo)));
                }
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

}
