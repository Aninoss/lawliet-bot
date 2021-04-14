package events.discordevents.guildmessagereactionadd;

import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.OnStaticReactionAddListener;
import constants.Emojis;
import core.CustomObservableMap;
import core.MainLogger;
import core.ShardManager;
import core.cache.MessageCache;
import core.utils.BotPermissionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMessageReactionAddAbstract;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildBean;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

@DiscordEvent
public class GuildMessageReactionAddCommandsStatic extends GuildMessageReactionAddAbstract {

    @Override
    public boolean onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if (!BotPermissionUtil.canReadHistory(event.getChannel(), Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS)) {
            return true;
        }

        Message message;
        try {
            message = MessageCache.getInstance().retrieveMessage(event.getChannel(), event.getMessageIdLong()).get();
        } catch (InterruptedException | ExecutionException e) {
            //Ignore
            return true;
        }

        boolean valid = DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong()).containsKey(event.getMessageIdLong());
        if ((valid || message.getAuthor().getIdLong() == ShardManager.getInstance().getSelfId()) &&
                message.getEmbeds().size() > 0
        ) {
            GuildBean guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
            MessageEmbed embed = message.getEmbeds().get(0);
            if (embed.getTitle() != null && embed.getAuthor() == null) {
                String title = embed.getTitle();
                for (Class<? extends OnStaticReactionAddListener> clazz : CommandContainer.getInstance().getStaticReactionAddCommands()) {
                    Command command = CommandManager.createCommandByClass((Class<? extends Command>) clazz, guildBean.getLocale(), guildBean.getPrefix());
                    if (title.toLowerCase().startsWith(((OnStaticReactionAddListener) command).titleStartIndicator().toLowerCase()) && (valid || title.endsWith(Emojis.EMPTY_EMOJI))) {
                        try {
                            CustomObservableMap<Long, StaticReactionMessageData> map = DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong());
                            if (!map.containsKey(event.getMessageIdLong())) {
                                map.put(event.getMessageIdLong(), new StaticReactionMessageData(message, command.getTrigger()));
                            }

                            ((OnStaticReactionAddListener) command).onStaticReactionAdd(message, event);
                        } catch (Throwable throwable) {
                            MainLogger.get().error("Static reaction add exception", throwable);
                        }
                    }
                }
            }
        }

        return true;
    }

}
