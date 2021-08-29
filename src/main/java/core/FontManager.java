package core;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FontManager {

    private static final ArrayList<Font> fontList = new ArrayList<>();

    public static void init() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (File file : Objects.requireNonNull(new LocalFile(LocalFile.Directory.RESOURCES, "fonts").listFiles())) {
            try {
                Font font = Font.createFont(Font.TRUETYPE_FONT, new File(file.getAbsolutePath()));
                fontList.add(font);
                ge.registerFont(font);
            } catch (FontFormatException | IOException e) {
                MainLogger.get().error("Error for file {}", file.getName(), e);
            }
        }
    }

    public static Font getFirstFont(int size) {
        return fontList.get(0).deriveFont(Font.PLAIN, size);
    }

    public static List<Font> getFontList(int size) {
        return fontList.stream().map(font -> font.deriveFont(Font.PLAIN, size)).collect(Collectors.toList());
    }

    public static void reload() {
        fontList.clear();
        init();
    }

}
