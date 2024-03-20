package events.discordevents.guildmemberjoin;

import constants.AssetIds;
import core.MainLogger;
import core.schedule.MainScheduler;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

@DiscordEvent(priority = EventPriority.HIGH, allowBots = true, allowBannedUser = true)
public class GuildMemberJoinAnicordAntiRaid extends GuildMemberJoinAbstract {

    private static boolean lockdown = false;
    private static final ArrayList<Instant> instantList = new ArrayList<>();

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event, EntityManagerWrapper entityManager) throws Throwable {
        if (event.getGuild().getIdLong() != AssetIds.ANICORD_SERVER_ID) {
            return true;
        }
        if (lockdown) {
            return false;
        }

        synchronized (this) {
            Instant now = Instant.now();
            instantList.add(now);
            if (instantList.size() > 10) {
                instantList.remove(0);

                if (instantList.get(0).plus(Duration.ofMinutes(1)).isAfter(now)) {
                    lockdown = true;
                    event.getGuild().getManager()
                            .setInvitesDisabled(true)
                            .reason("Raid Protection")
                            .queue();
                    event.getGuild().getChannelById(GuildMessageChannel.class, 462420339364724751L)
                            .sendMessage("Raid wurde erkannt! Invites zum Server sind temporär deaktiviert.")
                            .queue();

                    MainScheduler.schedule(Duration.ofMinutes(10), () -> {
                        event.getGuild().getMembers().stream()
                                .filter(m -> m.hasTimeJoined() && m.getTimeJoined().plusMinutes(1).toInstant().isAfter(now))
                                .forEach(m -> MainLogger.get().warn("Raid user: " + m.getUser().getName()));
                        event.getGuild().getManager()
                                .setInvitesDisabled(false)
                                .reason("Raid Protection")
                                .queue();
                        lockdown = false;
                    });
                    return false;
                }
            }

            return true;
        }
    }

}
