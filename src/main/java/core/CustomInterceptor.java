package core;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import constants.RegexPatterns;
import core.utils.ExceptionUtil;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import websockets.syncserver.SendEvent;
import websockets.syncserver.SyncManager;

public class CustomInterceptor implements Interceptor {

    private long nextRequest = 0;

    @Override
    public @NotNull Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (RegexPatterns.INTERACTION.matcher(request.url().encodedPath()).matches()) {
            Request newRequest = request.newBuilder().removeHeader("authorization").build();
            return chain.proceed(newRequest);
        }

        try (AsyncTimer asyncTimer = new AsyncTimer(Duration.ofSeconds(5))) {
            asyncTimer.setTimeOutListener(t -> {
                t.interrupt();
                MainLogger.get().error("Rest API stuck: {}", request.method() + " " + request.url(), ExceptionUtil.generateForStack(t));
                SyncManager.getInstance().reconnect();
            });
            requestQuota();
        } catch (Exception e) {
            MainLogger.get().error("Interrupted", e);
        }

        return chain.proceed(request);
    }

    private synchronized void requestQuota() throws InterruptedException {
        if (System.nanoTime() < nextRequest) {
            long sleepTime;
            while ((sleepTime = calculateLocalSleepTime()) > 0) { // Sleep is unreliable, so we have to loop
                Thread.sleep(sleepTime / 1_000_000, (int) (sleepTime % 1_000_000));
            }
        }

        if (Program.isProductionMode()) {
            while (true) {
                try {
                    long syncedSleepTime = SendEvent.sendRequestSyncedRatelimit().get();
                    Thread.sleep(syncedSleepTime / 1_000_000, (int) (syncedSleepTime % 1_000_000));
                    break;
                } catch (ExecutionException e) {
                    MainLogger.get().error("Error when requesting synced waiting time", e);
                    Thread.sleep(5000);
                }
            }
        }

        nextRequest = System.nanoTime() + 21_000_000L;
    }

    private long calculateLocalSleepTime() {
        return (nextRequest - System.nanoTime());
    }

}
