package General.Internet;

import org.json.JSONObject;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Internet {
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.84 Safari/537.36";

    public static String getData(String urlString) throws Throwable {
        return getDataCookie(urlString, null);
    }

    public static String getDataRerquestPropertyGet(String urlString, String propertyName, String propertyValue) throws Throwable {
        BufferedReader br;
        String line;
        StringBuilder text = new StringBuilder();
        HttpsURLConnection connection = (HttpsURLConnection) (new URL(urlString)).openConnection();
        connection.setRequestProperty("User-Agent", USER_AGENT);
        if (propertyValue != null && propertyName != null) connection.setRequestProperty(propertyName, propertyValue);

        InputStream in = connection.getInputStream();
        br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        while ((line = br.readLine()) != null) {
            text.append(line);
        }

        return text.toString();
    }

    public static String getDataRequestPropertyPost(String urlString, String propertyName, String propertyValue, String query) throws Throwable {
        BufferedReader br;
        String line;
        StringBuilder text = new StringBuilder();
        HttpURLConnection connection = (HttpURLConnection) (new URL(urlString)).openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty(propertyName, propertyValue);
        connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        if (query != null) {
            connection.setRequestProperty("Content-Length", Integer.toString(query.length()));
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(query);
            wr.flush();
            wr.close();
        }

        if (connection.getResponseCode() == 404) return null;

        InputStream in = connection.getInputStream();
        br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        while ((line = br.readLine()) != null) {
            text.append(line);
        }

        return text.toString();
    }

    public static String getDataCookie(String urlString, String cookie) throws Throwable {
        return getDataRerquestPropertyGet(urlString, "Cookie", cookie);
    }

    public static String getData(String urlString, int waitingTime) throws Throwable {
        BufferedReader br;
        String line;
        StringBuilder text = new StringBuilder();
        HttpsURLConnection connection = (HttpsURLConnection) (new URL(urlString)).openConnection();
        Thread.sleep(waitingTime);
        connection.setRequestProperty("User-Agent", USER_AGENT);

        if (connection.getResponseCode() == 404) return null;

        InputStream in = connection.getInputStream();
        br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

        while ((line = br.readLine()) != null) {
            text.append(line);
        }

        return text.toString();
    }

    public static InternetResponse getDataPostWithCookie(String urlString, String body) throws Throwable {
        BufferedReader br;
        String line;
        StringBuilder text = new StringBuilder();
        HttpsURLConnection connection = (HttpsURLConnection) (new URL(urlString)).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setDoOutput(true);

        byte[] out = body.getBytes(StandardCharsets.UTF_8);
        int length = out.length;

        connection.setFixedLengthStreamingMode(length);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        connection.connect();
        try(OutputStream os = connection.getOutputStream()) {
            os.write(out);
        }

        if (connection.getResponseCode() == 404) return null;

        InputStream in = connection.getInputStream();
        br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        while ((line = br.readLine()) != null) {
            text.append(line);
        }

        String cookie = connection.getHeaderField("Set-Cookie").split(";")[0];

        return new InternetResponse(text.toString(), cookie);
    }

    public static InternetResponse getDataRaw(String domain, int port, String urlString, String method, String requestHead, String requestBody) throws Throwable {
        String tempUrl = urlString.replaceFirst("//", "");
        tempUrl = tempUrl.substring(tempUrl.split("/")[0].length());

        String request = method + " " + urlString + " HTTP/1.1\r\n"
                + requestHead + "\r\n";

        if (requestBody != null && requestBody.length() > 0) request += "\r\n" + requestBody + "\r\n";

        Socket socket = new Socket(domain, port);
        byte[] out = request.getBytes(StandardCharsets.UTF_8);
        try(OutputStream os = socket.getOutputStream()) {
            os.write(out);
        }

        InputStream in = socket.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        StringBuilder text = new StringBuilder();
        while ((line = br.readLine()) != null) {
            text.append(line);
        }

        String[] parts = text.toString().split("\r\n\r\n");

        return new InternetResponse(parts[0], parts[1]);
    }

    public static JSONObject getJSONObject(String url) throws Throwable {
        return new JSONObject(Internet.getData(url));
    }

    public static boolean stringIsURL(String str) {
        if (str.contains("http://") || str.contains("https://")) return true;

        String [] parts = str.split("\\s+");

        // Attempt to convert each item into an URL.
        for( String item : parts ) try {
            URL url = new URL(item);
            return true;
        } catch (MalformedURLException e) {
            //Ignore
        }

        return false;
    }
}
