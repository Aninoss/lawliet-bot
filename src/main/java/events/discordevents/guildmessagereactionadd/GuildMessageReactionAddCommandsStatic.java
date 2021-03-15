package events.discordevents.guildmessagereactionadd;

import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.OnStaticReactionAddListener;
import constants.AssetIds;
import constants.Emojis;
import core.MainLogger;
import core.cache.MessageCache;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMessageReactionAddAbstract;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildBean;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

@DiscordEvent
public class GuildMessageReactionAddCommandsStatic extends GuildMessageReactionAddAbstract {

    @Override
    public boolean onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        Message message;
        try {
            message = MessageCache.getInstance().retrieveMessage(event.getChannel(), event.getMessageIdLong()).get();
        } catch (InterruptedException | ExecutionException e) {
            //Ignore
            return true;
        }

        if (message.getAuthor().getIdLong() == AssetIds.LAWLIET_USER_ID &&
                message.getEmbeds().size() > 0
        ) {
            GuildBean guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
            MessageEmbed embed = message.getEmbeds().get(0);
            if (embed.getTitle() != null && embed.getAuthor() == null) {
                String title = embed.getTitle();
                for (Class<? extends OnStaticReactionAddListener> clazz : CommandContainer.getInstance().getStaticReactionAddCommands()) {
                    Command command = CommandManager.createCommandByClass((Class<? extends Command>) clazz, guildBean.getLocale(), guildBean.getPrefix());
                    if (title.toLowerCase().startsWith(((OnStaticReactionAddListener) command).titleStartIndicator().toLowerCase()) && title.endsWith(Emojis.EMPTY_EMOJI)) {
                        try {
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
