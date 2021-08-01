package events.discordevents.guildmessagereactionremove;

import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.CommandManager;
import commands.listeners.OnStaticReactionRemoveListener;
import core.CustomObservableMap;
import core.MemberCacheController;
import core.cache.MessageCache;
import core.utils.BotPermissionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMessageReactionRemoveAbstract;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;

@DiscordEvent
public class GuildMessageReactionRemoveCommandsStatic extends GuildMessageReactionRemoveAbstract {

    @Override
    public boolean onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) throws Throwable {
        if (!BotPermissionUtil.canReadHistory(event.getChannel(), Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS)) {
            return true;
        }

        CustomObservableMap<Long, StaticReactionMessageData> map = DBStaticReactionMessages.getInstance()
                .retrieve(event.getGuild().getIdLong());
        StaticReactionMessageData messageData = map.get(event.getMessageIdLong());

        if (messageData != null) {
            GuildData guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
            Command command = CommandManager.createCommandByTrigger(messageData.getCommand(), guildBean.getLocale(), guildBean.getPrefix()).get();
            if (command instanceof OnStaticReactionRemoveListener) {
                Message message;
                try {
                    message = MessageCache.getInstance().retrieveMessage(event.getChannel(), event.getMessageIdLong()).get();
                } catch (InterruptedException | ExecutionException e) {
                    //Ignore
                    return true;
                }

                if (map.containsKey(event.getMessageIdLong())) {
                    MemberCacheController.getInstance().loadMembers(event.getGuild()).get();
                    if (!event.getUser().isBot()) {
                        ((OnStaticReactionRemoveListener) command).onStaticReactionRemove(message, event);
                    }
                }
            }
        }

        return true;
    }

}
