package core.internet;

import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.internal.utils.IOUtil;
import okhttp3.*;

public class HttpClient {

    private final OkHttpClient client = IOUtil.newHttpClientBuilder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .cache(null)
            .build();

    public Call newCall(Request request) {
        return client.newCall(request);
    }

}
