package commands.runnables.moderationcategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import commands.listeners.CommandProperties;
import commands.runnables.MemberAccountAbstract;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import javafx.util.Pair;
import mysql.modules.warning.DBServerWarnings;
import mysql.modules.warning.GuildWarningsSlot;
import mysql.modules.warning.ServerWarningsBean;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "warnlog",
        emoji = "\uD83D\uDCDD",
        executableWithoutArgs = true,
        aliases = { "warns" }
)
public class WarnLogCommand extends MemberAccountAbstract {

    public WarnLogCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder processMember(GuildMessageReceivedEvent event, Member member, boolean memberIsAuthor, String args) {
        ServerWarningsBean serverWarningsBean = DBServerWarnings.getInstance().retrieve(new Pair<>(event.getGuild().getIdLong(), member.getIdLong()));

        StringBuilder latestWarnings = new StringBuilder();

        List<GuildWarningsSlot> slots = serverWarningsBean.getLatest(3);
        Collections.reverse(slots);
        for (GuildWarningsSlot serverWarningsSlot : slots) {
            Optional<Member> requestor = serverWarningsSlot.getRequesterMember();
            Optional<String> reason = serverWarningsSlot.getReason();
            String userString = requestor.map(IMentionable::getAsMention).orElseGet(() -> TextManager.getString(getLocale(), TextManager.GENERAL, "unknown_user"));
            String timeDiffString = TimeUtil.getRemainingTimeString(getLocale(), Instant.now(), serverWarningsSlot.getTime(), true);
            latestWarnings.append(getString("latest_slot", reason.isPresent(), userString, timeDiffString, reason.orElse(getString("noreason"))));
        }

        String latestWarningsString = latestWarnings.toString();
        if (latestWarningsString.isEmpty()) {
            latestWarningsString = TextManager.getString(getLocale(), TextManager.GENERAL, "empty");
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .setTitle("")
                .setAuthor(getString("author", getCommandProperties().emoji(), member.getEffectiveName()))
                .setThumbnail(member.getUser().getEffectiveAvatarUrl());
        eb.addField(getString("latest"), latestWarningsString, false);
        eb.addField(getString("amount"), getString(
                "amount_template",
                StringUtil.numToString(serverWarningsBean.getAmountLatest(24, ChronoUnit.HOURS).size()),
                StringUtil.numToString(serverWarningsBean.getAmountLatest(7, ChronoUnit.DAYS).size()),
                StringUtil.numToString(serverWarningsBean.getAmountLatest(30, ChronoUnit.DAYS).size()),
                StringUtil.numToString(serverWarningsBean.getWarnings().size())
        ), false);

        return eb;
    }

}
