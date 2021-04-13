package core.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import com.google.common.io.Files;
import com.google.common.net.UrlEscapers;
import core.LocalFile;
import core.MainLogger;
import core.internet.InternetCache;
import net.dv8tion.jda.api.EmbedBuilder;

public final class InternetUtil {

    private InternetUtil() {
    }

    public static String getUrlFromInputStream(InputStream inputStream, String fileExt) throws ExecutionException, InterruptedException, IOException {
        LocalFile cdnFile = new LocalFile(LocalFile.Directory.CDN, String.format("temp/%d.%s", System.nanoTime(), fileExt));
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        Files.write(buffer, cdnFile);
        return cdnFile.cdnGetUrl();
    }

    public static boolean urlContainsImage(String url) {
        return url.endsWith("jpeg") || url.endsWith("jpg") || url.endsWith("png") || url.endsWith("bmp") || url.endsWith("gif");
    }

    public static boolean stringHasURL(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        return url.contains("http://") || url.contains("https://") || url.contains("www.");
    }

    public static boolean stringIsURL(String url) {
        return url != null && EmbedBuilder.URL_PATTERN.matcher(url).matches();
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
