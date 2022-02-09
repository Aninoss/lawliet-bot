package core.internet;

import java.util.concurrent.TimeUnit;
import core.CustomInterceptor;
import net.dv8tion.jda.internal.utils.IOUtil;
import okhttp3.OkHttpClient;

public class HttpClient {

    private static final OkHttpClient client = IOUtil.newHttpClientBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(new CustomInterceptor())
            .build();

    public static OkHttpClient getClient() {
        return client;
    }

}
