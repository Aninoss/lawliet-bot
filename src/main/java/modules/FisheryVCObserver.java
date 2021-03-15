package modules;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import constants.FisheryStatus;
import core.*;
import mysql.modules.bannedusers.DBBannedUsers;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildBean;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;

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
                        .filter(guild -> {
                            try {
                                return DBGuild.getInstance().retrieve(guild.getIdLong()).getFisheryStatus() == FisheryStatus.ACTIVE;
                            } catch (Throwable e) {
                                MainLogger.get().error("Could not get server bean", e);
                            }
                            return false;
                        })
                        .forEach(guild -> {
                            try {
                                manageVCFish(guild, actions);
                            } catch (Throwable e) {
                                MainLogger.get().error("Could not manage vc fish observer", e);
                            }
                        });
                MainLogger.get().info("VC Observer - {} Actions", actions.get());
            }
        }, "vc_observer", 1).start();
    }

    private void manageVCFish(Guild guild, AtomicInteger actions) {
        FisheryGuildBean serverBean = DBFishery.getInstance().retrieve(guild.getIdLong());

        for (VoiceChannel voiceChannel : guild.getVoiceChannels()) {
            try {
                List<Member> validMembers = getValidVCMembers(voiceChannel);
                if (validMembers.size() > (Program.isProductionMode() ? 1 : 0) &&
                        (guild.getAfkChannel() != null || voiceChannel.getIdLong() != guild.getAfkChannel().getIdLong())
                ) {
                    validMembers.forEach(member -> {
                        try {
                            serverBean.getMemberBean(member.getIdLong()).registerVC(VC_CHECK_INTERVAL_MIN);
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

    public static List<Member> getValidVCMembers(VoiceChannel voiceChannel) {
        ArrayList<Member> validMembers = new ArrayList<>();
        for (Member member : voiceChannel.getMembers()) {
            GuildVoiceState voice = member.getVoiceState();
            if (voice != null &&
                    !member.getUser().isBot() &&
                    !voice.isMuted() &&
                    !voice.isDeafened() &&
                    !voice.isSelfMuted() &&
                    !voice.isSelfDeafened() &&
                    !DBBannedUsers.getInstance().retrieve().getUserIds().contains(member.getIdLong())
            ) {
                validMembers.add(member);
            }
        }

        return Collections.unmodifiableList(validMembers);
    }

}
