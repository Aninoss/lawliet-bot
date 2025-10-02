package modules.automod;

import commands.Category;
import commands.Command;
import commands.runnables.moderationcategory.InviteFilterCommand;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.guild.InviteFilterEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class InviteFilter extends AutoModAbstract {

    private final InviteFilterEntity inviteFilterEntity;

    public InviteFilter(Message message, GuildEntity guildEntity) {
        super(message, guildEntity);
        inviteFilterEntity = guildEntity.getInviteFilter();
    }

    @Override
    protected boolean willBanMember(Message message, Member member, Locale locale) {
        return inviteFilterEntity.getAction() == InviteFilterEntity.Action.BAN_USER &&
                PermissionCheckRuntime.botHasPermission(locale, getCommandClass(), message.getGuildChannel(), Permission.BAN_MEMBERS) &&
                member.getGuild().getSelfMember().canInteract(member);
    }

    @Override
    protected boolean withAutoActions(Message message, Member member, Locale locale) {
        if (willBanMember(message, member, locale)) {
            message.getGuild()
                    .ban(member, 0, TimeUnit.DAYS)
                    .reason(TextManager.getString(locale, Category.MODERATION, "invitefilter_auditlog_sp"))
                    .queue();
            return false;
        } else if (inviteFilterEntity.getAction() == InviteFilterEntity.Action.KICK_USER &&
                PermissionCheckRuntime.botHasPermission(locale, getCommandClass(), message.getGuildChannel(), Permission.KICK_MEMBERS) &&
                member.getGuild().getSelfMember().canInteract(member)
        ) {
            message.getGuild()
                    .kick(member)
                    .reason(TextManager.getString(locale, Category.MODERATION, "invitefilter_auditlog_sp"))
                    .queue();
            return false;
        }

        return true;
    }

    @Override
    protected void designEmbed(Message message, Member member, Locale locale, EmbedBuilder eb) {
        String content = JDAUtil.combineMessageContentRaw(message);
        for (String invite : message.getInvites()) {
            content = content.replace("https://discord.gg/", "discord.gg/")
                    .replace(invite, "\\*".repeat(6));
        }

        eb.setDescription(TextManager.getString(locale, Category.MODERATION, "invitefilter_log", StringUtil.escapeMarkdown(member.getUser().getName())))
                .addField(TextManager.getString(locale, Category.MODERATION, "invitefilter_state0_maction"), TextManager.getString(locale, Category.MODERATION, "invitefilter_state0_mactionlist").split("\n")[inviteFilterEntity.getAction().ordinal()], true)
                .addField(TextManager.getString(locale, Category.MODERATION, "invitefilter_log_channel"), message.getChannel().getAsMention(), true)
                .addField(TextManager.getString(locale, Category.MODERATION, "invitefilter_log_content"), StringUtil.shortenString(content, 1024), false);

        for (Long userId : inviteFilterEntity.getLogReceiverUserIds()) {
            if (userId != message.getGuild().getSelfMember().getIdLong()) {
                JDAUtil.openPrivateChannel(message.getJDA(), userId)
                        .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()))
                        .queue();
            }
        }
    }

    @Override
    protected Class<? extends Command> getCommandClass() {
        return InviteFilterCommand.class;
    }

    @Override
    protected boolean checkCondition(Message message, Member member) {
        return inviteFilterEntity.getActive() &&
                !inviteFilterEntity.getExcludedMemberIds().contains(member.getIdLong()) &&
                !JDAUtil.collectionContainsChannelOrParent(inviteFilterEntity.getExcludedChannelIds(), message.getChannel()) &&
                !BotPermissionUtil.can(member, Permission.ADMINISTRATOR) &&
                !message.getInvites().isEmpty();
    }

}
