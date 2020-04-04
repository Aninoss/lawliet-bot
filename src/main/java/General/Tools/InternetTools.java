package General.Tools;

import General.DiscordApiCollection;
import org.javacord.api.entity.message.Message;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class InternetTools {

    public static URL getURLFromInputStream(InputStream inputStream) throws ExecutionException, InterruptedException {
        Message message = DiscordApiCollection.getInstance().getHomeServer().getTextChannelById(521088289894039562L).get().sendMessage(inputStream, "welcome.png").get();
        URL url = message.getAttachments().get(0).getUrl();
        Thread t =new Thread(() -> {
            try {
                Thread.sleep(10000);
                message.delete();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t.setName("message_delete_counter");
        t.start();
        return url;
    }

    public static boolean urlContainsImage(String url) {
        return url.endsWith("jpeg") || url.endsWith("jpg") || url.endsWith("png") || url.endsWith("bmp") || url.endsWith("gif");
    }

}
