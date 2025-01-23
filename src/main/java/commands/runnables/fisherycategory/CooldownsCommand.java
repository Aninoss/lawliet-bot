package commands.runnables.fisherycategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnStringSelectMenuListener;
import commands.runnables.FisheryInterface;
import constants.ExternalLinks;
import constants.Language;
import constants.LogStatus;
import core.EmbedFactory;
import core.utils.TimeUtil;
import javafx.util.Pair;
import mysql.hibernate.entity.user.FisheryDmReminderEntity;
import mysql.hibernate.entity.user.UserEntity;
import mysql.modules.survey.DBSurvey;
import mysql.modules.survey.SurveyData;
import mysql.modules.upvotes.DBUpvotes;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "cooldowns",
        emoji = "⏲️",
        executableWithoutArgs = true,
        aliases = { "cooldown", "cd" }
)
public class CooldownsCommand extends Command implements FisheryInterface, OnStringSelectMenuListener {

    private FisheryMemberData fisheryMemberData;

    public CooldownsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(CommandEvent event, String args) throws Throwable {
        this.fisheryMemberData = FisheryUserManager.getGuildData(event.getGuild().getIdLong())
                .getMemberData(event.getMember().getIdLong());
        registerStringSelectMenuListener(event.getMember());
        return true;
    }

    @Override
    public boolean onStringSelectMenu(StringSelectInteractionEvent event) throws Throwable {
        FisheryDmReminderEntity.Type[] types = FisheryDmReminderEntity.Type.values();
        List<Integer> activeTypes = event.getValues().stream()
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        for (int i = 0; i < types.length; i++) {
            FisheryDmReminderEntity.Type type = types[i];
            UserEntity userEntity = getUserEntity();
            Map<FisheryDmReminderEntity.Type, FisheryDmReminderEntity> fisheryDmReminders = userEntity.getFisheryDmReminders();

            boolean newActive = activeTypes.contains(i);
            boolean currentlyActive = fisheryDmReminders.containsKey(type);

            userEntity.beginTransaction();
            if (newActive && !currentlyActive) {
                FisheryDmReminderEntity fisheryDmReminderEntity = new FisheryDmReminderEntity(type, Language.from(getLocale()));
                fisheryDmReminders.put(type, fisheryDmReminderEntity);
            } else if (!newActive && currentlyActive) {
                fisheryDmReminders.remove(type);
            }
            userEntity.commitTransaction();

            setLog(LogStatus.SUCCESS, getString("subset"));
        }
        return true;
    }

    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        FisheryDmReminderEntity.Type[] types = FisheryDmReminderEntity.Type.values();

        StringSelectMenu.Builder builder = StringSelectMenu.create("reminders");
        ArrayList<String> defaultValues = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            FisheryDmReminderEntity.Type type = types[i];
            String property = getString("property_" + type.name().toLowerCase());

            builder.addOption(property, String.valueOf(i));
            if (getUserEntity().getFisheryDmReminders().containsKey(type)) {
                defaultValues.add(String.valueOf(i));
            }
        }
        builder.setRequiredRange(0, 4)
                .setDefaultValues(defaultValues);
        setActionRows(
                ActionRow.of(builder.build()),
                ActionRow.of(Button.of(ButtonStyle.LINK, ExternalLinks.UPVOTE_URL, getString("upvotebutton")))
        );

        String template = getString("template",
                getRemainingTimeDaily(),
                getRemainingTimeWork(),
                getRemainingTimeUpvotes(),
                getRemainingTimeSurvey()
        );

        return EmbedFactory.getEmbedDefault(this, template)
                .addField(getString("dmreminders"), "⬇️ " + getString("dmreminders_desc"), false);
    }

    private String getRemainingTimeDaily() {
        Instant nextDaily = TimeUtil.localDateToInstant(fisheryMemberData.getDailyReceived()).plus(24, ChronoUnit.HOURS);
        return getRemainingTime(nextDaily);
    }

    private String getRemainingTimeWork() {
        Instant nextWork = fisheryMemberData.getNextWork().orElse(Instant.MIN);
        return getRemainingTime(nextWork);
    }

    private String getRemainingTimeUpvotes() {
        Instant lastUpvote = DBUpvotes.getUpvoteSlot(fisheryMemberData.getMemberId()).getLastUpvote().plus(12, ChronoUnit.HOURS);
        return getRemainingTime(lastUpvote);
    }

    private String getRemainingTimeSurvey() {
        SurveyData surveyData = DBSurvey.getInstance().getCurrentSurvey();
        Instant nextSurvey = surveyData.getSecondVotes().containsKey(new Pair<>(fisheryMemberData.getGuildId(), fisheryMemberData.getMemberId())) ? TimeUtil.localDateToInstant(surveyData.getNextDate()) : Instant.MIN;
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
