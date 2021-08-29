package core;

import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.Optional;
import core.schedule.MainScheduler;
import net.dv8tion.jda.api.requests.RestAction;

public class RatelimitUpdater {

    private final long amount;
    private final TemporalUnit temporalUnit;

    private final HashMap<Long, Optional<RestAction<?>>> restActionMap = new HashMap<>();

    public RatelimitUpdater(long amount, TemporalUnit temporalUnit) {
        this.amount = amount;
        this.temporalUnit = temporalUnit;
    }

    public synchronized void update(long key, RestAction<?> restAction) {
        if (restActionMap.containsKey(key)) {
            restActionMap.put(key, Optional.of(restAction));
        } else {
            restAction.queue();
            restActionMap.put(key, Optional.empty());
            startTimer(key);
        }
    }

    private void startTimer(long key) {
        MainScheduler.schedule(amount, temporalUnit, "ratelimit_updater", () -> {
            if (restActionMap.containsKey(key)) {
                restActionMap.get(key).ifPresentOrElse(restAction -> {
                    restAction.queue();
                    restActionMap.put(key, Optional.empty());
                    startTimer(key);
                }, () -> {
                    restActionMap.remove(key);
                });
            }
        });
    }

}
