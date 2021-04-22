package core.utils;

import java.util.concurrent.ExecutionException;
import core.LocalFile;
import core.MainLogger;
import net.dv8tion.jda.api.entities.Message;

public class FileUtil {

    public static boolean downloadImageAttachment(Message.Attachment messageAttachment, LocalFile localFile) {
        try {
            messageAttachment.downloadToFile(localFile).get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            MainLogger.get().error("Message attachment download exception", e);
            return false;
        }
    }

}
