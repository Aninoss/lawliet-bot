package Core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FontContainer {

    final static Logger LOGGER = LoggerFactory.getLogger(FontContainer.class);

    public static final FontContainer ourInstance = new FontContainer();
    public static FontContainer getInstance() { return ourInstance; }
    private FontContainer() {}

    private final ArrayList<Font> fontList = new ArrayList<>();

    public void init() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for(File file : Objects.requireNonNull(new File("recourses/fonts").listFiles())) {
            try {
                Font font = Font.createFont(Font.TRUETYPE_FONT, new File(file.getAbsolutePath()));
                fontList.add(font);
                ge.registerFont(font);
            } catch (FontFormatException | IOException e) {
                LOGGER.error("Error for file {}", file.getName(), e);
            }
        }
    }

    public List<Font> getFontList(int size) {
        return fontList.stream().map(font -> font.deriveFont(Font.PLAIN, size)).collect(Collectors.toList());
    }

    public void reload() {
        fontList.clear();
        init();
    }

}
