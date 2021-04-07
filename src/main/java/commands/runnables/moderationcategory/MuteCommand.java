package commands.runnables.moderationcategory;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import commands.listeners.CommandProperties;
import constants.Category;
import core.EmbedFactory;
import core.TextManager;
import core.atomicassets.AtomicRole;
import core.mention.Mention;
import core.mention.MentionValue;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import modules.schedulers.ServerMuteScheduler;
import mysql.modules.moderation.DBModeration;
import mysql.modules.moderation.ModerationBean;
import mysql.modules.servermute.DBServerMute;
import mysql.modules.servermute.ServerMuteSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

@CommandProperties(
        trigger = "mute",
        botGuildPermissions = Permission.MANAGE_ROLES,
        userGuildPermissions = Permission.MANAGE_ROLES,
        emoji = "🛑",
        executableWithoutArgs = false,
        releaseDate = { 2021, 04, 15 },
        aliases = { "chmute", "channelmute" }
)
public class MuteCommand extends WarnCommand {

    private long minutes = 0;
    private final boolean giveRole;
    private AtomicRole atomicRole;

    public MuteCommand(Locale locale, String prefix) {
        this(locale, prefix, true, true);
    }

    public MuteCommand(Locale locale, String prefix, boolean sendWarning, boolean giveRole) {
        super(locale, prefix, sendWarning, false, true);
        this.giveRole = giveRole;
    }

    @Override
    protected boolean setUserListAndReason(GuildMessageReceivedEvent event, String args) throws Throwable {
        ModerationBean moderationBean = DBModeration.getInstance().retrieve(event.getGuild().getIdLong());
        Optional<Role> muteRoleOpt = moderationBean.getMuteRole();
        if (muteRoleOpt.isEmpty()) {
            event.getChannel()
                    .sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.MODERATION, "mute_norole", getPrefix())).build())
                    .queue();
            return false;
        }

        if (!event.getGuild().getSelfMember().canInteract(muteRoleOpt.get())) {
            event.getChannel()
                    .sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role", false, muteRoleOpt.get().getAsMention())).build())
                    .queue();
            return false;
        }

        this.atomicRole = new AtomicRole(event.getGuild().getIdLong(), muteRoleOpt.get().getIdLong());
        if (giveRole) {
            MentionValue<Long> mention = MentionUtil.getTimeMinutesExt(args);
            this.minutes = mention.getValue();
            return super.setUserListAndReason(event, mention.getFilteredArgs());
        } else {
            return super.setUserListAndReason(event, args);
        }
    }

    @Override
    protected void process(Guild guild, User target, String reason) {
        if (giveRole) {
            Instant expiration = minutes > 0 ? Instant.now().plus(Duration.ofMinutes(minutes)) : null;
            ServerMuteSlot serverMuteSlot = new ServerMuteSlot(guild.getIdLong(), target.getIdLong(), expiration);
            DBServerMute.getInstance().retrieve(guild.getIdLong()).put(target.getIdLong(), serverMuteSlot);
            ServerMuteScheduler.getInstance().loadServerMute(serverMuteSlot);
        } else {
            DBServerMute.getInstance().retrieve(guild.getIdLong()).remove(target.getIdLong());
        }

        Optional.ofNullable(guild.getMemberById(target.getIdLong())).ifPresent(member -> {
            atomicRole.get().ifPresent(muteRole -> {
                AuditableRestAction<Void> restAction = giveRole ? guild.addRoleToMember(member, muteRole) : guild.removeRoleFromMember(member, muteRole);
                restAction.reason(getCommandLanguage().getTitle())
                        .queue();
            });
        });
    }

    @Override
    protected boolean canProcess(Member executor, User target) {
        Member member = executor.getGuild().getMember(target);
        return !giveRole || member == null || !BotPermissionUtil.can(member, Permission.ADMINISTRATOR);
    }

    @Override
    protected EmbedBuilder getActionEmbed(Member executor, TextChannel channel) {
        String remaining = TimeUtil.getRemainingTimeString(getLocale(), minutes * 60_000, false);
        Mention mention = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), getUserList());
        return EmbedFactory.getEmbedDefault(this, getString(minutes == 0 ? "action" : "action_temp", mention.isMultiple(), mention.getMentionText(), executor.getAsMention(), StringUtil.escapeMarkdown(channel.getGuild().getName()), remaining));
    }

    @Override
    protected EmbedBuilder getConfirmationEmbed() {
        String remaining = TimeUtil.getRemainingTimeString(getLocale(), minutes * 60_000, false);
        Mention mention = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), getUserList());
        return EmbedFactory.getEmbedDefault(this, getString(minutes == 0 ? "confirmaion" : "confirmaion_temp", mention.getMentionText(), remaining));
    }

    @Override
    protected EmbedBuilder getSuccessEmbed() {
        String remaining = TimeUtil.getRemainingTimeString(getLocale(), minutes * 60_000, false);
        Mention mention = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), getUserList());
        return EmbedFactory.getEmbedDefault(this, getString(minutes == 0 ? "success_description" : "success_description_temp", mention.isMultiple(), mention.getMentionText(), remaining));
    }

}
