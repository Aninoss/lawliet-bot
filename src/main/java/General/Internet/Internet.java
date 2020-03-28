package General.Internet;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Internet {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.84 Safari/537.36";

    public static CompletableFuture<InternetResponse> getData(String urlString, InternetProperty... properties) throws IOException {
        return getData(urlString, "GET", 0, null, properties);
    }

    public static CompletableFuture<InternetResponse> getData(String urlString, String body, InternetProperty... properties) throws IOException {
        return getData(urlString, "POST", 0, body, properties);
    }

    public static CompletableFuture<InternetResponse> getData(String urlString, String method, String body, InternetProperty... properties) throws IOException {
        return getData(urlString, method, 0, body, properties);
    }

    public static CompletableFuture<InternetResponse> getData(String urlString, int pauseTimeMilis, InternetProperty... properties) throws IOException {
        return getData(urlString, "GET", pauseTimeMilis, null, properties);
    }

    public static CompletableFuture<InternetResponse> getData(String urlString, int pauseTimeMilis, String body, InternetProperty... properties) throws IOException {
        return getData(urlString, "POST", pauseTimeMilis, body, properties);
    }

    public static CompletableFuture<InternetResponse> getData(String urlString, String method, int pauseTimeMilis, String body, InternetProperty... properties) throws IOException {
        CompletableFuture<InternetResponse> future = new CompletableFuture<>();
        Thread t = new Thread(() -> download(future, urlString, method, pauseTimeMilis, body, properties));
        t.setName("download_url");
        t.start();
        return future;
    }

    private static void download(CompletableFuture<InternetResponse> future, String urlString, String method, int pauseTimeMilis, String body, InternetProperty... properties) {
        try {
            BufferedReader br;
            String line;
            StringBuilder text = new StringBuilder();
            HttpsURLConnection connection = (HttpsURLConnection) (new URL(urlString)).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            Thread.sleep(pauseTimeMilis);
            connection.setRequestMethod(method);

            connection.setRequestProperty("User-Agent", USER_AGENT);
            for (InternetProperty property : properties) {
                connection.setRequestProperty(property.getKey().toString(), property.getValue().toString());
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
            if (code != 200) {
                future.complete(new InternetResponse(code));
                return;
            }

            InputStream in = connection.getInputStream();
            br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            while ((line = br.readLine()) != null) {
                text.append(line);
            }

            Map<String, List<String>> s = connection.getHeaderFields();

            future.complete(new InternetResponse(text.toString(), connection.getHeaderFields(), code));
        } catch (Throwable e) {
            future.completeExceptionally(e);
        }
    }

    public static boolean stringIsURL(String str) {
        if (str.contains("http://") || str.contains("https://") || str.contains("www")) return true;

        String [] parts = str.split("\\s+");

        for( String item : parts ) try {
            new URL(item);
            return true;
        } catch (MalformedURLException e) {
            //Ignore
        }

        return false;
    }
}
