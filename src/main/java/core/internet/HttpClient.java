package core.internet;

import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.internal.utils.IOUtil;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpClient {

    private static final HttpClient ourInstance = new HttpClient();

    public static HttpClient getInstance() {
        return ourInstance;
    }

    private final OkHttpClient client = IOUtil.newHttpClientBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .cache(null)
            .build();

    private HttpClient() {
    }

    public Call newCall(Request request) {
        return client.newCall(request);
    }

}
