package events.sync.events;

import core.GlobalThreadPool;
import core.MainLogger;
import core.cache.UserBannedCache;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;
import modules.fishery.Fishery;
import modules.fishery.FisheryStatus;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.modules.autoclaim.DBAutoClaim;
import mysql.modules.upvotes.DBUpvotes;
import mysql.modules.upvotes.UpvoteSlot;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryUserManager;
import org.json.JSONObject;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@SyncServerEvent(event = "TOPGG")
public class OnTopGG implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        String type = jsonObject.getString("type");
        boolean isWeekend = jsonObject.has("isWeekend") && jsonObject.getBoolean("isWeekend");

        if (!type.equals("upvote")) {
            MainLogger.get().error("Wrong type: " + type);
            return null;
        }

        GlobalThreadPool.submit(() -> {
            long userId = jsonObject.getLong("user");
            if (UserBannedCache.getInstance().isBanned(userId)) {
                return;
            }

            try {
                processUpvote(userId, isWeekend);
            } catch (ExecutionException | InterruptedException e) {
                MainLogger.get().error("Exception", e);
            }
        });
        return null;
    }

    protected void processUpvote(long userId, boolean isWeekend) throws ExecutionException, InterruptedException {
        AtomicInteger guilds = new AtomicInteger();
        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(OnTopGG.class)) {
            for (Long guildId : FisheryUserManager.getGuildIdsByUserId(userId, true)) {
                if (entityManager.findGuildEntity(guildId).getFishery().getFisheryStatus() != FisheryStatus.ACTIVE) {
                    continue;
                }

                int factor = isWeekend ? 2 : 1;
                FisheryMemberData userBean = FisheryUserManager.getGuildData(guildId).getMemberData(userId);

                if (DBAutoClaim.getInstance().retrieve().isActive(userId)) {
                    userBean.changeValues(Fishery.getClaimValue(userBean) * factor, 0);
                } else {
                    userBean.addUpvote(factor);
                }
                guilds.incrementAndGet();

                entityManager.clear();
            }
        }

        DBUpvotes.saveUpvoteSlot(new UpvoteSlot(userId, Instant.now(), 0));
        MainLogger.get().info("UPVOTE | {} ({} guild/s)", userId, guilds.get());
    }

}
