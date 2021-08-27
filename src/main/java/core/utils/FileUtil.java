package core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import com.google.common.io.Files;
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

    public static String writeInputStreamToFile(InputStream inputStream, LocalFile localFile) throws IOException {
        try (inputStream) {
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            Files.write(buffer, localFile);
            return localFile.cdnGetUrl();
        }
    }

}
