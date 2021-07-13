package core.internet;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import core.MainLogger;
import core.utils.BotUtil;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

public class HttpRequest {

    private static final String USER_AGENT = String.format("Lawliet Discord Bot v%s made by Aninoss", BotUtil.getCurrentVersion());
    private static final OkHttpClient client = new OkHttpClient();

    public static CompletableFuture<HttpResponse> getData(String url, HttpHeader... headers) {
        return getData(url, null, null, headers);
    }

    public static CompletableFuture<HttpResponse> getData(String url, String mediaType, String body, HttpHeader... headers) {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT);
        if (body != null) {
            requestBuilder.method("POST", RequestBody.create(MediaType.get(mediaType), body));
        }
        for (HttpHeader header : headers) {
            requestBuilder.addHeader(header.getKey(), header.getValue());
        }
        Request request = requestBuilder.build();

        CompletableFuture<HttpResponse> future = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try(ResponseBody body = response.body()) {
                    int code = response.code();
                    HttpResponse httpResponse = new HttpResponse()
                            .setCode(code)
                            .setBody(body.string());
                    future.complete(httpResponse);

                    if (code / 100 != 2) {
                        MainLogger.get().warn("Error code {} for URL {}", code, url);
                    }
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

}
