package core;

import constants.RegexPatterns;
import core.internet.HttpResponse;
import core.restclient.RestClient;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class CustomInterceptor implements Interceptor {

    @Override
    public @NotNull Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        request = request.newBuilder()
                .url(request.url().url().toString().replace("https://discord.com", "https://" + DiscordDomain.get()))
                .build();

        if (RegexPatterns.INTERACTION.matcher(request.url().encodedPath()).matches()) {
            Request newRequest = request.newBuilder().removeHeader("authorization").build();
            return chain.proceed(newRequest);
        }

        try {
            requestQuota();
        } catch (InterruptedException e) {
            MainLogger.get().error("Interrupted", e);
        }

        return chain.proceed(request);
    }

    private synchronized void requestQuota() throws InterruptedException {
        if (Program.productionMode() && Program.publicInstance()) {
            try {
                HttpResponse httpResponse = RestClient.RATELIMITER.get("relative").get();
                int sleepTimeMillis = Integer.parseInt(httpResponse.getBody());
                if (sleepTimeMillis > 0) {
                    Thread.sleep(sleepTimeMillis);
                }
            } catch (InterruptedException e) {
                throw e;
            } catch (Throwable e) {
                MainLogger.get().error("Ratelimit exception", e);
                Thread.sleep(1000);
                requestQuota();
            }
        }
    }

}
