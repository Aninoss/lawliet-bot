package events.discordevents.guildmessagereactionadd;

import commands.listeners.OnStaticReactionAddListener;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import constants.Emojis;
import core.MainLogger;
import core.ShardManager;
import core.cache.MessageCache;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMessageReactionAddAbstract;
import mysql.modules.server.DBServer;
import mysql.modules.server.GuildBean;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

@DiscordEvent()
public class GuildMessageReactionAddCommandsStatic extends GuildMessageReactionAddAbstract {

    @Override
    public boolean onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) throws Throwable {
        MessageCache.getInstance().get(event.getChannel(), event.getMessageIdLong())
                .thenAccept(message -> {
                    if (message.getAuthor().getIdLong() == ShardManager.getInstance().getSelfId() &&
                            message.getEmbeds().size() > 0
                    ) {
                        GuildBean guildBean = DBServer.getInstance().retrieve(event.getGuild().getIdLong());
                        MessageEmbed embed = message.getEmbeds().get(0);
                        if (embed.getTitle() != null && embed.getAuthor() == null) {
                            String title = embed.getTitle();
                            for (Class<? extends OnStaticReactionAddListener> clazz : CommandContainer.getInstance().getStaticReactionAddCommands()) {
                                Command command = CommandManager.createCommandByClass((Class<? extends Command>) clazz, guildBean.getLocale(), guildBean.getPrefix());
                                if (title.toLowerCase().startsWith(((OnStaticReactionAddListener)command).titleStartIndicator().toLowerCase()) && title.endsWith(Emojis.EMPTY_EMOJI)) {
                                    try {
                                        ((OnStaticReactionAddListener)command).onStaticReactionAdd(message, event);
                                    } catch (Throwable throwable) {
                                        MainLogger.get().error("Static reaction add exception", throwable);
                                    }
                                }
                            }
                        }
                    }
                });

        return true;
    }

}
