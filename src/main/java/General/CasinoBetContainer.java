package General;

import org.javacord.api.entity.user.User;

import java.util.HashMap;

public class CasinoBetContainer {

    private static CasinoBetContainer ourInstance = new CasinoBetContainer();
    private HashMap<Long, Long> collectedBet = new HashMap<>();

    private CasinoBetContainer() {}

    public static CasinoBetContainer getInstance() {
        return ourInstance;
    }

    public long getCurrentBet(User user) {
        long userId = user.getId();
        return collectedBet.getOrDefault(userId, 0L);
    }

    public void addBet(User user, long betAdd) {
        collectedBet.put(user.getId(), getCurrentBet(user) + betAdd);
    }

    public void removeBet(User user, long betRemove) {
        long newBet = getCurrentBet(user) - betRemove;
        if (newBet > 0) {
            collectedBet.put(user.getId(), newBet);
        } else {
            collectedBet.remove(user.getId());
        }
    }

}
