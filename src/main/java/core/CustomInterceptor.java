package core;

import java.io.IOException;
import java.net.URI;
import constants.RegexPatterns;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.UriBuilder;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

public class CustomInterceptor implements Interceptor {

    WebTarget target;

    public CustomInterceptor() {
        Client client = ClientBuilder.newClient();
        URI endpoint = UriBuilder
                .fromUri(String.format("http://%s/api/", System.getenv("RATELIMITER_HOST")))
                .port(Integer.parseInt(System.getenv("RATELIMITER_PORT")))
                .build();

        target = client.target(endpoint);
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
            try {
                Invocation.Builder invocationBuilder = target.path("ratelimit")
                        .request()
                        .header("Authorization", System.getenv("RATELIMITER_AUTH"));

                long nextRequest = invocationBuilder.get().readEntity(Long.class);
                long sleepTimeMillis = nextRequest * 1_000 - System.currentTimeMillis();
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
