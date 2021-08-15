package commands.runnables.fisherycategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import commands.runnables.FisheryInterface;
import constants.ExternalLinks;
import constants.LogStatus;
import core.CustomObservableMap;
import core.EmbedFactory;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import javafx.util.Pair;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import mysql.modules.subs.DBSubs;
import mysql.modules.subs.SubSlot;
import mysql.modules.survey.DBSurvey;
import mysql.modules.survey.SurveyData;
import mysql.modules.upvotes.DBUpvotes;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.utils.TimeFormat;

@CommandProperties(
        trigger = "cooldowns",
        emoji = "⏲️",
        executableWithoutArgs = true,
        aliases = { "cooldown", "cd" }
)
public class CooldownsCommand extends Command implements FisheryInterface, OnButtonListener {

    private FisheryMemberData fisheryMemberData;

    public CooldownsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(GuildMessageReceivedEvent event, String args) throws Throwable {
        this.fisheryMemberData = DBFishery.getInstance().retrieve(event.getGuild().getIdLong())
                .getMemberData(event.getMember().getIdLong());
        registerButtonListener(event.getMember());
        return true;
    }

    @Override
    public boolean onButton(ButtonClickEvent event) throws Throwable {
        int i;
        if (StringUtil.stringIsInt(event.getComponentId()) && ((i = Integer.parseInt(event.getComponentId())) >= 0 && i < DBSubs.Command.values().length)) {
            DBSubs.Command command = DBSubs.Command.values()[i];
            CustomObservableMap<Long, SubSlot> slotMap = DBSubs.getInstance().retrieve(command);
            boolean newActive = !slotMap.containsKey(event.getMember().getIdLong());
            if (newActive) {
                SubSlot subSlot = new SubSlot(command, event.getMember().getIdLong(), getLocale());
                slotMap.put(subSlot.getUserId(), subSlot);
            } else {
                slotMap.remove(event.getMember().getIdLong());
            }
            setLog(LogStatus.SUCCESS, getString("subset", newActive, getString("property_" + command.name().toLowerCase())));
            return true;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        Button[] buttons = new Button[5];
        StringBuilder sb = new StringBuilder();
        DBSubs.Command[] commands = DBSubs.Command.values();

        for (int i = 0; i < commands.length; i++) {
            DBSubs.Command command = commands[i];
            boolean active = DBSubs.getInstance().retrieve(command).containsKey(member.getIdLong());
            String property = getString("property_" + commands[i].name().toLowerCase());

            sb.append(getString("dmreminders_desc", StringUtil.getEmojiForBoolean(getTextChannel().get(), active), property))
                    .append("\n");
            buttons[i] = Button.of(ButtonStyle.PRIMARY, String.valueOf(i), getString("dmreminders_subscribe", active, property));
        }
        buttons[4] = Button.of(ButtonStyle.LINK, ExternalLinks.UPVOTE_URL, getString("upvotebutton"));
        setButtons(buttons);

        String template = getString("template",
                getRemainingTimeDaily(),
                getRemainingTimeWork(),
                getRemainingTimeUpvotes(),
                getRemainingTimeSurvey()
        );

        return EmbedFactory.getEmbedDefault(this, template)
                .addField(getString("dmreminders"), sb.toString(), false);
    }

    private String getRemainingTimeDaily() {
        Instant nextDaily = TimeUtil.localDateToInstant(fisheryMemberData.getDailyReceived()).plus(24, ChronoUnit.HOURS);
        return getRemainingTime(nextDaily);
    }

    private String getRemainingTimeWork() {
        Instant nextWork = fisheryMemberData.getNextWork().orElse(Instant.ofEpochSecond(0));
        return getRemainingTime(nextWork);
    }

    private String getRemainingTimeUpvotes() {
        Instant lastUpvote = DBUpvotes.getInstance().retrieve().getLastUpvote(fisheryMemberData.getMemberId()).plus(12, ChronoUnit.HOURS);
        return getRemainingTime(lastUpvote);
    }

    private String getRemainingTimeSurvey() {
        SurveyData surveyData = DBSurvey.getInstance().getCurrentSurvey();
        Instant nextSurvey = surveyData.getSecondVotes().containsKey(new Pair<>(fisheryMemberData.getGuildId(), fisheryMemberData.getMemberId())) ? TimeUtil.localDateToInstant(surveyData.getNextDate()) : Instant.ofEpochSecond(0);
        return getRemainingTime(nextSurvey);
    }

    private String getRemainingTime(Instant instant) {
        if (Instant.now().isAfter(instant)) {
            return getString("now");
        } else {
            return TimeFormat.RELATIVE.atInstant(instant).toString();
        }
    }

}
