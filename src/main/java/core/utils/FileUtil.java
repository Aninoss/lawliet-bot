package core.utils;

import org.javacord.api.entity.message.MessageAttachment;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class FileUtil {

    public static Optional<File> downloadMessageAttachment(MessageAttachment messageAttachment, String fileName) throws IOException {
        return downloadMessageAttachment(messageAttachment, new File(fileName));
    }

    public static Optional<File> downloadMessageAttachment(MessageAttachment messageAttachment, File file) throws IOException {
        BufferedImage bi;
        try {
            bi = messageAttachment.downloadAsImage().get();
        } catch (Throwable e) {
            return Optional.empty();
        }
        if (bi == null) {
            return Optional.empty();
        }

        ImageIO.write(bi, "png", file);
        return Optional.of(file);
    }

}
