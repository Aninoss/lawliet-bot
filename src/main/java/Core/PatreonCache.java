package Core;

import Constants.Settings;
import Core.Utils.BotUtil;
import MySQL.Modules.Donators.DBDonators;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class PatreonCache {

    private static final PatreonCache ourInstance = new PatreonCache();
    private PatreonCache() {}
    public static PatreonCache getInstance() {
        return ourInstance;
    }

    private final LoadingCache<Long, Integer> cache = CacheBuilder.newBuilder()
            .build(
                    new CacheLoader<Long, Integer>() {
                        @Override
                        public Integer load(@NonNull Long userId) throws SQLException {
                            if (DiscordApiCollection.getInstance().getOwner().getId() == userId) return Settings.DONATION_ROLE_IDS.length;
                            if (!Bot.isProductionMode()) return 0;

                            Server server = DiscordApiCollection.getInstance().getServerById(Settings.SUPPORT_SERVER_ID).get();
                            for(int i = 0; i < Settings.DONATION_ROLE_IDS.length; i++) {
                                if (server.getRoleById(Settings.DONATION_ROLE_IDS[i]).get().getUsers().stream().anyMatch(user -> user.getId() == userId)) return i + 1;
                            }

                            if (DBDonators.getInstance().getBean().get(userId).isValid()) return 1;
                            return 0;
                        }
                    }
            );

    public void reset() {
        cache.invalidateAll();
    }

    public void resetUser(long userId) {
        cache.refresh(userId);
    }

    public int getPatreonLevel(long userId) throws ExecutionException {
        return cache.get(userId);
    }

}
