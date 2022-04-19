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
import mysql.modules.invitetracking.InviteTrackingSlot;
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

    private enum OrderBy { TOTAL_INVITES, ON_SERVER, RETAINED, ACTIVE }

    private ArrayList<InviteMetrics> inviteMetricsSlots;

    public InvitesTopCommand(Locale locale, String prefix) {
        super(locale, prefix, 10);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        if (DBInviteTracking.getInstance().retrieve(event.getGuild().getIdLong()).isActive()) {
            registerList(event.getMember(), args, getString("orderby").split("\n"));
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
        ArrayList<InviteTrackingSlot> slots = new ArrayList<>(DBInviteTracking.getInstance().retrieve(member.getGuild().getIdLong()).getInviteTrackingSlots().values());
        slots.forEach(slot -> {
                    long userId = slot.getInviterUserId();
                    if (!memberIds.contains(userId) && (userId == 0 || member.getGuild().getMemberById(userId) != null)) {
                        memberIds.add(userId);
                        inviteMetricsSlots.add(InviteTracking.generateInviteMetrics(member.getGuild(), userId));
                    }
                });

        OrderBy orderByEnum = OrderBy.values()[orderBy];
        inviteMetricsSlots.sort((i0, i1) -> Integer.compare(getInviteValue(i1, orderByEnum), getInviteValue(i0, orderByEnum)));
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

        OrderBy orderByEnum = OrderBy.values()[orderBy];
        int value = getInviteValue(inviteMetrics, orderByEnum);
        int rank = (int) inviteMetricsSlots.stream()
                .filter(other -> getInviteValue(other, orderByEnum) > value)
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
                        makeThicc(StringUtil.numToString(inviteMetrics.getTotalInvites()), orderByEnum == OrderBy.TOTAL_INVITES),
                        makeThicc(StringUtil.numToString(inviteMetrics.getOnServer()), orderByEnum == OrderBy.ON_SERVER),
                        makeThicc(StringUtil.numToString(inviteMetrics.getRetained()), orderByEnum == OrderBy.RETAINED),
                        makeThicc(StringUtil.numToString(inviteMetrics.getActive()), orderByEnum == OrderBy.ACTIVE)
                )
        );
    }

    @Override
    protected int calculateOrderBy(String args) {
        return switch (args.replace("_", "")) {
            case "total" -> OrderBy.TOTAL_INVITES.ordinal();
            case "onserver", "server" -> OrderBy.ON_SERVER.ordinal();
            case "retained" -> OrderBy.RETAINED.ordinal();
            case "active" -> OrderBy.ACTIVE.ordinal();
            default -> -1;
        };
    }

    @Override
    protected EmbedBuilder postProcessEmbed(EmbedBuilder eb, int orderBy) {
        return eb.setDescription(getString("desc"));
    }

    private int getInviteValue(InviteMetrics inviteMetrics, OrderBy orderBy) {
        return switch (orderBy) {
            case TOTAL_INVITES -> inviteMetrics.getTotalInvites();
            case ON_SERVER -> inviteMetrics.getOnServer();
            case RETAINED -> inviteMetrics.getRetained();
            case ACTIVE -> inviteMetrics.getActive();
        };
    }

    private String makeThicc(String text, boolean thicc) {
        if (thicc) {
            return "**" + text + "**";
        } else {
            return text;
        }
    }

}