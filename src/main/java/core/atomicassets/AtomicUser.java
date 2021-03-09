package core.atomicassets;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import core.CustomObservableList;
import core.ShardManager;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.User;

public class AtomicUser implements MentionableAtomicAsset<User> {

    private final long userId;

    public AtomicUser(long userId) {
        this.userId = userId;
    }

    public AtomicUser(User user) {
        userId = user.getIdLong();
    }

    @Override
    public long getIdLong() {
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

    public static List<AtomicUser> from(List<User> users) {
        return users.stream()
                .map(AtomicUser::new)
                .collect(Collectors.toList());
    }

    public static CustomObservableList<AtomicUser> transformIdList(CustomObservableList<Long> list) {
        return list.transform(
                id -> ShardManager.getInstance().getCachedUserById(id).map(AtomicUser::new).orElse(null),
                atomic -> atomic.get().map(ISnowflake::getIdLong).orElse(null)
        );
    }

}
