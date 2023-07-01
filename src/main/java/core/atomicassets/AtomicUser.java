package core.atomicassets;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import core.CustomObservableList;
import core.ShardManager;
import core.TextManager;
import core.utils.StringUtil;
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
        return ShardManager.getCachedUserById(userId);
    }

    @Override
    public Optional<String> getPrefixedNameRaw() {
        return get().map(u -> "@" + u.getName());
    }

    @Override
    public Optional<String> getNameRaw() {
        return get().map(User::getName);
    }

    public Optional<String> getTaggedNameRaw() {
        return get().map(User::getAsTag);
    }

    public String getTaggedName(Locale locale) {
        return getTaggedNameRaw()
                .orElseGet(() -> TextManager.getString(locale, TextManager.GENERAL, "notfound", StringUtil.numToHex(getIdLong())));
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

    public static List<User> to(List<AtomicUser> channels) {
        return channels.stream()
                .map(AtomicUser::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static CustomObservableList<AtomicUser> transformIdList(CustomObservableList<Long> list) {
        return list.transform(
                AtomicUser::new,
                AtomicUser::getIdLong
        );
    }

    public static AtomicUser fromOutsideCache(long userId) {
        AtomicUser atomicUser;
        try {
            User user = ShardManager.fetchUserById(userId).get();
            atomicUser = new AtomicUser(user);
        } catch (InterruptedException | ExecutionException e) {
            // ignore
            atomicUser = new AtomicUser(userId);
        }
        return atomicUser;
    }

}
