package General.Fishing;

import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.util.ArrayList;

public class FishingProfile {
    private Server server;
    private User user;
    private long fish, coins;
    private ArrayList<FishingSlot> slots;

    public FishingProfile(Server server, User user) {
        this.server = server;
        this.user = user;
        this.fish = 0;
        this.coins = 0;
        this.slots = new ArrayList<>();
    }

    public Server getServer() {
        return server;
    }

    public User getUser() {
        return user;
    }

    public long getFish() {
        return fish;
    }

    public long getCoins() {
        return coins;
    }

    public void insert(FishingSlot fishingSlot) {
        slots.add(fishingSlot);
    }

    public long getEffect(int id) {
        return find(id).getEffect();
    }

    public FishingSlot find(int id) {
        for(FishingSlot slot: slots) {
            if (slot.getId() == id) return slot;
        }
        return null;
    }

    public ArrayList<FishingSlot> getSlots() {
        return slots;
    }

    public void setFish(long fish) {
        this.fish = fish;
    }

    public void setCoins(long coins) {
        this.coins = coins;
    }
}
