package Core.Tools;

import Core.CustomThread;
import Core.DiscordApiCollection;
import org.javacord.api.entity.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class InternetTools {

    final static Logger LOGGER = LoggerFactory.getLogger(InternetTools.class);

    public static URL getURLFromInputStream(InputStream inputStream) throws ExecutionException, InterruptedException {
        Message message = DiscordApiCollection.getInstance().getHomeServer().getTextChannelById(521088289894039562L).get().sendMessage(inputStream, "welcome.png").get();
        URL url = message.getAttachments().get(0).getUrl();
        Thread t =new CustomThread(() -> {
            try {
                Thread.sleep(10000);
                message.delete();
            } catch (InterruptedException e) {
                LOGGER.error("Could not get url from input stream", e);
            }
        }, "message_delete_counter", 1);
        t.start();
        return url;
    }

    public static boolean urlContainsImage(String url) {
        return url.endsWith("jpeg") || url.endsWith("jpg") || url.endsWith("png") || url.endsWith("bmp") || url.endsWith("gif");
    }

}
