package commands.runnables.utilitycategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.modals.ModalMediator;
import core.utils.StringUtil;
import mysql.hibernate.entity.guild.BirthdayEntity;
import mysql.hibernate.entity.guild.BirthdayUserEntryEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@CommandProperties(
        trigger = "birthday",
        emoji = "🎁",
        executableWithoutArgs = true
)
public class BirthdayCommand extends NavigationAbstract {

    public static final String[] TIME_ZONES = ZoneId.getAvailableZoneIds().stream().sorted().toArray(String[]::new);
    public static final String[] TIME_ZONE_LABELS = ZoneId.getAvailableZoneIds().stream().map(s -> s.replace("_", " ")).sorted().toArray(String[]::new);

    public static final int STATE_SET_TIME_ZONE = 1;

    public BirthdayCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        if (!getGuildEntity().getBirthday().getActive()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("disabled"));
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return true;
        }

        registerNavigationListener(event.getMember());
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        BirthdayEntity birthday = getGuildEntity().getBirthday();
        BirthdayUserEntryEntity userEntry = birthday.getUserEntries().get(event.getUser().getIdLong());

        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                if (userEntry != null && userEntry.getTriggered() != null) {
                    setLog(LogStatus.FAILURE, getString("log_nottoday"));
                    return true;
                }

                String dayId = "day";
                TextInput textDay = TextInput.create(dayId, getString("home_date_day"), TextInputStyle.SHORT)
                        .setValue(userEntry != null && userEntry.getDay() != null ? StringUtil.numToString(userEntry.getDay()) : null)
                        .setRequiredRange(0, 2)
                        .setRequired(false)
                        .build();

                String monthId = "month";
                TextInput textMonth = TextInput.create(monthId, getString("home_date_month"), TextInputStyle.SHORT)
                        .setValue(userEntry != null && userEntry.getMonth() != null ? StringUtil.numToString(userEntry.getMonth()) : null)
                        .setRequiredRange(0, 2)
                        .setRequired(false)
                        .build();

                Modal modal = ModalMediator.createDrawableCommandModal(this, getString("home_adjust_date_title"), e -> {
                            BirthdayEntity newBirthday = getGuildEntity().getBirthday();
                            BirthdayUserEntryEntity newUserEntry = newBirthday.getUserEntries().get(e.getUser().getIdLong());

                            String dayStr = e.getValue(dayId).getAsString();
                            String monthStr = e.getValue(monthId).getAsString();

                            if (dayStr.isEmpty() || monthStr.isEmpty()) {
                                if (newUserEntry != null) {
                                    newBirthday.beginTransaction();
                                    newUserEntry.setDay(null);
                                    newUserEntry.setMonth(null);
                                    newBirthday.commitTransaction();
                                }

                                setLog(LogStatus.SUCCESS, getString("log_setdate"));
                                return null;
                            }

                            if (!StringUtil.stringIsInt(dayStr) || !StringUtil.stringIsInt(monthStr)) {
                                setLog(LogStatus.FAILURE, getString("error_invaliddate"));
                                return null;
                            }

                            int month = Integer.parseInt(monthStr);
                            if (month < 1 || month > 12) {
                                setLog(LogStatus.FAILURE, getString("error_invaliddate"));
                                return null;
                            }

                            int day = Integer.parseInt(dayStr);
                            int daysInMonth = YearMonth.of(2000, month).lengthOfMonth();
                            if (day < 1 || day > daysInMonth) {
                                setLog(LogStatus.FAILURE, getString("error_invaliddate"));
                                return null;
                            }

                            newBirthday.beginTransaction();
                            if (newUserEntry == null) {
                                newUserEntry = new BirthdayUserEntryEntity();
                                newBirthday.getUserEntries().put(e.getUser().getIdLong(), newUserEntry);
                            }
                            newUserEntry.setDay(day);
                            newUserEntry.setMonth(month);
                            newBirthday.commitTransaction();

                            setLog(LogStatus.SUCCESS, getString("log_setdate"));
                            return null;
                        }).addComponents(ActionRow.of(textDay), ActionRow.of(textMonth))
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case 1 -> {
                if (userEntry != null && userEntry.getTriggered() != null) {
                    setLog(LogStatus.FAILURE, getString("log_nottoday"));
                    return true;
                }

                setState(STATE_SET_TIME_ZONE);
                return true;
            }
        }
        return false;
    }

    @ControllerButton(state = STATE_SET_TIME_ZONE)
    public boolean onButtonTimeZone(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
        } else {
            BirthdayEntity birthday = getGuildEntity().getBirthday();
            BirthdayUserEntryEntity userEntry = birthday.getUserEntries().get(event.getUser().getIdLong());

            if (userEntry != null && userEntry.getTriggered() != null) {
                setLog(LogStatus.FAILURE, getString("log_nottoday"));
                setState(DEFAULT_STATE);
                return true;
            }

            birthday.beginTransaction();
            if (userEntry == null) {
                userEntry = new BirthdayUserEntryEntity();
                birthday.getUserEntries().put(event.getUser().getIdLong(), userEntry);
            }
            userEntry.setTimeZone(TIME_ZONES[i]);
            birthday.commitTransaction();

            setLog(LogStatus.SUCCESS, getString("log_settimezone"));
            setState(DEFAULT_STATE);
        }
        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) {
        setComponents(getString("home_options").split("\n"));

        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        BirthdayUserEntryEntity userEntry = getGuildEntity().getBirthday().getUserEntries().get(member.getIdLong());
        if (userEntry == null) {
            userEntry = new BirthdayUserEntryEntity();
        }

        Integer day = userEntry.getDay();
        Integer month = userEntry.getMonth();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM", getLocale());
        String formattedDate = month != null && day != null ? LocalDate.of(2000, month, day).format(formatter) : null;
        if (formattedDate != null && day == 29 && month == 2) {
            formattedDate = getString("home_leapyeardate", formattedDate, LocalDate.of(2000, 3, 1).format(formatter));
        }

        return EmbedFactory.getEmbedDefault(this, getString("home_desc"))
                .addField(getString("home_date"), formattedDate != null ? formattedDate : notSet, true)
                .addField(getString("home_timezone"), userEntry.getTimeZoneEffectively().replace("_", " "), true);
    }

    @Draw(state = STATE_SET_TIME_ZONE)
    public EmbedBuilder drawTimeZone(Member member) {
        setComponents(TIME_ZONE_LABELS);
        return EmbedFactory.getEmbedDefault(this, getString("timezone_desc"), getString("timezone_title"));
    }

}
