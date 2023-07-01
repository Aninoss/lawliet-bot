package events.discordevents.guildmemberjoin;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import commands.Command;
import commands.runnables.moderationcategory.MuteCommand;
import core.utils.BotPermissionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.modules.servermute.DBServerMute;
import mysql.modules.servermute.ServerMuteData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildMemberJoinMute extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event, EntityManagerWrapper entityManager) throws Throwable {
        ServerMuteData serverMuteData = DBServerMute.getInstance().retrieve(event.getGuild().getIdLong())
                .get(event.getMember().getIdLong());

        if (serverMuteData != null &&
                serverMuteData.isNewMethod() &&
                !event.getMember().isTimedOut()
        ) {
            Locale locale = entityManager.findGuildEntity(event.getGuild().getIdLong()).getLocale();
            Instant expirationMax = Instant.now().plus(Duration.ofDays(27));
            Instant expiration = serverMuteData.getExpirationTime().orElse(Instant.MAX);
            if (expiration.isAfter(expirationMax)) {
                expiration = expirationMax;
            }
            if (!BotPermissionUtil.can(event.getMember(), Permission.ADMINISTRATOR)) {
                event.getMember().timeoutUntil(expiration)
                        .reason(Command.getCommandLanguage(MuteCommand.class, locale).getTitle())
                        .queue();
            }
        }
        return true;
    }

}
