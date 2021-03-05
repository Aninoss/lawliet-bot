package core.utils;

import com.google.common.net.UrlEscapers;
import core.DiscordApiManager;
import core.MainLogger;
import net.dv8tion.jda.api.entities.Message;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public final class InternetUtil {

    private InternetUtil() {
    }

    public static String getURLFromInputStream(InputStream inputStream) throws ExecutionException, InterruptedException {
        Message message = JDAUtil.sendPrivateFile(DiscordApiManager.getInstance().fetchCacheUser().get(), inputStream, "welcome.png")
                .complete();

        String url = message.getAttachments().get(0).getUrl();
        message.delete().queueAfter(10, TimeUnit.SECONDS);
        return url;
    }

    public static boolean urlContainsImage(String url) {
        return url.endsWith("jpeg") || url.endsWith("jpg") || url.endsWith("png") || url.endsWith("bmp") || url.endsWith("gif");
    }

    public static boolean stringHasURL(String str, boolean strict) {
        if (str == null || str.isEmpty())
            return false;

        if (!strict &&
                (str.contains("http://") || str.contains("https://") || str.contains("www."))
        ) return true;

        String[] parts = str.split("\\s+");

        for (String item : parts) {
            try {
                new URL(item);
                return true;
            } catch (MalformedURLException e) {
                //Ignore
            }
        }

        return false;
    }

    public static boolean checkConnection() {
        try {
            URL url = new URL("https://www.google.com/");
            URLConnection connection = url.openConnection();
            connection.connect();

            return true;
        } catch (Throwable e) {
            MainLogger.get().error("Could not create connection to google", e);
        }
        return false;
    }

    public static String escapeForURL(String url) {
        return UrlEscapers.urlFragmentEscaper().escape(url);
    }

}
