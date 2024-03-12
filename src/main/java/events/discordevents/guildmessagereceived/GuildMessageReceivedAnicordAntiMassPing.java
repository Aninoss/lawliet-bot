package events.discordevents.guildmessagereceived;

import constants.AssetIds;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class GuildMessageReceivedAnicordAntiMassPing extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(MessageReceivedEvent event, EntityManagerWrapper entityManager) throws Throwable {
        if (event.getGuild().getIdLong() == AssetIds.ANICORD_SERVER_ID &&
                event.getMessage().getMentions().getUsers().size() >= 20 &&
                event.getMessage().getMember().hasTimeJoined() &&
                event.getMessage().getMember().getTimeJoined().toInstant().plus(30, ChronoUnit.MINUTES).isAfter(Instant.now())
        ) {
            event.getGuild().ban(event.getMember(), 1, TimeUnit.DAYS).reason("Anti Raid (Mass Ping)").queue();
            event.getGuild().getTextChannelById(462420339364724751L).sendMessage("ANTI RAID (MASS PING) FOR " + StringUtil.escapeMarkdown(event.getMember().getUser().getName())).queue();
            return false;
        }

        return true;
    }

}
