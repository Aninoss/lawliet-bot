package commands.runnables.moderationcategory;

import java.time.Duration;
import java.util.Locale;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.mention.Mention;
import core.mention.MentionValue;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.Mute;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.TimeFormat;

@CommandProperties(
        trigger = "mute",
        botGuildPermissions = Permission.MODERATE_MEMBERS,
        userGuildPermissions = Permission.MODERATE_MEMBERS,
        emoji = "🛑",
        executableWithoutArgs = false,
        releaseDate = { 2021, 4, 16 },
        requiresFullMemberCache = true,
        aliases = { "chmute", "channelmute", "timeout" }
)
public class MuteCommand extends WarnCommand {

    private long minutes = 0;
    private final boolean setMute;

    public MuteCommand(Locale locale, String prefix) {
        this(locale, prefix, true);
    }

    public MuteCommand(Locale locale, String prefix, boolean setMute) {
        super(locale, prefix, false, false, setMute, true);
        this.setMute = setMute;
    }

    @Override
    protected boolean setUserListAndReason(CommandEvent event, String args) throws Throwable {
        if (setMute) {
            MentionValue<Long> mention = MentionUtil.getTimeMinutes(args);
            this.minutes = mention.getValue();
            return super.setUserListAndReason(event, mention.getFilteredArgs());
        } else {
            return super.setUserListAndReason(event, args);
        }
    }

    @Override
    protected void process(Guild guild, User target, String reason) {
        if (setMute) {
            Mute.mute(guild, target, minutes, reason);
        } else {
            Mute.unmute(guild, target, reason);
        }
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
