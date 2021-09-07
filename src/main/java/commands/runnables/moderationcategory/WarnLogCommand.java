package commands.runnables.moderationcategory;

import java.time.temporal.ChronoUnit;
import java.util.*;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import commands.runnables.MemberAccountAbstract;
import core.EmbedFactory;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import javafx.util.Pair;
import mysql.modules.warning.DBServerWarnings;
import mysql.modules.warning.ServerWarningSlot;
import mysql.modules.warning.ServerWarningsData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.utils.TimeFormat;

@CommandProperties(
        trigger = "warnlog",
        emoji = "\uD83D\uDCDD",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "warns" }
)
public class WarnLogCommand extends MemberAccountAbstract implements OnButtonListener {

    private static final String BUTTON_ID_PREVIOUS = "prev";
    private static final String BUTTON_ID_NEXT = "next";
    private static final int ENTRIES_PER_PAGE = 3;

    List<ServerWarningSlot> warningSlots;
    private String avatarUrl;
    private String effectiveName;
    private int page = 0;
    private ServerWarningsData serverWarningsData;
    private EmbedBuilder ebCache;

    public WarnLogCommand(Locale locale, String prefix) {
        super(locale, prefix, true);
    }

    @Override
    protected EmbedBuilder processUser(GuildMessageReceivedEvent event, User user, boolean userIsAuthor, String args) throws Throwable {
        Member member = event.getGuild().getMemberById(user.getIdLong());
        serverWarningsData = DBServerWarnings.getInstance().retrieve(new Pair<>(event.getGuild().getIdLong(), user.getIdLong()));
        avatarUrl = user.getEffectiveAvatarUrl();
        effectiveName = member != null ? member.getEffectiveName() : user.getName();
        warningSlots = new ArrayList<>(serverWarningsData.getWarnings());
        Collections.reverse(warningSlots);

        if (StringUtil.stringIsInt(args)) {
            setFound();
            int pageStart = Integer.parseInt(args);
            if (pageStart >= 1) {
                page = Math.min(getPageSize(), pageStart) - 1;
            }
        }

        return draw(member);
    }

    @Override
    protected void sendMessage(Member member, TextChannel channel, EmbedBuilder eb) {
        if (warningSlots.size() > ENTRIES_PER_PAGE) {
            setComponents(
                    Button.of(ButtonStyle.PRIMARY, BUTTON_ID_PREVIOUS, TextManager.getString(getLocale(), TextManager.GENERAL, "list_previous")),
                    Button.of(ButtonStyle.PRIMARY, BUTTON_ID_NEXT, TextManager.getString(getLocale(), TextManager.GENERAL, "list_next"))
            );
            ebCache = eb;
            registerButtonListener(member);
        } else {
            drawMessage(eb);
        }
    }

    @Override
    public boolean onButton(ButtonClickEvent event) throws Throwable {
        if (event.getComponentId().equals(BUTTON_ID_PREVIOUS)) {
            page--;
            if (page < 0) page = getPageSize() - 1;
            return true;
        } else if (event.getComponentId().equals(BUTTON_ID_NEXT)) {
            page++;
            if (page > getPageSize() - 1) page = 0;
            return true;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        if (ebCache != null) {
            EmbedBuilder eb = ebCache;
            ebCache = null;
            setFooter(eb);
            return eb;
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .setTitle(null)
                .setAuthor(getString("author", getCommandProperties().emoji(), effectiveName))
                .setThumbnail(avatarUrl);

        StringBuilder sb = new StringBuilder();
        for (int i = page * ENTRIES_PER_PAGE; i < Math.min(warningSlots.size(), page * ENTRIES_PER_PAGE + ENTRIES_PER_PAGE); i++) {
            ServerWarningSlot serverWarningsSlot = warningSlots.get(i);
            Optional<Member> requestor = serverWarningsSlot.getRequesterMember();
            Optional<String> reason = serverWarningsSlot.getReason();
            String userString = requestor.map(IMentionable::getAsMention).orElseGet(() -> TextManager.getString(getLocale(), TextManager.GENERAL, "unknown_user"));
            String timeDiffString = TimeFormat.DATE_TIME_SHORT.atInstant(serverWarningsSlot.getTime()).toString();
            sb.append(getString("latest_slot", reason.isPresent(), userString, timeDiffString, reason.orElse(getString("noreason"))));
        }
        eb.setDescription(sb.toString());

        eb.addField(getString("amount"), getString(
                "amount_template",
                StringUtil.numToString(serverWarningsData.getAmountLatest(24, ChronoUnit.HOURS).size()),
                StringUtil.numToString(serverWarningsData.getAmountLatest(7, ChronoUnit.DAYS).size()),
                StringUtil.numToString(serverWarningsData.getAmountLatest(30, ChronoUnit.DAYS).size()),
                StringUtil.numToString(serverWarningsData.getWarnings().size())
        ), false);
        setFooter(eb);

        return eb;
    }

    private void setFooter(EmbedBuilder eb) {
        EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "list_footer", String.valueOf(page + 1), String.valueOf(getPageSize())));
    }

    private int getPageSize() {
        return ((warningSlots.size() - 1) / ENTRIES_PER_PAGE) + 1;
    }

}
