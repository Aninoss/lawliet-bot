package commands.runnables.moderationcategory;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.mention.Mention;
import core.mention.MentionValue;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.Mute;
import mysql.modules.moderation.DBModeration;
import mysql.modules.moderation.ModerationData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.utils.TimeFormat;

@CommandProperties(
        trigger = "mute",
        botGuildPermissions = Permission.MANAGE_ROLES,
        userGuildPermissions = Permission.MANAGE_ROLES,
        emoji = "🛑",
        executableWithoutArgs = false,
        releaseDate = { 2021, 4, 16 },
        requiresFullMemberCache = true,
        aliases = { "chmute", "channelmute" }
)
public class MuteCommand extends WarnCommand {

    private long minutes = 0;
    private final boolean giveRole;

    public MuteCommand(Locale locale, String prefix) {
        this(locale, prefix, true);
    }

    public MuteCommand(Locale locale, String prefix, boolean giveRole) {
        super(locale, prefix, false, false, true, true);
        this.giveRole = giveRole;
    }

    @Override
    protected boolean setUserListAndReason(CommandEvent event, String args) throws Throwable {
        ModerationData moderationBean = DBModeration.getInstance().retrieve(event.getGuild().getIdLong());
        Optional<Role> muteRoleOpt = moderationBean.getMuteRole();
        if (muteRoleOpt.isEmpty()) {
            drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.MODERATION, "mute_norole", getPrefix())))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        if (!BotPermissionUtil.canManage(muteRoleOpt.get())) {
            drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role", false, muteRoleOpt.get().getAsMention())))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        if (giveRole) {
            MentionValue<Long> mention = MentionUtil.getTimeMinutes(args);
            this.minutes = mention.getValue();
            return super.setUserListAndReason(event, mention.getFilteredArgs());
        } else {
            return super.setUserListAndReason(event, args);
        }
    }

    @Override
    protected void process(Guild guild, User target, String reason) {
        if (giveRole) {
            Mute.mute(guild, target, minutes, reason);
        } else {
            Mute.unmute(guild, target, reason);
        }
    }

    @Override
    protected boolean canProcessMember(Member executor, User target) {
        return BotPermissionUtil.canInteract(executor, target);
    }

    @Override
    protected boolean canProcessBot(Guild guild, User target) {
        return true;
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
        return EmbedFactory.getEmbedDefault(this, getString(minutes == 0 ? "success_description" : "success_description_temp", mention.isMultiple(), mention.getMentionText(), remaining));
    }

}
