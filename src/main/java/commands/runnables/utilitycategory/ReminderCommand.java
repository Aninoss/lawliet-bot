package commands.runnables.utilitycategory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
import core.mention.MentionList;
import core.mention.MentionValue;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.schedulers.ReminderScheduler;
import mysql.modules.reminders.DBReminders;
import mysql.modules.reminders.ReminderData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
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
        MentionList<TextChannel> channelMention = MentionUtil.getTextChannels(event.getGuild(), args);
        args = channelMention.getFilteredArgs();

        List<TextChannel> channels = channelMention.getList();
        if (channels.size() > 1) {
            drawMessageNew(EmbedFactory.getEmbedError(this, getString("twochannels")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        TextChannel channel = channels.size() == 0 ? event.getChannel() : channels.get(0);
        if (!BotPermissionUtil.canWriteEmbed(channel)) {
            String error = TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel", channel.getAsMention());
            drawMessageNew(EmbedFactory.getEmbedError(this, error))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        EmbedBuilder missingPermissionsEmbed = BotPermissionUtil.getUserAndBotPermissionMissingEmbed(
                getLocale(),
                channel,
                event.getMember(),
                new Permission[0],
                new Permission[] { Permission.MESSAGE_WRITE },
                new Permission[0],
                new Permission[] { Permission.MESSAGE_WRITE },
                new Permission[0]
        );
        if (missingPermissionsEmbed != null) {
            drawMessageNew(missingPermissionsEmbed)
                    .exceptionally(ExceptionLogger.get());
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

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("template", Emojis.X))
                .addField(getString("channel"), channel.getAsMention(), true)
                .addField(getString("timespan"), TimeFormat.RELATIVE.after(Duration.ofMinutes(minutes)).toString(), true)
                .addField(getString("content"), StringUtil.shortenString(messageText, 1024), false);
        EmbedUtil.addLog(eb, LogStatus.WARNING, getString("dontremovemessage"));

        setComponents(Button.of(ButtonStyle.SECONDARY, "cancel", TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort")));
        drawMessageNew(eb)
                .thenAccept(message -> insertReminderBean(event.getChannel(), channel, minutes, messageText, message))
                .exceptionally(ExceptionLogger.get());

        return true;
    }

    private void insertReminderBean(TextChannel sourceChannel, TextChannel targetChannel, long minutes, String messageText, Message message) {
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
    public void onStaticButton(ButtonClickEvent event) {
        EmbedBuilder eb = BotPermissionUtil.getUserAndBotPermissionMissingEmbed(
                getLocale(),
                event.getTextChannel(),
                event.getMember(),
                new Permission[]{ Permission.MANAGE_SERVER },
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
