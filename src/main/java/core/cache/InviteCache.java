package core.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import core.ShardManager;
import core.utils.FutureUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Invite;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class InviteCache {

    private static final LoadingCache<String, Optional<Invite>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build(
                    new CacheLoader<>() {
                        @Override
                        public Optional<Invite> load(@NonNull String code) throws ExecutionException {
                            JDA jda = ShardManager.getAnyJDA().get();
                            return Optional.of(Invite.resolve(jda, code).complete());
                        }
                    }
            );

    public static CompletableFuture<Invite> getInviteByCode(String code) {
        return FutureUtil.supplyAsync(() -> {
            try {
                Optional<Invite> inviteOpt = cache.get(code);
                if (inviteOpt.isPresent()) {
                    return inviteOpt.get();
                } else {
                    throw new NoSuchElementException("No such invite");
                }
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

}
