package modules.automod;

import commands.Command;
import commands.CommandManager;
import commands.listeners.CommandProperties;
import constants.Category;
import constants.Permission;
import core.EmbedFactory;
import core.DiscordApiManager;
import core.PermissionCheckRuntime;
import core.TextManager;
import modules.Mod;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public abstract class AutoModAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(AutoModAbstract.class);

    private final Message message;

    public AutoModAbstract(Message message) throws ExecutionException {
        this.message = message;
    }

    /*
    * returns true if the message is fine
     */
    public boolean check() {
        if (!message.getUserAuthor().get().isBot() && checkCondition(message)) {
            try {
                ServerBean serverBean = DBServer.getInstance().getBean(message.getServer().get().getId());
                Class<? extends Command> commandClass = getCommandClass();
                if (PermissionCheckRuntime.getInstance().botHasPermission(serverBean.getLocale(), commandClass, message.getServerTextChannel().get(), Permission.MANAGE_MESSAGES)) {
                    message.delete();
                }
                punish(message, serverBean, commandClass);
                return false;
            } catch (Throwable e) {
                LOGGER.error("Exception in server bean", e);
            }
        }

        return true;
    }

    private void punish(Message message, ServerBean serverBean, Class<? extends Command> commandClass) {
        Server server = message.getServer().get();
        User author = message.getUserAuthor().get();
        CommandProperties commandProperties = Command.getClassProperties(commandClass);
        String commandTitle = TextManager.getString(serverBean.getLocale(), Category.MODERATION, commandProperties.trigger() + "_title");
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(commandProperties.emoji() + " " + commandTitle);
        designEmbed(message, serverBean.getLocale(), eb);

        try {
            Command command = CommandManager.createCommandByClass(commandClass, serverBean.getLocale(), serverBean.getPrefix());
            Mod.postLog(command, eb, server, author).thenRun(() -> {
                try {
                    Mod.insertWarning(serverBean.getLocale(), server, author, DiscordApiManager.getInstance().getYourself(), commandTitle, withAutoActions(message, serverBean.getLocale()));
                } catch (ExecutionException e) {
                    LOGGER.error("Error when creating command instance");
                }
            });
        } catch (ExecutionException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            LOGGER.error("Error when creating command instance");
        }
    }

    protected abstract boolean withAutoActions(Message message, Locale locale);

    protected abstract void designEmbed(Message message, Locale locale, EmbedBuilder eb);

    protected abstract Class<? extends Command> getCommandClass();

    protected abstract boolean checkCondition(Message message);

}
