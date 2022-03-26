package events.sync.events;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import core.GlobalThreadPool;
import core.MainLogger;
import modules.fishery.Fishery;
import modules.fishery.FisheryStatus;
import mysql.modules.autoclaim.DBAutoClaim;
import mysql.modules.bannedusers.DBBannedUsers;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import mysql.modules.guild.DBGuild;
import mysql.modules.upvotes.DBUpvotes;
import mysql.modules.upvotes.UpvotesData;
import org.json.JSONObject;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;

@SyncServerEvent(event = "TOPGG")
public class OnTopGG implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long userId = jsonObject.getLong("user");
        if (DBBannedUsers.getInstance().retrieve().getUserIds().contains(userId)) {
            return null;
        }

        String type = jsonObject.getString("type");
        boolean isWeekend = jsonObject.has("isWeekend") && jsonObject.getBoolean("isWeekend");

        if (type.equals("upvote")) {
            GlobalThreadPool.getExecutorService().submit(() -> {
                try {
                    processUpvote(userId, isWeekend);
                } catch (ExecutionException | InterruptedException e) {
                    MainLogger.get().error("Exception", e);
                }
            });
        } else {
            MainLogger.get().error("Wrong type: " + type);
        }
        return null;
    }

    protected void processUpvote(long userId, boolean isWeekend) throws ExecutionException, InterruptedException {
        UpvotesData upvotesData = DBUpvotes.getInstance().retrieve();
        AtomicInteger guilds = new AtomicInteger();
        DBFishery.getInstance().getGuildIdsForFisheryUser(userId).stream()
                .filter(guildId -> DBGuild.getInstance().retrieve(guildId).getFisheryStatus() == FisheryStatus.ACTIVE)
                .forEach(guildId -> {
                    int factor = isWeekend ? 2 : 1;
                    FisheryMemberData userBean = DBFishery.getInstance().retrieve(guildId).getMemberData(userId);

                    if (DBAutoClaim.getInstance().retrieve().isActive(userId)) {
                        userBean.changeValues(Fishery.getClaimValue(userBean) * factor, 0);
                    } else {
                        userBean.addUpvote(factor);
                    }
                    guilds.incrementAndGet();
                });

        upvotesData.updateLastUpvote(userId);
        MainLogger.get().info("UPVOTE | {} ({} guild/s)", userId, guilds.get());
    }

}
