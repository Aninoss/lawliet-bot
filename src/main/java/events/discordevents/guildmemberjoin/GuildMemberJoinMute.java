package events.discordevents.guildmemberjoin;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import commands.Command;
import commands.runnables.moderationcategory.MuteCommand;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import mysql.modules.guild.DBGuild;
import mysql.modules.servermute.DBServerMute;
import mysql.modules.servermute.ServerMuteData;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildMemberJoinMute extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event) throws Throwable {
        ServerMuteData serverMuteData = DBServerMute.getInstance().retrieve(event.getGuild().getIdLong())
                .get(event.getMember().getIdLong());

        if (serverMuteData != null &&
                serverMuteData.isNewMethod() &&
                !event.getMember().isTimedOut()
        ) {
            Locale locale = DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).getLocale();
            Instant expirationMax = Instant.now().plus(Duration.ofDays(27));
            Instant expiration = serverMuteData.getExpirationTime().orElse(Instant.MAX);
            if (expiration.isAfter(expirationMax)) {
                expiration = expirationMax;
            }
            event.getMember().timeoutUntil(expiration)
                    .reason(Command.getCommandLanguage(MuteCommand.class, locale).getTitle())
                    .queue();
        }
        return true;
    }

}
