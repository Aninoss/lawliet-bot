package core.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import core.LocalFile;
import net.dv8tion.jda.api.entities.Message;

public class FileUtil {

    public static boolean downloadImageAttachment(Message.Attachment messageAttachment, LocalFile localFile) throws IOException, ExecutionException, InterruptedException {
        BufferedImage bi;
        bi = ImageIO.read(messageAttachment.retrieveInputStream().get());
        if (bi == null) {
            return false;
        }

        ImageIO.write(bi, "png", localFile);
        return true;
    }

}
