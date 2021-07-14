package core;

import java.io.IOException;
import constants.RegexPatterns;
import core.restclient.RestClient;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

public class CustomInterceptor implements Interceptor {

    private final RestClient restClient;

    public CustomInterceptor() {
        restClient = RestClient.RATELIMITER;
    }

    @Override
    public @NotNull Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
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
        if (Program.isProductionMode()) {
            Invocation.Builder invocationBuilder = restClient.request("relative", MediaType.TEXT_PLAIN);
            try (jakarta.ws.rs.core.Response response = invocationBuilder.get()) {
                int sleepTimeMillis = response.readEntity(Integer.class);
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
