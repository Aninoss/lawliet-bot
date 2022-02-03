package core.internet;

import java.util.concurrent.TimeUnit;
import core.CustomInterceptor;
import net.dv8tion.jda.internal.utils.IOUtil;
import okhttp3.*;

public class HttpClient {

    private static final ConnectionPool connectionPool = new ConnectionPool(5, 10, TimeUnit.SECONDS);
    private static final Dispatcher dispatcher = new Dispatcher();
    private static final OkHttpClient client;

    static {
        dispatcher.setMaxRequestsPerHost(25);
        client = IOUtil.newHttpClientBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(new CustomInterceptor())
                .connectionPool(connectionPool)
                .dispatcher(dispatcher)
                .cache(null)
                .build();
    }

    public static OkHttpClient getClient() {
        return client;
    }

}
