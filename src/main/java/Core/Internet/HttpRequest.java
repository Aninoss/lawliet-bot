package Core.Internet;

import Core.CustomThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class HttpRequest {

    final static Logger LOGGER = LoggerFactory.getLogger(HttpRequest.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:74.0) Gecko/20100101 Firefox/74.0";

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
        Thread t = new CustomThread(() -> download(future, urlString, method, pauseTimeMilis, body, headers), "download_url");
        t.start();
        return future;
    }

    private static void download(CompletableFuture<HttpResponse> future, String urlString, String method, int pauseTimeMilis, String body, HttpProperty... headers) {
        try {
            if (LOGGER.isDebugEnabled()) LOGGER.debug("Downloading from url {}", urlString);

            BufferedReader br;
            String line;
            StringBuilder text = new StringBuilder();
            HttpsURLConnection connection = (HttpsURLConnection) (new URL(urlString)).openConnection();
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
