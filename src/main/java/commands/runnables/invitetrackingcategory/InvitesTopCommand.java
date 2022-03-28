package commands.runnables.invitetrackingcategory;

import java.util.*;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.ListAbstract;
import constants.Emojis;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.StringUtil;
import javafx.util.Pair;
import modules.invitetracking.InviteMetrics;
import modules.invitetracking.InviteTracking;
import mysql.modules.invitetracking.DBInviteTracking;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "invtop",
        emoji = "🏆",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        releaseDate = { 2022, 4, 3 },
        aliases = { "invitestop", "invitetop", "leaderboard", "leaderboards" }
)
public class InvitesTopCommand extends ListAbstract {

    private ArrayList<InviteMetrics> inviteMetricsSlots;

    public InvitesTopCommand(Locale locale, String prefix) {
        super(locale, prefix, 10);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        if (DBInviteTracking.getInstance().retrieve(event.getGuild().getIdLong()).isActive()) {
            registerList(event.getMember(), args);
            return true;
        } else {
            EmbedBuilder eb = EmbedFactory.getEmbedError(
                    this,
                    TextManager.getString(getLocale(), TextManager.GENERAL, "invites_notenabled_description").replace("{PREFIX}", getPrefix()),
                    TextManager.getString(getLocale(), TextManager.GENERAL, "invites_notenabled_title")
            );
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }
    }

    @Override
    protected int configure(Member member, int orderBy) throws Throwable {
        inviteMetricsSlots = new ArrayList<>();
        HashSet<Long> memberIds = new HashSet<>();
        DBInviteTracking.getInstance().retrieve(member.getGuild().getIdLong()).getInviteTrackingSlots().values()
                .forEach(slot -> {
                    long userId = slot.getInviterUserId();
                    if (!memberIds.contains(userId)) {
                        memberIds.add(userId);
                        inviteMetricsSlots.add(InviteTracking.generateInviteMetrics(member.getGuild(), userId));
                    }
                });

        inviteMetricsSlots.sort(Collections.reverseOrder());
        return inviteMetricsSlots.size();
    }

    @Override
    protected Pair<String, String> getEntry(int i, int orderBy) {
        InviteMetrics inviteMetrics = inviteMetricsSlots.get(i);
        long memberId = inviteMetrics.getMemberId();
        String userString;
        if (memberId != 0) {
            Optional<Member> memberOpt = inviteMetrics.getMember();
            userString = memberOpt
                    .map(Member::getEffectiveName)
                    .orElse(TextManager.getString(getLocale(), TextManager.GENERAL, "nouser", String.valueOf(memberId)));
            userString = StringUtil.escapeMarkdown(userString);
        } else {
            userString = TextManager.getString(getLocale(), TextManager.GENERAL, "invites_vanity");
        }

        int rank = (int) inviteMetricsSlots.stream()
                .filter(other -> inviteMetrics.compareTo(other) < 0)
                .count() + 1;
        String rankString = switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> getString("stringrank", String.valueOf(rank));
        };

        return new Pair<>(
                Emojis.ZERO_WIDTH_SPACE + "\n" + getString(
                        "template_title",
                        rankString,
                        userString
                ),
                getString(
                        "template_value",
                        Emojis.FULL_SPACE_UNICODE,
                        StringUtil.numToString(inviteMetrics.getTotalInvites()),
                        StringUtil.numToString(inviteMetrics.getOnServer()),
                        StringUtil.numToString(inviteMetrics.getRetained()),
                        StringUtil.numToString(inviteMetrics.getActive())
                )
        );
    }

    @Override
    protected EmbedBuilder postProcessEmbed(EmbedBuilder eb, int orderBy) {
        return eb.setDescription(getString("desc"));
    }

}