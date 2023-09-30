package core.utils;

import com.google.common.net.UrlEscapers;
import core.LocalFile;
import core.MainLogger;
import core.internet.HttpCache;
import net.dv8tion.jda.api.EmbedBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class InternetUtil {

    private InternetUtil() {
    }

    public static void downloadFile(String url, File file) throws IOException {
        try(InputStream inputStream = new URL(url).openStream()) {
            Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static String getUrlFromInputStream(InputStream inputStream, String fileExt) throws IOException {
        LocalFile cdnFile = new LocalFile(LocalFile.Directory.CDN, String.format("temp/%s.%s", RandomUtil.generateRandomString(30), fileExt));
        return FileUtil.writeInputStreamToFile(inputStream, cdnFile);
    }

    public static boolean uriIsImage(String url) {
        int index = url.indexOf('?');
        if (index != -1) {
            url = url.substring(0, index);
        }
        return url.endsWith(".jpeg") || url.endsWith(".jpg") || url.endsWith(".png") || url.endsWith(".bmp") || url.endsWith(".gif");
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
        return HttpCache.get(url, Duration.ofDays(1))
                .thenApply(data -> {
                    String content = data.getBody();
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
