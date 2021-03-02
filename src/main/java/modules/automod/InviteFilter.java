package modules.automod;

import commands.Command;
import commands.runnables.moderationcategory.InviteFilterCommand;
import constants.AssetIds;
import constants.Category;
import constants.Permission;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.cache.InviteCache;
import core.utils.DiscordUtil;
import core.utils.PermissionUtil;
import mysql.modules.spblock.DBSPBlock;
import mysql.modules.spblock.SPBlockBean;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.util.logging.ExceptionLogger;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

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
                !spBlockBean.getIgnoredChannelIds().contains(message.getServerTextChannel().get().getId()) &&
                !PermissionUtil.hasAdminPermissions(message.getServer().get(), message.getUserAuthor().get())
        ) {
            ArrayList<String> inviteLinks = DiscordUtil.filterServerInviteLinks(message.getContent());
            if (inviteLinks.size() > 0) {
                return inviteLinks.stream()
                        .map(str -> {
                            String[] parts = str.split("/");
                            return parts[parts.length - 1];
                        })
                        .map(inviteCode -> InviteCache.getInstance().getInviteByCode(inviteCode))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .anyMatch(invite -> invite.getServerId() != message.getServer().get().getId() && invite.getServerId() != AssetIds.SUPPORT_SERVER_ID);
            }
        }

        return false;
    }

}
