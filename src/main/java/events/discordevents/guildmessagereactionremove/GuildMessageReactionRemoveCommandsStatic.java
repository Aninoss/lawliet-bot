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
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.GuildEntity;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;

@DiscordEvent
public class GuildMessageReactionRemoveCommandsStatic extends GuildMessageReactionRemoveAbstract {

    @Override
    public boolean onGuildMessageReactionRemove(MessageReactionRemoveEvent event, EntityManagerWrapper entityManager) throws Throwable {
        if (!BotPermissionUtil.canReadHistory(event.getGuildChannel(), Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
            return true;
        }

        CustomObservableMap<Long, StaticReactionMessageData> map = DBStaticReactionMessages.getInstance()
                .retrieve(event.getGuild().getIdLong());
        StaticReactionMessageData messageData = map.get(event.getMessageIdLong());

        if (messageData != null) {
            GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
            Command command = CommandManager.createCommandByTrigger(messageData.getCommand(), guildEntity.getLocale(), guildEntity.getPrefix()).get();
            if (command instanceof OnStaticReactionRemoveListener) {
                Message message;
                try {
                    message = MessageCache.retrieveMessage(event.getGuildChannel(), event.getMessageIdLong()).get();
                } catch (InterruptedException | ExecutionException e) {
                    //Ignore
                    return true;
                }

                if (map.containsKey(event.getMessageIdLong())) {
                    if (command.getCommandProperties().requiresFullMemberCache()) {
                        MemberCacheController.getInstance().loadMembersFull(event.getGuild()).get();
                    } else {
                        MemberCacheController.getInstance().loadMember(event.getGuild(), event.getUserIdLong()).get();
                    }
                    command.setEntityManager(entityManager);
                    if (event.getMember() != null && !event.getMember().getUser().isBot()) {
                        ((OnStaticReactionRemoveListener) command).onStaticReactionRemove(message, event);
                    }
                }
            }
        }

        return true;
    }

}
