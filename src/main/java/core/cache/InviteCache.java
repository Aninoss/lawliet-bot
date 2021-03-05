package core.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import core.DiscordApiManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Invite;
import org.checkerframework.checker.nullness.qual.NonNull;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class InviteCache {

    private static final InviteCache ourInstance = new InviteCache();
    public static InviteCache getInstance() { return ourInstance; }
    private InviteCache() { }

    private final LoadingCache<String, Optional<Invite>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build(
                    new CacheLoader<>() {
                        @Override
                        public Optional<Invite> load(@NonNull String code) throws ExecutionException {
                            JDA jda = DiscordApiManager.getInstance().getAnyJDA().get();
                            return Optional.of(Invite.resolve(jda, code).complete());
                        }
                    }
            );

    public Optional<Invite> getInviteByCode(String code) {
        try {
            return cache.get(code);
        } catch (ExecutionException e) {
            return Optional.empty();
        }
    }

}
