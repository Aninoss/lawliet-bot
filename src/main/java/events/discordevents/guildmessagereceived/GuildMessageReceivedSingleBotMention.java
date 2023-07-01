package events.discordevents.guildmessagereceived;

import core.ShardManager;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.JDAUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.GuildEntity;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@DiscordEvent(priority = EventPriority.MEDIUM)
public class GuildMessageReceivedSingleBotMention extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(MessageReceivedEvent event, EntityManagerWrapper entityManager) throws Throwable {
        if (event.getMessage().getContentRaw().replace("@!", "@").trim().equalsIgnoreCase(ShardManager.getSelf().getAsMention())) {
            GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
            String text = TextManager.getString(guildEntity.getLocale(), TextManager.GENERAL, "bot_ping_help", guildEntity.getPrefix());
            if (BotPermissionUtil.canWrite(event.getGuildChannel())) {
                JDAUtil.replyMessage(event.getMessage(), text).queue();
            }

            return false;
        }

        return true;
    }

}
