package modules.automod;

import commands.Category;
import commands.Command;
import commands.runnables.moderationcategory.InviteFilterCommand;
import constants.AssetIds;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.cache.InviteCache;
import core.utils.BotPermissionUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.guild.InviteFilterEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class InviteFilter extends AutoModAbstract {

    private final InviteFilterEntity inviteFilterEntity;

    public InviteFilter(Message message, GuildEntity guildEntity) {
        super(message, guildEntity);
        inviteFilterEntity = guildEntity.getInviteFilter();
    }

    @Override
    protected boolean withAutoActions(Message message, Locale locale) {
        if (inviteFilterEntity.getAction() == InviteFilterEntity.Action.BAN_USER &&
                PermissionCheckRuntime.botHasPermission(locale, getCommandClass(), message.getGuildChannel(), Permission.BAN_MEMBERS) &&
                BotPermissionUtil.canInteract(message.getGuild(), message.getAuthor())
        ) {
            message.getGuild()
                    .ban(message.getMember(), 0, TimeUnit.DAYS)
                    .reason(TextManager.getString(locale, Category.MODERATION, "invitefilter_auditlog_sp"))
                    .queue();
            return false;
        } else if (inviteFilterEntity.getAction() == InviteFilterEntity.Action.KICK_USER &&
                PermissionCheckRuntime.botHasPermission(locale, getCommandClass(), message.getGuildChannel(), Permission.KICK_MEMBERS) &&
                BotPermissionUtil.canInteract(message.getGuild(), message.getAuthor())
        ) {
            message.getGuild()
                    .kick(message.getMember(), TextManager.getString(locale, Category.MODERATION, "invitefilter_auditlog_sp"))
                    .queue();
            return false;
        }

        return true;
    }

    @Override
    protected void designEmbed(Message message, Locale locale, EmbedBuilder eb) {
        String content = message.getContentRaw();
        for (String invite : message.getInvites()) {
            content = content.replace("https://discord.gg/", "discord.gg/")
                    .replace(invite, "\\*".repeat(6));
        }

        eb.setDescription(TextManager.getString(locale, Category.MODERATION, "invitefilter_log", StringUtil.escapeMarkdown(message.getAuthor().getAsTag())))
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
    protected boolean checkCondition(Message message) {
        if (inviteFilterEntity.getActive() &&
                !inviteFilterEntity.getExcludedMemberIds().contains(message.getAuthor().getIdLong()) &&
                !inviteFilterEntity.getExcludedChannelIds().contains(message.getChannel().getIdLong()) &&
                !BotPermissionUtil.can(message.getMember(), Permission.ADMINISTRATOR)
        ) {
            List<String> inviteLinks = message.getInvites();
            if (inviteLinks.size() > 0) {
                return inviteLinks.stream()
                        .map(InviteCache::getInviteByCode)
                        .map(future -> {
                            try {
                                return future.get();
                            } catch (InterruptedException | ExecutionException e) {
                                //Ignore
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .anyMatch(invite -> invite.getGuild() != null &&
                                invite.getGuild().getIdLong() != message.getGuild().getIdLong() &&
                                invite.getGuild().getIdLong() != AssetIds.SUPPORT_SERVER_ID
                        );
            }
        }

        return false;
    }

}
