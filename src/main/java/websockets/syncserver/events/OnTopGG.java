package websockets.syncserver.events;

import constants.FisheryStatus;
import core.DiscordApiManager;
import core.cache.PatreonCache;
import modules.Fishery;
import mysql.modules.autoclaim.DBAutoClaim;
import mysql.modules.bannedusers.DBBannedUsers;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryUserBean;
import mysql.modules.server.DBServer;
import mysql.modules.upvotes.DBUpvotes;
import mysql.modules.upvotes.UpvotesBean;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;

@SyncServerEvent(event = "TOPGG")
public class OnTopGG implements SyncServerFunction {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnTopGG.class);

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long userId = jsonObject.getLong("user");
        if (DBBannedUsers.getInstance().getBean().getUserIds().contains(userId))
            return null;

        String type = jsonObject.getString("type");
        boolean isWeekend = jsonObject.has("isWeekend") && jsonObject.getBoolean("isWeekend");

        if (type.equals("upvote")) {
            try {
                processUpvote(userId, isWeekend);
            } catch (ExecutionException | InterruptedException e) {
                LOGGER.error("Exception", e);
            }
        } else {
            LOGGER.error("Wrong type: " + type);
        }
        return null;
    }

    protected void processUpvote(long userId, boolean isWeekend) throws ExecutionException, InterruptedException {
        UpvotesBean upvotesBean = DBUpvotes.getInstance().getBean();
        if (upvotesBean.getLastUpvote(userId).plus(11, ChronoUnit.HOURS).isBefore(Instant.now())) {
            DiscordApiManager.getInstance().getCachedUserById(userId).ifPresent(user -> {
                LOGGER.info("UPVOTE | {}", user.getName());

                DiscordApiManager.getInstance().getLocalMutualServers(user).stream()
                        .filter(server -> DBServer.getInstance().getBean(server.getId()).getFisheryStatus() == FisheryStatus.ACTIVE)
                        .forEach(server -> {
                            int value = isWeekend ? 2 : 1;
                            FisheryUserBean userBean = DBFishery.getInstance().getBean(server.getId()).getUserBean(userId);

                            if (PatreonCache.getInstance().getUserTier(userId) >= 2 &&
                                    DBAutoClaim.getInstance().getBean().isActive(userId)
                            ) {
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
