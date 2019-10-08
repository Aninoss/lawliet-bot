package General;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.Map;
import java.util.Optional;

public class AttributedStringGenerator {

    private Font[] fonts;
    private final static int MAIN_FONT_POS = 1;

    public AttributedStringGenerator(int size) {
        Font fontMain = new Font("Segoe UI Symbol", Font.PLAIN, size);
        Font fontJapanese = new Font("MS UI Gothic", Font.PLAIN, size);
        Font fontLucida = new Font("Lucida Sans Unicode", Font.PLAIN, size);
        fonts = new Font[]{fontJapanese, fontMain, fontLucida};
    }

    public AttributedCharacterIterator getIterator(String str) {
        AttributedString astr = new AttributedString(str);
        if (!str.isEmpty()) astr.addAttribute(TextAttribute.FONT, fonts[MAIN_FONT_POS], 0, str.length());

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
        float width = 0, maxAscent = 0, maxDescent = 0, maxLeading = 0;

        while(!str.isEmpty()) {
            Pair<Font, Integer> values = getResponsibleFont(str).orElse(new Pair<>(fonts[MAIN_FONT_POS], 1));
            Font font = values.getKey();
            int n = values.getValue();

            TextLayout tl = new TextLayout(str.substring(0, n), font, frc);
            maxAscent = Math.max(maxAscent, tl.getAscent());
            maxDescent = Math.max(maxDescent, tl.getDescent());
            maxDescent = Math.max(maxDescent, tl.getDescent());
            maxLeading = Math.max(maxLeading, tl.getLeading());
            width += tl.getAdvance();

            str = str.substring(n);
        }

        return new Rectangle2D.Float(0, -maxAscent, width, maxAscent + maxDescent + maxLeading);
    }

}
