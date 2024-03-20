package events.discordevents.guildmessagereceived;

import constants.AssetIds;
import core.RatelimitManager;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@DiscordEvent(priority = EventPriority.HIGH, allowBannedUser = true)
public class GuildMessageReceivedAnicordAntiSpam extends GuildMessageReceivedAbstract {

    private final RatelimitManager ratelimitManager = new RatelimitManager();

    @Override
    public boolean onGuildMessageReceived(MessageReceivedEvent event, EntityManagerWrapper entityManager) throws Throwable {
        if (event.getGuild().getIdLong() == AssetIds.ANICORD_SERVER_ID &&
                event.getChannel().getIdLong() != 758285721877479504L &&
                event.getChannel().getIdLong() != 462405404211675136L &&
                event.getChannel().getIdLong() != 693912897998553198L &&
                event.getMember().hasTimeJoined() &&
                event.getMember().getTimeJoined().toInstant().plus(30, ChronoUnit.MINUTES).isAfter(Instant.now())
        ) {
            if (ratelimitManager.checkAndSet(event.getMember().getIdLong(), 5, Duration.ofSeconds(3)).isPresent()) {
                event.getGuild().ban(event.getMember(), 1, TimeUnit.DAYS).reason("Anti Raid (Spam)").queue();
                event.getGuild().getChannelById(GuildMessageChannel.class, 462420339364724751L).sendMessage("ANTI RAID (SPAM) FOR " + StringUtil.escapeMarkdown(event.getMember().getUser().getName()) + " IN " + event.getChannel().getAsMention()).queue();
                return false;
            }
        }

        return true;
    }

}
