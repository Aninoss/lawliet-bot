package modules.automod;

import commands.Command;
import commands.CommandManager;
import commands.listeners.CommandProperties;
import constants.Category;
import core.*;
import modules.Mod;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public abstract class AutoModAbstract {

    private final Message message;

    public AutoModAbstract(Message message) throws ExecutionException {
        this.message = message;
    }

    /*
    * returns true if the message is fine
     */
    public boolean check() {
        if (!message.getAuthor().isBot() && checkCondition(message)) {
            try {
                ServerBean serverBean = DBServer.getInstance().getBean(message.getGuild().getIdLong());
                Class<? extends Command> commandClass = getCommandClass();
                if (PermissionCheckRuntime.getInstance().botHasPermission(serverBean.getLocale(), commandClass, message.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                    message.delete().queue();
                }
                punish(message, serverBean, commandClass);
                return false;
            } catch (Throwable e) {
                MainLogger.get().error("Exception in server bean", e);
            }
        }

        return true;
    }

    private void punish(Message message, ServerBean serverBean, Class<? extends Command> commandClass) {
        Guild guild = message.getGuild();
        Member member = message.getMember();
        CommandProperties commandProperties = Command.getCommandProperties(commandClass);
        String commandTitle = TextManager.getString(serverBean.getLocale(), Category.MODERATION, commandProperties.trigger() + "_title");
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(commandProperties.emoji() + " " + commandTitle);
        designEmbed(message, serverBean.getLocale(), eb);

        try {
            Command command = CommandManager.createCommandByClass(commandClass, serverBean.getLocale(), serverBean.getPrefix());
            Mod.postLog(command, eb, guild, member).thenRun(() -> {
                try {
                    Mod.insertWarning(serverBean.getLocale(), guild, member, guild.getSelfMember(), commandTitle, withAutoActions(message, serverBean.getLocale()));
                } catch (ExecutionException e) {
                    MainLogger.get().error("Error when creating command instance");
                }
            });
        } catch (ExecutionException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            MainLogger.get().error("Error when creating command instance");
        }
    }

    protected abstract boolean withAutoActions(Message message, Locale locale);

    protected abstract void designEmbed(Message message, Locale locale, EmbedBuilder eb);

    protected abstract Class<? extends Command> getCommandClass();

    protected abstract boolean checkCondition(Message message);

}
