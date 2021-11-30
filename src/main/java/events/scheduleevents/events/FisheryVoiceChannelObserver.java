package events.scheduleevents.events;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import modules.fishery.FisheryStatus;
import core.MainLogger;
import core.Program;
import core.ShardManager;
import constants.ExceptionRunnable;
import events.scheduleevents.ScheduleEventFixedRate;
import mysql.modules.bannedusers.DBBannedUsers;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;

@ScheduleEventFixedRate(rateValue = FisheryVoiceChannelObserver.VC_CHECK_INTERVAL_MIN, rateUnit = ChronoUnit.MINUTES)
public class FisheryVoiceChannelObserver implements ExceptionRunnable {

    public static final int VC_CHECK_INTERVAL_MIN = 1;

    @Override
    public void run() throws Throwable {
        AtomicInteger actions = new AtomicInteger(0);
        ShardManager.getLocalGuilds().stream()
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
                        manageVoiceFish(guild, actions);
                    } catch (Throwable e) {
                        MainLogger.get().error("Could not manage vc fish observer", e);
                    }
                });
        MainLogger.get().info("VC Observer - {} Actions", actions.get());
    }

    private void manageVoiceFish(Guild guild, AtomicInteger actions) {
        FisheryGuildData serverBean = DBFishery.getInstance().retrieve(guild.getIdLong());

        for (VoiceChannel voiceChannel : guild.getVoiceChannels()) {
            try {
                List<Member> validMembers = getValidVCMembers(voiceChannel);
                VoiceChannel afkVoice = guild.getAfkChannel();
                if (validMembers.size() >= (Program.productionMode() ? 2 : 1) &&
                        (afkVoice == null || voiceChannel.getIdLong() != afkVoice.getIdLong())
                ) {
                    validMembers.forEach(member -> {
                        try {
                            serverBean.getMemberData(member.getIdLong()).registerVoice(VC_CHECK_INTERVAL_MIN);
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
                    !voice.isSuppressed() &&
                    !DBBannedUsers.getInstance().retrieve().getUserIds().contains(member.getIdLong())
            ) {
                validMembers.add(member);
            }
        }

        return Collections.unmodifiableList(validMembers);
    }

}
