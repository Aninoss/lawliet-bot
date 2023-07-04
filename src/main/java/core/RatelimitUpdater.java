package core;

import core.schedule.MainScheduler;
import net.dv8tion.jda.api.requests.RestAction;

import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;

public class RatelimitUpdater {

    private final Duration duration;

    private final HashMap<Long, Optional<RestAction<?>>> restActionMap = new HashMap<>();

    public RatelimitUpdater(Duration duration) {
        this.duration = duration;
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
        MainScheduler.schedule(duration, () -> {
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
