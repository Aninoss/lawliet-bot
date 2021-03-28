package core.utils;

import java.io.File;
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
import core.internet.InternetCache;

public final class InternetUtil {

    private InternetUtil() {
    }

    public static CompletableFuture<String> getURLFromFile(File file) throws ExecutionException, InterruptedException {
        return getURLFromFile(file, 0);
    }

    //TODO: switching to cdn
    public static CompletableFuture<String> getURLFromFile(File file, int deleteAfterSeconds) throws ExecutionException, InterruptedException {
        return JDAUtil.sendPrivateFile(AssetIds.CACHE_USER_ID, file)
                .submit()
                .exceptionally(ExceptionLogger.get())
                .thenApply(m -> {
                    String url = m.getAttachments().get(0).getUrl();
                    if (deleteAfterSeconds > 0) {
                        m.delete().queueAfter(deleteAfterSeconds, TimeUnit.SECONDS);
                    }
                    return url;
                });
    }

    public static CompletableFuture<String> getURLFromInputStream(InputStream inputStream, String filename) throws ExecutionException, InterruptedException {
        return getURLFromInputStream(inputStream, filename, 0);
    }

    //TODO: switching to cdn
    public static CompletableFuture<String> getURLFromInputStream(InputStream inputStream, String filename, int deleteAfterSeconds) throws ExecutionException, InterruptedException {
        return JDAUtil.sendPrivateFile(AssetIds.CACHE_USER_ID, inputStream, filename)
                .submit()
                .exceptionally(ExceptionLogger.get())
                .thenApply(m -> {
                    String url = m.getAttachments().get(0).getUrl();
                    if (deleteAfterSeconds > 0) {
                        m.delete().queueAfter(deleteAfterSeconds, TimeUnit.SECONDS);
                    }
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

    public static CompletableFuture<String> retrieveThumbnailPreview(String url) {
        return InternetCache.getData(url, 60 * 60)
                .thenApply(data -> {
                    String content = data.getContent().get();
                    return StringUtil.extractGroups(content, "<meta property=\"og:image\" content=\"", "\"")[0];
                });
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
