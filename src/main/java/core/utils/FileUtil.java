package core.utils;

import com.google.common.io.Files;
import core.LocalFile;
import core.MainLogger;
import net.dv8tion.jda.api.entities.Message;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

public class FileUtil {

    public static boolean downloadImageAttachment(Message.Attachment messageAttachment, LocalFile localFile) {
        if (!InternetUtil.uriIsImage(messageAttachment.getUrl())) {
            return false;
        }

        deleteLocalFile(localFile);
        try {
            messageAttachment.downloadToFile(localFile).get();
            return true;
        } catch (InterruptedException | ExecutionException e) {
            MainLogger.get().error("Message attachment download exception", e);
            return false;
        }
    }

    public static String writeInputStreamToFile(InputStream inputStream, LocalFile localFile) throws IOException {
        deleteLocalFile(localFile);
        try (inputStream) {
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            Files.write(buffer, localFile);
            return localFile.cdnGetUrl();
        }
    }

    private static void deleteLocalFile(LocalFile localFile) {
        if (localFile.exists() && !localFile.delete()) {
            MainLogger.get().error("Could not remove file {}", localFile.getAbsoluteFile());
        }
    }

    public static String getUriExt(String uri) {
        return uri.substring(uri.lastIndexOf("."));
    }

}
