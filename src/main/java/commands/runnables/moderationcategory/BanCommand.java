package commands.runnables.moderationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.mention.Mention;
import core.mention.MentionValue;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.schedulers.TempBanScheduler;
import mysql.modules.tempban.DBTempBan;
import mysql.modules.tempban.TempBanData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@CommandProperties(
        trigger = "ban",
        botGuildPermissions = Permission.BAN_MEMBERS,
        userGuildPermissions = Permission.BAN_MEMBERS,
        emoji = "\uD83D\uDEAB",
        executableWithoutArgs = false,
        requiresFullMemberCache = true,
        aliases = { "tempban" }
)
public class BanCommand extends WarnCommand {

    private long minutes;

    public BanCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false, true, true, true, true);
    }

    @Override
    protected boolean setUserListAndReason(CommandEvent event, String args) throws Throwable {
        MentionValue<Long> mention = MentionUtil.getTimeMinutes(args);
        this.minutes = mention.getValue();
        return super.setUserListAndReason(event, mention.getFilteredArgs());
    }

    @Override
    public void userActionPrepareExecution(User target, String reason, long durationMinutes, int amount) {
        super.userActionPrepareExecution(target, reason, durationMinutes, amount);
        this.minutes = durationMinutes;
    }

    @Override
    protected void process(Guild guild, User target, String reason) {
        if (minutes > 0) {
            TempBanData tempBan = new TempBanData(guild.getIdLong(), target.getIdLong(), Instant.now().plus(Duration.ofMinutes(minutes)));
            DBTempBan.getInstance().retrieve(guild.getIdLong()).put(target.getIdLong(), tempBan);
            TempBanScheduler.loadTempBan(tempBan);
        } else {
            DBTempBan.getInstance().retrieve(guild.getIdLong()).remove(target.getIdLong());
        }

        guild.ban(target, 1, TimeUnit.DAYS)
                .reason(reason)
                .submit()
                .exceptionally(e -> {
                    guild.ban(target, 1, TimeUnit.DAYS).queue();
                    return null;
                });
    }

    @Override
    protected EmbedBuilder getActionEmbed(Member executor, GuildChannel channel) {
        String remaining = TimeFormat.DATE_TIME_SHORT.after(Duration.ofMinutes(minutes)).toString();
        Mention mention = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), getUserList());
        return EmbedFactory.getEmbedDefault(this, getString(minutes == 0 ? "action" : "action_temp", mention.isMultiple(), mention.getMentionText(), StringUtil.escapeMarkdown(executor.getUser().getAsTag()), StringUtil.escapeMarkdown(channel.getGuild().getName()), remaining));
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
