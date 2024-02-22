package commands.runnables.moderationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.CustomObservableList;
import core.EmbedFactory;
import core.mention.Mention;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import javafx.util.Pair;
import mysql.modules.warning.DBServerWarnings;
import mysql.modules.warning.ServerWarningSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.util.Locale;

@CommandProperties(
        trigger = "warnremove",
        emoji = "\uD83D\uDDD1",
        userGuildPermissions = Permission.KICK_MEMBERS,
        executableWithoutArgs = false,
        requiresFullMemberCache = true,
        aliases = { "unwarn", "removewarn" }
)
public class WarnRemoveCommand extends WarnCommand {

    private final int MAX = 9999;

    private int n = 1;

    public WarnRemoveCommand(Locale locale, String prefix) {
        super(locale, prefix, false, false, true, true, false, false);
    }

    @Override
    protected boolean setUserListAndReason(CommandEvent event, String args) throws Throwable {
        for (String part : args.split(" ")) {
            if (StringUtil.stringIsLong(part)) {
                long value = Math.min(Long.parseLong(part), MAX);
                if (value > 0 && value < MAX) {
                    args = args.replaceAll("(^| )" + part + "($| )", " ").trim();
                    n = (int) value;
                    break;
                }
            } else if (part.equalsIgnoreCase("all")) {
                n = MAX;
                args = args.replace("all", "");
                break;
            }
        }

        return super.setUserListAndReason(event, args);
    }

    @Override
    public void userActionPrepareExecution(User target, String reason, long durationMinutes, int amount) {
        super.userActionPrepareExecution(target, reason, durationMinutes, amount);
        this.n = Math.min(MAX, amount);
    }

    @Override
    protected void process(Guild guild, User target, String reason) throws Throwable {
        CustomObservableList<ServerWarningSlot> serverWarningsSlots = DBServerWarnings.getInstance().retrieve(new Pair<>(guild.getIdLong(), target.getIdLong())).getWarnings();
        serverWarningsSlots.remove(Math.max(0, serverWarningsSlots.size() - n), serverWarningsSlots.size());
    }

    @Override
    protected EmbedBuilder getActionEmbed(Member executor, GuildChannel channel) {
        Mention mention = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), getUserList());
        return EmbedFactory.getEmbedDefault(this, getString("action", n > 1, StringUtil.escapeMarkdown(executor.getUser().getAsTag()), getAmountString().toLowerCase(), mention.getMentionText()));
    }

    @Override
    protected EmbedBuilder getConfirmationEmbed() {
        Mention mention = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), getUserList());
        return EmbedFactory.getEmbedDefault(this, getString("confirmaion", n > 1, getAmountString().toLowerCase(), mention.getMentionText()));
    }

    @Override
    protected EmbedBuilder getSuccessEmbed() {
        Mention mention = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), getUserList());
        return EmbedFactory.getEmbedDefault(this, getString("success_description", n > 1, getAmountString(), mention.getMentionText()));
    }

    private String getAmountString() {
        if (n < MAX) {
            return StringUtil.numToString(n);
        } else {
            return getString("all");
        }
    }

}
