package modules.automod;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.CommandManager;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.MainLogger;
import core.PermissionCheckRuntime;
import modules.Mod;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import mysql.modules.ticket.DBTicket;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

public abstract class AutoModAbstract {

    private final Message message;

    public AutoModAbstract(Message message) throws ExecutionException {
        this.message = message;
    }

    /*
     * returns true if the message is fine
     */
    public boolean check() {
        boolean isTicketChannel = DBTicket.getInstance().retrieve(message.getGuild().getIdLong()).getTicketChannels()
                .containsKey(message.getTextChannel().getIdLong());
        if (!message.getAuthor().isBot() && !isTicketChannel && checkCondition(message)) {
            try {
                GuildData guildBean = DBGuild.getInstance().retrieve(message.getGuild().getIdLong());
                Class<? extends Command> commandClass = getCommandClass();
                if (PermissionCheckRuntime.botHasPermission(guildBean.getLocale(), commandClass, message.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                    message.delete().queue();
                }
                punish(message, guildBean, commandClass);
                return false;
            } catch (Throwable e) {
                MainLogger.get().error("Exception in server bean", e);
            }
        }

        return true;
    }

    private void punish(Message message, GuildData guildBean, Class<? extends Command> commandClass) {
        Guild guild = message.getGuild();
        Member member = message.getMember();
        CommandProperties commandProperties = Command.getCommandProperties(commandClass);
        String commandTitle = Command.getCommandLanguage(commandClass, guildBean.getLocale()).getTitle();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(commandProperties.emoji() + " " + commandTitle);
        designEmbed(message, guildBean.getLocale(), eb);

        Command command = CommandManager.createCommandByClass(commandClass, guildBean.getLocale(), guildBean.getPrefix());
        Mod.postLogMembers(command, eb, guild, member).thenRun(() -> {
            Mod.insertWarning(guildBean.getLocale(), member, guild.getSelfMember(), commandTitle, withAutoActions(message, guildBean.getLocale()));
        });
    }

    protected abstract boolean withAutoActions(Message message, Locale locale);

    protected abstract void designEmbed(Message message, Locale locale, EmbedBuilder eb);

    protected abstract Class<? extends Command> getCommandClass();

    protected abstract boolean checkCondition(Message message);

}
