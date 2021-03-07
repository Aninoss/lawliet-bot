package core.atomicassets;

import core.ShardManager;
import net.dv8tion.jda.api.entities.User;
import java.util.Objects;
import java.util.Optional;

public class AtomicUser implements MentionableAtomicAsset<User> {

    private final long userId;

    public AtomicUser(long userId) {
        this.userId = userId;
    }

    public AtomicUser(User user) {
        userId = user.getIdLong();
    }

    @Override
    public long getId() {
        return userId;
    }

    @Override
    public Optional<User> get() {
        return ShardManager.getInstance().getCachedUserById(userId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtomicUser that = (AtomicUser) o;
        return userId == that.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

}
