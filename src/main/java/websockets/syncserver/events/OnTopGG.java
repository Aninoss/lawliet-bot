package websockets.syncserver.events;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import constants.FisheryStatus;
import core.MainLogger;
import core.ShardManager;
import modules.Fishery;
import mysql.modules.autoclaim.DBAutoClaim;
import mysql.modules.bannedusers.DBBannedUsers;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberBean;
import mysql.modules.guild.DBGuild;
import mysql.modules.upvotes.DBUpvotes;
import mysql.modules.upvotes.UpvotesBean;
import org.json.JSONObject;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

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
            try {
                processUpvote(userId, isWeekend);
            } catch (ExecutionException | InterruptedException e) {
                MainLogger.get().error("Exception", e);
            }
        } else {
            MainLogger.get().error("Wrong type: " + type);
        }
        return null;
    }

    protected void processUpvote(long userId, boolean isWeekend) throws ExecutionException, InterruptedException {
        UpvotesBean upvotesBean = DBUpvotes.getInstance().retrieve();
        if (upvotesBean.getLastUpvote(userId).plus(11, ChronoUnit.HOURS).isBefore(Instant.now())) {
            ShardManager.getInstance().getCachedUserById(userId).ifPresent(user -> {
                MainLogger.get().info("UPVOTE | {}", user.getName());

                ShardManager.getInstance().getLocalMutualGuilds(user).stream()
                        .filter(guild -> DBGuild.getInstance().retrieve(guild.getIdLong()).getFisheryStatus() == FisheryStatus.ACTIVE)
                        .forEach(guild -> {
                            int value = isWeekend ? 2 : 1;
                            FisheryMemberBean userBean = DBFishery.getInstance().retrieve(guild.getIdLong()).getMemberBean(userId);

                            if (DBAutoClaim.getInstance().retrieve().isActive(userId)) {
                                userBean.changeValues(Fishery.getClaimValue(userBean) * value, 0);
                            } else {
                                userBean.addUpvote(value);
                            }
                        });
            });
            upvotesBean.updateLastUpvote(userId);
        }
    }

}
