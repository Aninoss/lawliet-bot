package commands.runnables.utilitycategory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnStaticButtonListener;
import constants.Emojis;
import constants.LogStatus;
import core.CustomObservableMap;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.mention.MentionValue;
import core.utils.*;
import modules.schedulers.ReminderScheduler;
import mysql.modules.reminders.DBReminders;
import mysql.modules.reminders.ReminderData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "reminder",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "⏲️",
        executableWithoutArgs = false,
        releaseDate = { 2020, 10, 21 },
        aliases = { "remindme", "remind", "reminders", "schedule", "scheduler", "schedulers" }
)
public class ReminderCommand extends Command implements OnStaticButtonListener {

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
        long minutes = timeMention.getValue();
        String messageText = timeMention.getFilteredArgs();

        if (minutes <= 0 || minutes > 999 * 24 * 60) {
            drawMessageNew(EmbedFactory.getEmbedError(this, getString("notime")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        if (messageText.isEmpty()) {
            drawMessageNew(EmbedFactory.getEmbedError(this, getString("notext")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("template", Emojis.X.getFormatted()))
                .addField(getString("channel"), channel.getAsMention(), true)
                .addField(getString("timespan"), TimeFormat.RELATIVE.after(Duration.ofMinutes(minutes)).toString(), true)
                .addField(getString("content"), StringUtil.shortenString(messageText, 1024), false);
        EmbedUtil.addLog(eb, LogStatus.WARNING, getString("dontremovemessage"));

        setComponents(Button.of(ButtonStyle.SECONDARY, "cancel", TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort")));
        drawMessageNew(eb)
                .thenAccept(message -> insertReminderBean(event.getTextChannel(), channel, minutes, messageText, message))
                .exceptionally(ExceptionLogger.get());

        return true;
    }

    private void insertReminderBean(TextChannel sourceChannel, GuildMessageChannel targetChannel, long minutes, String messageText, Message message) {
        CustomObservableMap<Long, ReminderData> remindersMap = DBReminders.getInstance()
                .retrieve(targetChannel.getGuild().getIdLong());

        ReminderData remindersData = new ReminderData(
                targetChannel.getGuild().getIdLong(),
                System.nanoTime(),
                sourceChannel.getIdLong(),
                targetChannel.getIdLong(),
                message.getIdLong(),
                Instant.now().plus(minutes, ChronoUnit.MINUTES),
                messageText
        );

        remindersMap.put(remindersData.getId(), remindersData);
        ReminderScheduler.loadReminderBean(remindersData);
        registerStaticReactionMessage(message);
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

                event.getMessage().delete().queue();
                remindersMap.values().stream()
                        .filter(reminder -> reminder.getMessageId() == event.getMessageIdLong())
                        .findFirst()
                        .ifPresent(reminderData -> remindersMap.remove(reminderData.getId()));
            } else {
                event.replyEmbeds(eb.build())
                        .setEphemeral(true)
                        .queue();
            }
        }
    }

}
