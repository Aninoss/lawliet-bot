package events.discordevents.guildmessagereceived;

import core.ShardManager;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.JDAUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@DiscordEvent(priority = EventPriority.MEDIUM)
public class GuildMessageReceivedSingleBotMention extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(MessageReceivedEvent event) throws Throwable {
        if (event.getMessage().getContentRaw().replace("@!", "@").trim().equalsIgnoreCase(ShardManager.getSelf().getAsMention())) {
            GuildData guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());

            String text = TextManager.getString(guildBean.getLocale(), TextManager.GENERAL, "bot_ping_help", guildBean.getPrefix());
            if (BotPermissionUtil.canWrite(event.getTextChannel())) {
                JDAUtil.replyMessage(event.getMessage(), text).queue();
            }

            return false;
        }

        return true;
    }

}
