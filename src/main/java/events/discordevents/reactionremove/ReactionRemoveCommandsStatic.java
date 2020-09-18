package events.discordevents.reactionremove;

import commands.listeners.OnReactionRemoveStaticListener;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import constants.Settings;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ReactionRemoveAbstract;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;

import java.util.concurrent.ExecutionException;

@DiscordEvent()
public class ReactionRemoveCommandsStatic extends ReactionRemoveAbstract {

    @Override
    public boolean onReactionRemove(ReactionRemoveEvent event) throws Throwable {
        Message message;
        try {
            if (event.getMessage().isPresent()) message = event.getMessage().get();
            else message = event.getChannel().getMessageById(event.getMessageId()).get();
        } catch (InterruptedException | ExecutionException e) {
            //Ignore
            return true;
        }

        if (event.getServer().isPresent() &&
                message.getAuthor().isYourself() &&
                message.getEmbeds().size() > 0
        ) {
            Embed embed = message.getEmbeds().get(0);
            if (embed.getTitle().isPresent() && !embed.getAuthor().isPresent()) {
                ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
                String title = embed.getTitle().get();
                for (Class<? extends OnReactionRemoveStaticListener> clazz : CommandContainer.getInstance().getStaticReactionRemoveCommands()) {
                    Command command = CommandManager.createCommandByClass((Class<? extends Command>)clazz, serverBean.getLocale(), serverBean.getPrefix());
                    if (title.toLowerCase().startsWith(((OnReactionRemoveStaticListener)command).getTitleStartIndicator().toLowerCase()) && title.endsWith(Settings.EMPTY_EMOJI)) {
                        ((OnReactionRemoveStaticListener)command).onReactionRemoveStatic(message, event);

                        return false;
                    }
                }
            }
        }

        return true;
    }
}
