package core.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import core.LocalFile;
import net.dv8tion.jda.api.entities.Message;

public class FileUtil {

    public static boolean downloadImageAttachment(Message.Attachment messageAttachment, LocalFile localFile) throws IOException, ExecutionException, InterruptedException {
        try (InputStream is = messageAttachment.retrieveInputStream().get()) {
            BufferedImage bi;
            bi = ImageIO.read(is);
            if (bi == null) {
                return false;
            }

            ImageIO.write(bi, "png", localFile);
            return true;
        }
    }

}
