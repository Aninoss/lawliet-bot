package core.internet;

import core.GlobalThreadPool;
import core.utils.BotUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class HttpRequest {

    private final static Logger LOGGER = LoggerFactory.getLogger(HttpRequest.class);
    private static final String USER_AGENT = String.format("Lawliet Discord Bot v%s made by Aninoss", BotUtil.getCurrentVersion());

    public static CompletableFuture<HttpResponse> getData(String urlString, HttpProperty... headers) {
        return getData(urlString, "GET", 0, null, headers);
    }

    public static CompletableFuture<HttpResponse> getData(String urlString, String body, HttpProperty... headers) {
        return getData(urlString, "POST", 0, body, headers);
    }

    public static CompletableFuture<HttpResponse> getData(String urlString, String method, String body, HttpProperty... headers) {
        return getData(urlString, method, 0, body, headers);
    }

    public static CompletableFuture<HttpResponse> getData(String urlString, int pauseTimeMilis, HttpProperty... headers) {
        return getData(urlString, "GET", pauseTimeMilis, null, headers);
    }

    public static CompletableFuture<HttpResponse> getData(String urlString, int pauseTimeMilis, String body, HttpProperty... headers) {
        return getData(urlString, "POST", pauseTimeMilis, body, headers);
    }

    public static CompletableFuture<HttpResponse> getData(String urlString, String method, int pauseTimeMilis, String body, HttpProperty... headers) {
        CompletableFuture<HttpResponse> future = new CompletableFuture<>();
        GlobalThreadPool.getExecutorService().submit(() -> download(future, urlString, method, pauseTimeMilis, body, headers));
        return future;
    }

    private static void download(CompletableFuture<HttpResponse> future, String urlString, String method, int pauseTimeMilis, String body, HttpProperty... headers) {
        try {
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Downloading from url {}", urlString);

            BufferedReader br;
            String line;
            StringBuilder text = new StringBuilder();
            HttpURLConnection connection = (HttpURLConnection) (new URL(urlString)).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            Thread.sleep(pauseTimeMilis);
            connection.setRequestMethod(method);

            connection.setRequestProperty("User-Agent", USER_AGENT);
            for (HttpProperty property : headers) {
                connection.setRequestProperty(property.getKey(), property.getValue());
            }

            boolean hasBody = body != null && body.length() > 0;
            connection.setDoOutput(hasBody);

            if (hasBody) {
                byte[] out = body.getBytes(StandardCharsets.UTF_8);
                int length = out.length;
                connection.setFixedLengthStreamingMode(length);

                connection.connect();
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(out);
                }
            } else connection.connect();

            int code = connection.getResponseCode();
            if (code / 100 != 2) {
                LOGGER.warn("Error code {} for URL {}", code, urlString);
                future.complete(new HttpResponse(code));
                return;
            }

            InputStream in = connection.getInputStream();
            br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            while ((line = br.readLine()) != null) {
                text.append(line);
            }

            br.close();
            in.close();

            future.complete(new HttpResponse(text.toString(), connection.getHeaderFields(), code));
        } catch (Throwable e) {
            future.completeExceptionally(e);
        }
    }

}
