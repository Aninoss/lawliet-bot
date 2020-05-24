package DiscordListener.ReactionRemove;

import CommandListeners.OnReactionAddStaticListener;
import CommandListeners.OnReactionRemoveStaticListener;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import CommandSupporters.CommandManager;
import Constants.Settings;
import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerTypeAbstracts.ReactionAddAbstract;
import DiscordListener.ListenerTypeAbstracts.ReactionRemoveAbstract;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;

@DiscordListenerAnnotation()
public class ReactionRemoveCommandsStatic extends ReactionRemoveAbstract {

    @Override
    public boolean onReactionRemove(ReactionRemoveEvent event) throws Throwable {
        Message message = event.getMessage().get();

        if (event.getServer().isPresent() &&
                message.getAuthor().isYourself() &&
                message.getEmbeds().size() > 0
        ) {
            Embed embed = message.getEmbeds().get(0);
            if (embed.getTitle().isPresent() && !embed.getAuthor().isPresent()) {
                String title = embed.getTitle().get();
                for (Class<? extends OnReactionRemoveStaticListener> clazz : CommandContainer.getInstance().getStaticReactionRemoveCommands()) {
                    Command command = CommandManager.createCommandByClass((Class<? extends Command>)clazz);
                    if (title.toLowerCase().startsWith(((OnReactionRemoveStaticListener)command).getTitleStartIndicator().toLowerCase()) && title.endsWith(Settings.EMPTY_EMOJI)) {
                        ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
                        command.setPrefix(serverBean.getPrefix());
                        command.setLocale(serverBean.getLocale());
                        ((OnReactionRemoveStaticListener)command).onReactionRemoveStatic(message, event);

                        return false;
                    }
                }
            }
        }

        return true;
    }
}
