package core;

import core.schedule.MainScheduler;

import java.time.Duration;
import java.time.Instant;

import static java.util.Objects.requireNonNullElse;

public class HeartbeatReceiver {

    private static boolean started = false;
    private static Instant lastBeat;

    public static synchronized void start() {
        if (started) {
            return;
        }
        started = true;

        int minutes = Integer.parseInt(requireNonNullElse(System.getenv("HEARTBEAT_LIMIT_MINUTES"), "30"));
        registerHeartbeat();

        MainScheduler.poll(Duration.ofMinutes(1), () -> {
            try {
                if (Instant.now().isAfter(lastBeat.plus(Duration.ofMinutes(1)))) {
                    MainLogger.get().warn("Heartbeat not received for over {} minutes", Duration.between(lastBeat, Instant.now()).toMinutes());
                }

                if (Instant.now().isAfter(lastBeat.plus(Duration.ofMinutes(minutes)))) {
                    MainLogger.get().error("EXIT - No heartbeat received");
                    System.exit(9);
                    return false;
                }
            } catch (Throwable e) {
                MainLogger.get().error("Error while polling heartbeat", e);
            }
            return true;
        });
    }

    public static void registerHeartbeat() {
        lastBeat = Instant.now();
    }

}
