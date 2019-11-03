package General.Internet;

import General.Pair;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class Internet {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.84 Safari/537.36";

    public static InternetResponse getData(String urlString, Pair... properties) throws IOException {
        return getData(urlString, "GET", 0, null, properties);
    }

    public static InternetResponse getData(String urlString, String body, Pair... properties) throws IOException {
        return getData(urlString, "POST", 0, body, properties);
    }

    public static InternetResponse getData(String urlString, String method, String body, Pair... properties) throws IOException {
        return getData(urlString, method, 0, body, properties);
    }

    public static InternetResponse getData(String urlString, int pauseTimeMilis, Pair... properties) throws IOException {
        return getData(urlString, "GET", pauseTimeMilis, null, properties);
    }

    public static InternetResponse getData(String urlString, int pauseTimeMilis, String body, Pair... properties) throws IOException {
        return getData(urlString, "POST", pauseTimeMilis, body, properties);
    }

    public static InternetResponse getData(String urlString, String method, int pauseTimeMilis, String body, Pair... properties) throws IOException {
        BufferedReader br;
        String line;
        StringBuilder text = new StringBuilder();
        HttpsURLConnection connection = (HttpsURLConnection) (new URL(urlString)).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        try {
            Thread.sleep(pauseTimeMilis);
        } catch (InterruptedException e) {
            return new InternetResponse(-1);
        }
        connection.setRequestMethod(method);

        connection.setRequestProperty("User-Agent", USER_AGENT);
        for(Pair property: properties) {
            connection.setRequestProperty(property.getKey().toString(), property.getValue().toString());
        }

        boolean hasBody = body != null && body.length() > 0;
        connection.setDoOutput(hasBody);

        if (hasBody) {
            byte[] out = body.getBytes(StandardCharsets.UTF_8);
            int length = out.length;
            connection.setFixedLengthStreamingMode(length);

            connection.connect();
            try(OutputStream os = connection.getOutputStream()) {
                os.write(out);
            }
        } else connection.connect();

        int code = connection.getResponseCode();
        if (code != 200) return new InternetResponse(code);

        InputStream in = connection.getInputStream();
        br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        while ((line = br.readLine()) != null) {
            text.append(line);
        }

        Map<String, List<String>> s = connection.getHeaderFields();

        return new InternetResponse(text.toString(), connection.getHeaderFields(), code);
    }

    public static boolean stringIsURL(String str) {
        if (str.contains("http://") || str.contains("https://")) return true;

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
