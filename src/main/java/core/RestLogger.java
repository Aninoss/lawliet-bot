package core;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.util.Pair;

public class RestLogger {

    private static final LinkedList<ActionEvent> actionEvents = new LinkedList<>();

    public static synchronized void insert(Long guildId) {
        cleanOutDatedActions();
        actionEvents.add(new ActionEvent(guildId));
    }

    public static synchronized int count() {
        cleanOutDatedActions();
        return actionEvents.size();
    }

    public static synchronized List<Pair<Long, Integer>> countGuilds(int limit) {
        cleanOutDatedActions();
        HashMap<Long, Integer> guildCounter = new HashMap<>();

        actionEvents.stream()
                .filter(a -> a.guildId != null)
                .forEach(a -> {
                    int n = guildCounter.getOrDefault(a.guildId, 0);
                    guildCounter.put(a.guildId, n + 1);
                });

        return guildCounter.entrySet().stream()
                .map(set -> new Pair<>(set.getKey(), set.getValue()))
                .sorted((p1, p2) -> Integer.compare(p2.getValue(), p1.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private static void cleanOutDatedActions() {
        while(actionEvents.size() > 0) {
            Instant actionEventInstant = actionEvents.getFirst().instant;
            if (actionEventInstant.isBefore(Instant.now().minus(Duration.ofHours(1)))) {
                actionEvents.removeFirst();
            } else {
                break;
            }
        }
    }


    private static class ActionEvent {

        private final Instant instant;
        private final Long guildId;

        public ActionEvent(Long guildId) {
            this.instant = Instant.now();
            this.guildId = guildId;
        }

    }

}
