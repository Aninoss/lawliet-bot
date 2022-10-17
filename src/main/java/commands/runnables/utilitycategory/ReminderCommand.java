package commands.runnables.utilitycategory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;
import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnStaticButtonListener;
import constants.Emojis;
import constants.LogStatus;
import core.*;
import core.cache.ServerPatreonBoostCache;
import core.mention.MentionValue;
import core.utils.*;
import modules.schedulers.ReminderScheduler;
import mysql.modules.reminders.DBReminders;
import mysql.modules.reminders.ReminderData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.BaseGuildMessageChannel;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "reminder",
        botChannelPermissions = { Permission.MESSAGE_EXT_EMOJI },
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "⏲️",
        executableWithoutArgs = false,
        releaseDate = { 2020, 10, 21 },
        aliases = { "remindme", "remind", "reminders", "schedule", "scheduler", "schedulers" }
)
public class ReminderCommand extends Command implements OnStaticButtonListener {

    public static final String CANCEL_ID = "cancel";
    public static final String REPEAT_ID = "repeat";

    public ReminderCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        BaseGuildMessageChannel channel;
        CommandUtil.ChannelResponse response = CommandUtil.differentChannelExtract(this, event, args);
        if (response != null) {
            args = response.getArgs();
            channel = response.getChannel();
        } else {
            return false;
        }

        if (!BotPermissionUtil.memberCanMentionRoles(channel, event.getMember(), args)) {
            drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "user_nomention")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        MentionValue<Long> timeMention = MentionUtil.getTimeMinutes(args);
        int minutes = timeMention.getValue().intValue();
        String messageText = timeMention.getFilteredArgs();

        if (minutes < 1) {
            drawMessageNew(EmbedFactory.getEmbedError(this, getString("notime")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        if (messageText.isEmpty()) {
            drawMessageNew(EmbedFactory.getEmbedError(this, getString("notext")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        Instant time = Instant.now().plus(minutes, ChronoUnit.MINUTES);
        setComponents(generateButtons(getLocale()));
        drawMessageNew(generateEmbed(getLocale(), channel, time, messageText, 0))
                .thenAccept(message -> insertReminderBean(event.getTextChannel(), channel, time, messageText, message))
                .exceptionally(ExceptionLogger.get());

        return true;
    }

    @Override
    public void onStaticButton(ButtonInteractionEvent event) {
        if (event.getChannel() instanceof TextChannel) {
            EmbedBuilder eb = BotPermissionUtil.getUserAndBotPermissionMissingEmbed(
                    getLocale(),
                    event.getTextChannel(),
                    event.getMember(),
                    new Permission[] { Permission.MANAGE_SERVER },
                    new Permission[0],
                    new Permission[0],
                    new Permission[0],
                    new Permission[0]
            );

            if (eb == null) {
                CustomObservableMap<Long, ReminderData> remindersMap = DBReminders.getInstance()
                        .retrieve(event.getGuild().getIdLong());
                ReminderData reminderData = remindersMap.values().stream()
                        .filter(reminder -> reminder.getMessageId() == event.getMessageIdLong())
                        .findFirst()
                        .orElse(null);

                switch (Objects.requireNonNull(event.getComponent().getId())) {
                    case CANCEL_ID -> {
                        event.getMessage().delete().queue();
                        remindersMap.remove(reminderData.getId());
                    }
                    case REPEAT_ID -> {
                        if (!ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                            event.replyEmbeds(EmbedFactory.getPatreonBlockEmbed(getLocale()).build())
                                    .addActionRow(EmbedFactory.getPatreonBlockButtons(getLocale()))
                                    .setEphemeral(true)
                                    .queue();
                            return;
                        }

                        TextInput textInput = TextInput.create("interval", getString("interval"), TextInputStyle.SHORT)
                                .setMinLength(0)
                                .setMaxLength(12)
                                .build();

                        Modal modal = ModalMediator.createModal(getString("repeatafter"), e -> {
                                    String value = e.getValues().get(0).getAsString();
                                    long minutes = MentionUtil.getTimeMinutes(value).getValue();

                                    ReminderData newReminderData = new ReminderData(
                                            reminderData.getGuildId(),
                                            reminderData.getId(),
                                            reminderData.getSourceChannelId(),
                                            reminderData.getTargetChannelId(),
                                            reminderData.getMessageId(),
                                            reminderData.getTime(),
                                            reminderData.getMessage(),
                                            (int) minutes
                                    );
                                    remindersMap.put(newReminderData.getId(), newReminderData);

                                    EmbedBuilder newEmbed = generateEmbed(
                                            getLocale(),
                                            event.getTextChannel(),
                                            newReminderData.getTime(),
                                            newReminderData.getMessage(),
                                            newReminderData.getInterval()
                                    );
                                    e.editMessageEmbeds(newEmbed.build())
                                            .queue();
                                })
                                .addActionRow(textInput)
                                .build();

                        event.replyModal(modal).queue();
                    }
                }
            } else {
                event.replyEmbeds(eb.build())
                        .setEphemeral(true)
                        .queue();
            }
        }
    }

    private void insertReminderBean(TextChannel sourceChannel, GuildMessageChannel targetChannel, Instant time, String messageText, Message message) {
        CustomObservableMap<Long, ReminderData> remindersMap = DBReminders.getInstance()
                .retrieve(targetChannel.getGuild().getIdLong());

        ReminderData remindersData = new ReminderData(
                targetChannel.getGuild().getIdLong(),
                System.nanoTime(),
                sourceChannel.getIdLong(),
                targetChannel.getIdLong(),
                message.getIdLong(),
                time,
                messageText,
                0
        );

        remindersMap.put(remindersData.getId(), remindersData);
        ReminderScheduler.loadReminderData(remindersData);
        registerStaticReactionMessage(message);
    }

    public static EmbedBuilder generateEmbed(Locale locale, BaseGuildMessageChannel channel, Instant time, String messageText, int interval) {
        String intervalText = TextManager.getString(locale, Category.UTILITY, "reminder_norep");
        if (interval > 0) {
            intervalText = TimeUtil.getRemainingTimeString(locale, Duration.ofMinutes(interval).toMillis(), false);
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setDescription(TextManager.getString(locale, Category.UTILITY, "reminder_template", Emojis.X.getFormatted()))
                .addField(TextManager.getString(locale, Category.UTILITY, "reminder_channel"), channel.getAsMention(), true)
                .addField(TextManager.getString(locale, Category.UTILITY, "reminder_timespan"), TimeFormat.RELATIVE.atInstant(time).toString(), true)
                .addField(TextManager.getString(locale, Category.UTILITY, "reminder_repeatafter") + " " + Emojis.COMMAND_ICON_PREMIUM.getFormatted(), intervalText, true)
                .addField(TextManager.getString(locale, Category.UTILITY, "reminder_content"), StringUtil.shortenString(messageText, 1024), false);

        EmbedUtil.addLog(eb, LogStatus.WARNING,TextManager.getString(locale, Category.UTILITY, "reminder_dontremovemessage"));
        return eb;
    }

    public static Button[] generateButtons(Locale locale) {
        Button repeatButton = Button.of(ButtonStyle.PRIMARY, REPEAT_ID, TextManager.getString(locale, Category.UTILITY, "reminder_repeatafter"));
        Button cancelButton = Button.of(ButtonStyle.SECONDARY, CANCEL_ID, TextManager.getString(locale, TextManager.GENERAL, "process_abort"));
        return new Button[] { repeatButton, cancelButton };
    }

}
