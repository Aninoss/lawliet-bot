package core.utils;

import com.google.common.net.UrlEscapers;
import core.DiscordApiCollection;
import core.schedule.MainScheduler;
import org.javacord.api.entity.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;

public final class InternetUtil {

    private InternetUtil() {}

    private final static Logger LOGGER = LoggerFactory.getLogger(InternetUtil.class);

    public static URL getURLFromInputStream(InputStream inputStream) throws ExecutionException, InterruptedException {
        Message message = DiscordApiCollection.getInstance().getHomeServer().getTextChannelById(521088289894039562L).get().sendMessage(inputStream, "welcome.png").get();
        URL url = message.getAttachments().get(0).getUrl();

        MainScheduler.getInstance().schedule(10, ChronoUnit.SECONDS, message::delete);
        return url;
    }

    public static boolean urlContainsImage(String url) {
        return url.endsWith("jpeg") || url.endsWith("jpg") || url.endsWith("png") || url.endsWith("bmp") || url.endsWith("gif");
    }

    public static boolean stringHasURL(String str) {
        if (str.contains("http://") || str.contains("https://") || str.contains("www.")) return true;

        String [] parts = str.split("\\s+");

        for( String item : parts ) try {
            new URL(item);
            return true;
        } catch (MalformedURLException e) {
            //Ignore
        }

        return false;
    }

    public static boolean checkConnection() {
        try {
            URL url = new URL("https://www.google.com/");
            URLConnection connection = url.openConnection();
            connection.connect();

            return true;
        } catch (Exception e) {
            LOGGER.error("Could not create connection to google", e);
        }
        return false;
    }

    public static String escapeForURL(String url) {
        return UrlEscapers.urlFragmentEscaper().escape(url);
    }

}
