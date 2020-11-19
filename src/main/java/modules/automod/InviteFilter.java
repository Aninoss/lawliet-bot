package modules.automod;

import commands.Command;
import commands.runnables.moderationcategory.InviteFilterCommand;
import constants.Category;
import constants.ExternalLinks;
import constants.Permission;
import core.PermissionCheckRuntime;
import core.TextManager;
import mysql.modules.spblock.DBSPBlock;
import mysql.modules.spblock.SPBlockBean;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.invite.RichInvite;
import org.javacord.api.util.logging.ExceptionLogger;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class InviteFilter extends AutoModAbstract {

    private final SPBlockBean spBlockBean;

    public InviteFilter(Message message) throws ExecutionException {
        super(message);
        spBlockBean = DBSPBlock.getInstance().getBean(message.getServer().get().getId());
    }

    @Override
    protected boolean withAutoActions(Message message, Locale locale) {
        if (spBlockBean.getAction() == SPBlockBean.ActionList.BAN_USER &&
                PermissionCheckRuntime.getInstance().botHasPermission(locale, getCommandClass(), message.getServerTextChannel().get(), Permission.BAN_MEMBERS)
        ) {
            message.getServer().get()
                    .banUser(message.getUserAuthor().get(), 0, TextManager.getString(spBlockBean.getServerBean().getLocale(), Category.MODERATION, "invitefilter_auditlog_sp"))
                    .exceptionally(ExceptionLogger.get());
            return false;
        } else if (spBlockBean.getAction() == SPBlockBean.ActionList.KICK_USER &&
                PermissionCheckRuntime.getInstance().botHasPermission(locale, getCommandClass(), message.getServerTextChannel().get(), Permission.KICK_MEMBERS)
        ) {
            message.getServer().get()
                    .kickUser(message.getUserAuthor().get(), TextManager.getString(spBlockBean.getServerBean().getLocale(), Category.MODERATION, "invitefilter_auditlog_sp"))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        return true;
    }

    @Override
    protected void designEmbed(Message message, Locale locale, EmbedBuilder eb) {
        eb.setDescription(TextManager.getString(locale, Category.MODERATION, "invitefilter_log", message.getUserAuthor().get().getDiscriminatedName()))
                .addField(TextManager.getString(locale, Category.MODERATION, "invitefilter_state0_maction"), TextManager.getString(locale, Category.MODERATION, "invitefilter_state0_mactionlist").split("\n")[spBlockBean.getAction().ordinal()], true)
                .addField(TextManager.getString(locale, Category.MODERATION, "invitefilter_log_channel"), message.getServerTextChannel().get().getMentionTag(), true);

        spBlockBean.getLogReceiverUserIds().transform(message.getServer().get()::getMemberById, DiscordEntity::getId).forEach(user -> user.sendMessage(eb));
    }

    @Override
    protected Class<? extends Command> getCommandClass() {
        return InviteFilterCommand.class;
    }

    @Override
    protected boolean checkCondition(Message message) {
        if (spBlockBean.isActive() &&
                !spBlockBean.getIgnoredUserIds().contains(message.getUserAuthor().get().getId()) &&
                !spBlockBean.getIgnoredChannelIds().contains(message.getServerTextChannel().get().getId())
        ) {
            String content = message.getContent();
            content = content.replaceAll("(?i)" + Pattern.quote(ExternalLinks.SERVER_INVITE_URL), "");
            if (contentContainsDiscordLink(content)) {
                try {
                    for (RichInvite richInvite : message.getServer().get().getInvites().get()) {
                        content = content.replace(" ", "").replaceAll("(?i)" + Pattern.quote("discord.gg/" + richInvite.getCode()), "");
                    }
                    return contentContainsDiscordLink(content);
                } catch (ExecutionException | InterruptedException e) {
                    //Ignore
                    return true;
                }
            }
        }

        return false;
    }

    private boolean contentContainsDiscordLink(String string) {
        return string.matches("(.*)(discord\\.gg\\/[A-Za-z0-9]{6,8})|(discord\\.com\\/invite\\/[A-Za-z0-9])|(discordapp\\.com\\/invite\\/[A-Za-z0-9])(.*)");
    }

}
