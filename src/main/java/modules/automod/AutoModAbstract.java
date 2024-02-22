package modules.automod;

import commands.Command;
import commands.CommandManager;
import commands.listeners.CommandProperties;
import constants.ExceptionIds;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.MainLogger;
import core.PermissionCheckRuntime;
import modules.Mod;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import java.util.Locale;

public abstract class AutoModAbstract {

    private final Message message;
    private final GuildEntity guildEntity;

    public AutoModAbstract(Message message, GuildEntity guildEntity) {
        this.message = message;
        this.guildEntity = guildEntity;
    }

    /*
     * returns true if the message is fine
     */
    public boolean check() {
        boolean isTicketChannel = guildEntity.getTickets().getTicketChannels()
                .containsKey(message.getChannel().getIdLong());

        if (!message.getAuthor().isBot() && !isTicketChannel && checkCondition(message)) {
            try {
                Class<? extends Command> commandClass = getCommandClass();
                if (PermissionCheckRuntime.botHasPermission(guildEntity.getLocale(), commandClass, message.getGuildChannel(), Permission.MESSAGE_MANAGE)) {
                    message.delete().submit()
                            .exceptionally(ExceptionLogger.get(ExceptionIds.UNKNOWN_MESSAGE, ExceptionIds.UNKNOWN_CHANNEL));
                }
                punish(message, guildEntity, commandClass);
                return false;
            } catch (Throwable e) {
                MainLogger.get().error("Exception in auto mod check", e);
            }
        }

        return true;
    }

    private void punish(Message message, GuildEntity guildEntity, Class<? extends Command> commandClass) {
        Guild guild = message.getGuild();
        Member member = message.getMember();
        CommandProperties commandProperties = Command.getCommandProperties(commandClass);
        Locale locale = guildEntity.getLocale();
        String commandTitle = Command.getCommandLanguage(commandClass, locale).getTitle();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(commandProperties.emoji() + " " + commandTitle);
        designEmbed(message, locale, eb);

        Command command = CommandManager.createCommandByClass(commandClass, locale, guildEntity.getPrefix());
        Mod.postLogMembers(command, eb, guildEntity.getModeration(), member).join();
        Mod.insertWarning(guildEntity, member, guild.getSelfMember(), commandTitle,
                withAutoActions(message, locale)
        );
    }

    protected abstract boolean withAutoActions(Message message, Locale locale);

    protected abstract void designEmbed(Message message, Locale locale, EmbedBuilder eb);

    protected abstract Class<? extends Command> getCommandClass();

    protected abstract boolean checkCondition(Message message);

}
