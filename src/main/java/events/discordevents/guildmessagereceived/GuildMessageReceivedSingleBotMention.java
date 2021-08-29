package events.discordevents.guildmessagereceived;

import core.ShardManager;
import core.TextManager;
import core.utils.BotPermissionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@DiscordEvent(priority = EventPriority.MEDIUM)
public class GuildMessageReceivedSingleBotMention extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(GuildMessageReceivedEvent event) throws Throwable {
        if (event.getMessage().getContentRaw().replace("@!", "@").trim().equalsIgnoreCase(ShardManager.getSelf().getAsMention())) {
            GuildData guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());

            String text = TextManager.getString(guildBean.getLocale(), TextManager.GENERAL, "bot_ping_help", guildBean.getPrefix());
            if (BotPermissionUtil.canWrite(event.getChannel())) {
                event.getChannel().sendMessage(text).queue();
            }

            return false;
        }

        return true;
    }

}
