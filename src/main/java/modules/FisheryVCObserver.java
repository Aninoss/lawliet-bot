package modules;

import constants.FisheryStatus;
import core.Bot;
import core.CustomThread;
import core.IntervalBlock;
import core.DiscordApiManager;
import mysql.modules.bannedusers.DBBannedUsers;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryServerBean;
import mysql.modules.server.DBServer;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class FisheryVCObserver {

    private static final FisheryVCObserver ourInstance = new FisheryVCObserver();

    public static FisheryVCObserver getInstance() {
        return ourInstance;
    }

    private FisheryVCObserver() {
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(FisheryVCObserver.class);
    private final int VC_CHECK_INTERVAL_MIN = 1;

    private boolean active = false;

    public void start() {
        if (active) return;
        active = true;

        new CustomThread(() -> {
            IntervalBlock intervalBlock = new IntervalBlock(VC_CHECK_INTERVAL_MIN, ChronoUnit.MINUTES);
            while (intervalBlock.block()) {
                AtomicInteger actions = new AtomicInteger(0);
                DiscordApiManager.getInstance().getLocalServers().stream()
                        .filter(server -> {
                            try {
                                return DBServer.getInstance().getBean(server.getId()).getFisheryStatus() == FisheryStatus.ACTIVE;
                            } catch (Throwable e) {
                                LOGGER.error("Could not get server bean", e);
                            }
                            return false;
                        })
                        .forEach(server -> {
                            try {
                                manageVCFish(server, actions);
                            } catch (Throwable e) {
                                LOGGER.error("Could not manage vc fish observer", e);
                            }
                        });
                LOGGER.info("VC Observer - {} Actions", actions.get());
            }
        }, "vc_observer", 1).start();
    }

    private void manageVCFish(Server server, AtomicInteger actions) throws ExecutionException {
        FisheryServerBean serverBean = DBFishery.getInstance().getBean(server.getId());

        for (ServerVoiceChannel voiceChannel : server.getVoiceChannels()) {
            try {
                ArrayList<User> validUsers = getValidVCUsers(server, voiceChannel);
                if (validUsers.size() > (Bot.isProductionMode() ? 1 : 0) &&
                        (server.getAfkChannel().isEmpty() || voiceChannel.getId() != server.getAfkChannel().get().getId())
                ) {
                    validUsers.forEach(user -> {
                        try {
                            serverBean.getUserBean(user.getId()).registerVC(VC_CHECK_INTERVAL_MIN);
                            actions.incrementAndGet();
                        } catch (ExecutionException e) {
                            LOGGER.error("Exception when registering vc", e);
                        }
                    });
                }
            } catch (Throwable e) {
                LOGGER.error("Error while fetching VC member list", e);
            }
        }
    }

    public static ArrayList<User> getValidVCUsers(Server server, ServerVoiceChannel voiceChannel) {
        ArrayList<User> validUsers = new ArrayList<>();
        for (long userId : voiceChannel.getConnectedUserIds()) {
            server.getMemberById(userId)
                    .ifPresentOrElse(user -> {
                        if (!user.isBot() &&
                                !user.isMuted(server) &&
                                !user.isDeafened(server) &&
                                !user.isSelfDeafened(server) &&
                                !user.isSelfMuted(server) &&
                                !DBBannedUsers.getInstance().getBean().getUserIds().contains(user.getId())
                        ) {
                            validUsers.add(user);
                        }
                    }, () -> {
                        LOGGER.error("VC Observer - missing user with id {} on server {}", userId, server.getId());
                    });
        }

        return validUsers;
    }

}
