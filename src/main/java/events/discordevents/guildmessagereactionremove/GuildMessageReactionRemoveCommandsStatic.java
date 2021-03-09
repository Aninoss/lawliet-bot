package events.discordevents.guildmessagereactionremove;

import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.OnStaticReactionRemoveListener;
import constants.Emojis;
import core.ShardManager;
import core.cache.MessageCache;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMessageReactionRemoveAbstract;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildBean;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;

@DiscordEvent
public class GuildMessageReactionRemoveCommandsStatic extends GuildMessageReactionRemoveAbstract {

    @Override
    public boolean onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
        MessageCache.getInstance().get(event.getChannel(), event.getMessageIdLong())
                .thenAccept(message -> {
                    if (message.getAuthor().getIdLong() == ShardManager.getInstance().getSelfId() &&
                            message.getEmbeds().size() > 0
                    ) {
                        GuildBean guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
                        MessageEmbed embed = message.getEmbeds().get(0);
                        if (embed.getTitle() != null && embed.getAuthor() == null) {
                            String title = embed.getTitle();
                            for (Class<? extends OnStaticReactionRemoveListener> clazz : CommandContainer.getInstance().getStaticReactionRemoveCommands()) {
                                Command command = CommandManager.createCommandByClass((Class<? extends Command>) clazz, guildBean.getLocale(), guildBean.getPrefix());
                                if (title.toLowerCase().startsWith(((OnStaticReactionRemoveListener)command).titleStartIndicator().toLowerCase()) && title.endsWith(Emojis.EMPTY_EMOJI)) {
                                    try {
                                        ((OnStaticReactionRemoveListener)command).onStaticReactionRemove(message, event);
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
