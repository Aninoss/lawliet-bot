package commands.runnables.moderationcategory;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.MemberCacheController;
import core.TextManager;
import core.mention.Mention;
import core.mention.MentionValue;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.Jail;
import mysql.modules.moderation.DBModeration;
import mysql.modules.moderation.ModerationData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.utils.TimeFormat;

@CommandProperties(
        trigger = "jail",
        botGuildPermissions = Permission.MANAGE_ROLES,
        userGuildPermissions = Permission.MANAGE_ROLES,
        emoji = "🔒",
        executableWithoutArgs = false,
        requiresFullMemberCache = true,
        aliases = { "isolate" }
)
public class JailCommand extends WarnCommand {

    private long minutes = 0;
    private final boolean jail;
    private boolean permissionIssues = false;

    public JailCommand(Locale locale, String prefix) {
        this(locale, prefix, false, true);
    }

    public JailCommand(Locale locale, String prefix, boolean includeNotInGuild, boolean jail) {
        super(locale, prefix, false, false, includeNotInGuild, true);
        this.jail = jail;
    }

    @Override
    protected boolean setUserListAndReason(CommandEvent event, String args) throws Throwable {
        ModerationData moderationBean = DBModeration.getInstance().retrieve(event.getGuild().getIdLong());
        Guild guild = event.getGuild();
        List<Role> notManagableRoles = moderationBean.getJailRoleIds().transform(guild::getRoleById, ISnowflake::getIdLong).stream()
                .filter(role -> !BotPermissionUtil.canManage(role))
                .collect(Collectors.toList());

        if (notManagableRoles.size() > 0) {
            Mention mention = MentionUtil.getMentionedStringOfRoles(getLocale(), notManagableRoles);
            drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role", mention.isMultiple(), mention.getMentionText())))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        if (jail) {
            MentionValue<Long> mention = MentionUtil.getTimeMinutes(args);
            this.minutes = mention.getValue();
            return super.setUserListAndReason(event, mention.getFilteredArgs());
        } else {
            return super.setUserListAndReason(event, args);
        }
    }

    @Override
    protected void process(Guild guild, User target, String reason) {
        AtomicBoolean newPermissionIssues = new AtomicBoolean(false);
        if (jail) {
            MemberCacheController.getInstance().loadMember(guild, target.getIdLong()).thenAccept(member -> {
                if (member != null) {
                    newPermissionIssues.set(!Jail.jail(guild, member, minutes, reason));
                }
            });
        } else {
            newPermissionIssues.set(!Jail.unjail(guild, target, reason));
        }

        if (newPermissionIssues.get()) {
            permissionIssues = true;
        }
    }

    @Override
    protected boolean canProcessMember(Member executor, User target) {
        return BotPermissionUtil.canInteract(executor, target);
    }

    @Override
    protected boolean canProcessBot(Guild guild, User target) {
        return guild.getSelfMember().getIdLong() != target.getIdLong();
    }

    @Override
    protected EmbedBuilder getActionEmbed(Member executor, TextChannel channel) {
        String remaining = TimeFormat.DATE_TIME_SHORT.after(Duration.ofMinutes(minutes)).toString();
        Mention mention = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), getUserList());
        return EmbedFactory.getEmbedDefault(this, getString(minutes == 0 ? "action" : "action_temp", mention.isMultiple(), mention.getMentionText(), executor.getAsMention(), StringUtil.escapeMarkdown(channel.getGuild().getName()), remaining));
    }

    @Override
    protected EmbedBuilder getConfirmationEmbed() {
        String remaining = TimeFormat.DATE_TIME_SHORT.after(Duration.ofMinutes(minutes)).toString();
        Mention mention = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), getUserList());
        return EmbedFactory.getEmbedDefault(this, getString(minutes == 0 ? "confirmaion" : "confirmaion_temp", mention.getMentionText(), remaining));
    }

    @Override
    protected EmbedBuilder getSuccessEmbed() {
        String remaining = TimeFormat.DATE_TIME_SHORT.after(Duration.ofMinutes(minutes)).toString();
        Mention mention = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), getUserList());
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString(minutes == 0 ? "success_description" : "success_description_temp", mention.isMultiple(), mention.getMentionText(), remaining));
        if (permissionIssues) {
            EmbedUtil.addLog(eb, LogStatus.WARNING, TextManager.getString(getLocale(), Category.MODERATION, "jail_warning_notallroles", jail));
        }
        return eb;
    }

}
