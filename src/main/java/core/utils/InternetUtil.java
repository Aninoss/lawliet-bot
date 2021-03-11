package core.utils;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import com.google.common.net.UrlEscapers;
import constants.AssetIds;
import core.ExceptionLogger;
import core.MainLogger;

public final class InternetUtil {

    private InternetUtil() {
    }

    //TODO: switching to cdn
    public static CompletableFuture<String> getURLFromInputStream(InputStream inputStream, String filename) throws ExecutionException, InterruptedException {
        return JDAUtil.sendPrivateFile(AssetIds.CACHE_USER_ID, inputStream, filename)
                .submit()
                .exceptionally(ExceptionLogger.get())
                .thenApply(m -> {
                    String url = m.getAttachments().get(0).getUrl();
                    m.delete().queueAfter(10, TimeUnit.SECONDS);
                    return url;
                });
    }

    public static boolean urlContainsImage(String url) {
        return url.endsWith("jpeg") || url.endsWith("jpg") || url.endsWith("png") || url.endsWith("bmp") || url.endsWith("gif");
    }

    public static boolean stringHasURL(String str, boolean strict) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        if (!strict &&
                (str.contains("http://") || str.contains("https://") || str.contains("www."))
        ) {
            return true;
        }

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
