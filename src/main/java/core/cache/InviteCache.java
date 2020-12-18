package core.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import core.DiscordApiManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.server.invite.Invite;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class InviteCache {

    private static final InviteCache ourInstance = new InviteCache();
    public static InviteCache getInstance() { return ourInstance; }
    private InviteCache() { }

    private final LoadingCache<String, Optional<Invite>> cache = CacheBuilder.newBuilder()
            .build(
                    new CacheLoader<>() {
                        @Override
                        public Optional<Invite> load(@NonNull String code) {
                            DiscordApi api = DiscordApiManager.getInstance().getAnyApi().get();
                            try {
                                return Optional.of(api.getInviteByCode(code).get());
                            } catch (InterruptedException | ExecutionException e) {
                                return Optional.empty(); //Ignore
                            }
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
