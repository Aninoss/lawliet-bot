package Modules;

import Constants.Settings;
import org.javacord.api.entity.user.User;

import java.time.Instant;
import java.util.HashMap;

public class CasinoBetContainer {

    private static CasinoBetContainer ourInstance = new CasinoBetContainer();
    private HashMap<Long, Long> collectedBet = new HashMap<>();
    private HashMap<Long, Instant> lastAction = new HashMap<>();

    private CasinoBetContainer() {
        clearerStart();
    }

    public static CasinoBetContainer getInstance() {
        return ourInstance;
    }

    public long getCurrentBet(User user) {
        long userId = user.getId();
        return collectedBet.getOrDefault(userId, 0L);
    }

    public void addBet(User user, long betAdd) {
        collectedBet.put(user.getId(), getCurrentBet(user) + betAdd);
        lastAction.put(user.getId(), Instant.now());
    }

    public void removeBet(User user, long betRemove) {
        long newBet = getCurrentBet(user) - betRemove;
        if (newBet > 0) {
            collectedBet.put(user.getId(), newBet);
            lastAction.put(user.getId(), Instant.now());
        } else {
            collectedBet.remove(user.getId());
            lastAction.remove(user.getId());
        }
    }

    private void clearerStart() {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(1000 * 60);
                    for(long userId: new HashMap<>(lastAction).keySet()) {
                        if (lastAction.get(userId).plusSeconds(Settings.TIME_OUT_TIME * 2 / 1000).isBefore(Instant.now())) {
                            collectedBet.remove(userId);
                            lastAction.remove(userId);
                        }
                    }
                }
            } catch (InterruptedException e) {
                //Ignore
            }
        });
        t.setName("casino_bet_clearer");
        t.setPriority(1);
        t.start();
    }

}
