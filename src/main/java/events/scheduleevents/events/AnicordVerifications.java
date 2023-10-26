package events.scheduleevents.events;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import constants.AnicordVerificationIds;
import constants.ExceptionRunnable;
import core.ShardManager;
import events.scheduleevents.ScheduleEventFixedRate;
import mysql.redis.fisheryusers.FisheryUserManager;
import mysql.redis.fisheryusers.FisheryGuildData;
import mysql.redis.fisheryusers.FisheryMemberData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

@ScheduleEventFixedRate(rateValue = 1, rateUnit = ChronoUnit.HOURS)
public class AnicordVerifications implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        Guild guild = ShardManager.getLocalGuildById(AnicordVerificationIds.GUILD_ID).orElse(null);
        if (guild != null) {
            Role verificationRole = guild.getRoleById(AnicordVerificationIds.ROLE_ID);
            TextChannel verificationChannel = guild.getTextChannelById(AnicordVerificationIds.CHANNEL_ID);
            FisheryGuildData fisheryGuildData = FisheryUserManager.getGuildData(guild.getIdLong());
            guild.getMembers().stream()
                    .filter(m -> !m.getRoles().contains(verificationRole))
                    .forEach(m -> checkMember(verificationRole, verificationChannel, m, fisheryGuildData.getMemberData(m.getIdLong())));
        }
    }

    private void checkMember(Role verificationRole, TextChannel verificationChannel, Member member, FisheryMemberData fisheryMemberData) {
        if (fisheryMemberData.getMessagesAnicord() >= 50 &&
                member.hasTimeJoined() &&
                OffsetDateTime.now().isAfter(member.getTimeJoined().plusHours(24))
        ) {
            verificationRole.getGuild().addRoleToMember(member, verificationRole).queue();
            verificationChannel.sendMessage(String.format("%s ist jetzt ein verifiziertes Mitglied!", member.getAsMention())).queue();
        }
    }

}
