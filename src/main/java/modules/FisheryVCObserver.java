package modules;

import constants.FisheryStatus;
import core.*;
import mysql.modules.bannedusers.DBBannedUsers;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryServerBean;
import mysql.modules.server.DBServer;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class FisheryVCObserver {

    private static final FisheryVCObserver ourInstance = new FisheryVCObserver();

    public static FisheryVCObserver getInstance() {
        return ourInstance;
    }

    private FisheryVCObserver() {
    }

    private final int VC_CHECK_INTERVAL_MIN = 1;

    private boolean active = false;

    public void start() {
        if (active) return;
        active = true;

        new CustomThread(() -> {
            IntervalBlock intervalBlock = new IntervalBlock(VC_CHECK_INTERVAL_MIN, ChronoUnit.MINUTES);
            while (intervalBlock.block()) {
                AtomicInteger actions = new AtomicInteger(0);
                ShardManager.getInstance().getLocalGuilds().stream()
                        .filter(server -> {
                            try {
                                return DBServer.getInstance().getBean(server.getId()).getFisheryStatus() == FisheryStatus.ACTIVE;
                            } catch (Throwable e) {
                                MainLogger.get().error("Could not get server bean", e);
                            }
                            return false;
                        })
                        .forEach(server -> {
                            try {
                                manageVCFish(server, actions);
                            } catch (Throwable e) {
                                MainLogger.get().error("Could not manage vc fish observer", e);
                            }
                        });
                MainLogger.get().info("VC Observer - {} Actions", actions.get());
            }
        }, "vc_observer", 1).start();
    }

    private void manageVCFish(Server server, AtomicInteger actions) throws ExecutionException {
        FisheryServerBean serverBean = DBFishery.getInstance().getBean(server.getId());

        for (ServerVoiceChannel voiceChannel : server.getVoiceChannels()) {
            try {
                ArrayList<User> validUsers = getValidVCMembers(server, voiceChannel);
                if (validUsers.size() > (Bot.isProductionMode() ? 1 : 0) &&
                        (server.getAfkChannel().isEmpty() || voiceChannel.getId() != server.getAfkChannel().get().getId())
                ) {
                    validUsers.forEach(user -> {
                        try {
                            serverBean.getUserBean(user.getId()).registerVC(VC_CHECK_INTERVAL_MIN);
                            actions.incrementAndGet();
                        } catch (ExecutionException e) {
                            MainLogger.get().error("Exception when registering vc", e);
                        }
                    });
                }
            } catch (Throwable e) {
                MainLogger.get().error("Error while fetching VC member list", e);
            }
        }
    }

    public static ArrayList<Member> getValidVCMembers(VoiceChannel voiceChannel) {
        ArrayList<Member> validMembers = new ArrayList<>();
        for (Member member : voiceChannel.getMembers()) {
            GuildVoiceState voice = member.getVoiceState();
            if (!member.getUser().isBot() &&
                    !voice.isMuted() &&
                    !voice.isDeafened() &&
                    !voice.isSelfMuted() &&
                    !voice.isSelfDeafened() &&
                    !DBBannedUsers.getInstance().getBean().getUserIds().contains(member.getIdLong())
            ) {
                validMembers.add(member);
            }
        }

        return validMembers;
    }

}
