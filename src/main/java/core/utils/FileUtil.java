package core.utils;

import core.ResourceHandler;
import net.dv8tion.jda.api.entities.Message;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class FileUtil {

    public static Optional<File> downloadMessageAttachment(Message.Attachment messageAttachment, String fileName) throws IOException {
        return downloadMessageAttachment(messageAttachment, ResourceHandler.getFileResource(fileName));
    }

    public static Optional<File> downloadMessageAttachment(Message.Attachment messageAttachment, File file) throws IOException {
        BufferedImage bi;
        try {
            bi = ImageIO.read(messageAttachment.retrieveInputStream().get());
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
