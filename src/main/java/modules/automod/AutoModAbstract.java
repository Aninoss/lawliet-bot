package modules.automod;

import commands.Command;
import commands.CommandManager;
import commands.listeners.CommandProperties;
import constants.ExceptionIds;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.MainLogger;
import core.PermissionCheckRuntime;
import modules.moderation.Mod;
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

        Member member = getMember();
        if (member == null || member.getUser().isBot() || isTicketChannel || !checkCondition(message, member)) {
            return true;
        }

        try {
            Class<? extends Command> commandClass = getCommandClass();
            if (PermissionCheckRuntime.botHasPermission(guildEntity.getLocale(), commandClass, message.getGuildChannel(), Permission.MESSAGE_MANAGE)) {
                message.delete().submit()
                        .exceptionally(ExceptionLogger.get(ExceptionIds.UNKNOWN_MESSAGE, ExceptionIds.UNKNOWN_CHANNEL));
            }
            punish(message, member, guildEntity, commandClass);
            return false;
        } catch (Throwable e) {
            MainLogger.get().error("Exception in auto mod check", e);
            return true;
        }
    }

    private void punish(Message message, Member member, GuildEntity guildEntity, Class<? extends Command> commandClass) {
        Guild guild = member.getGuild();
        CommandProperties commandProperties = Command.getCommandProperties(commandClass);
        Locale locale = guildEntity.getLocale();
        String commandTitle = Command.getCommandLanguage(commandClass, locale).getTitle();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(commandProperties.emoji() + " " + commandTitle);
        designEmbed(message, member, locale, eb);

        Command command = CommandManager.createCommandByClass(commandClass, locale, guildEntity.getPrefix());
        Mod.postLogMembers(command, eb, guildEntity.getModeration(), member, willBanMember(message, member, locale)).join();
        Mod.insertWarning(guildEntity, member, guild.getSelfMember(), commandTitle,
                withAutoActions(message, member, locale)
        );
    }

    protected boolean willBanMember(Message message, Member member, Locale locale) {
        return false;
    }

    protected abstract boolean withAutoActions(Message message, Member member, Locale locale);

    protected abstract void designEmbed(Message message, Member member, Locale locale, EmbedBuilder eb);

    protected abstract Class<? extends Command> getCommandClass();

    protected abstract boolean checkCondition(Message message, Member member);

    private Member getMember() {
        Member member = message.getMember();
        if (member != null && !member.getUser().isBot()) {
            return member;
        }
        if (message.getInteraction() != null) {
            return message.getInteraction().getMember();
        }
        return null;
    }

}
