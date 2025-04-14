package core.internet;

import net.dv8tion.jda.internal.utils.IOUtil;
import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

public class HttpClient {

    private static final OkHttpClient client = IOUtil.newHttpClientBuilder()
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public static OkHttpClient getClient() {
        return client;
    }

}
