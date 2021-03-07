package commands.runnables.utilitycategory;

import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnReactionAddListener;
import core.CustomObservableMap;
import core.EmbedFactory;
import core.MainLogger;
import core.TextManager;
import core.mention.MentionList;
import core.utils.MentionUtil;
import core.utils.BotPermissionUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import modules.schedulers.ReminderScheduler;
import mysql.modules.reminders.DBReminders;
import mysql.modules.reminders.RemindersBean;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.javacord.api.util.logging.ExceptionLogger;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;

@CommandProperties(
        trigger = "reminder",
        userPermissions = PermissionDeprecated.MANAGE_SERVER,
        emoji = "⏲️",
        executableWithoutArgs = false,
        releaseDate = { 2020, 10, 21 },
        aliases = { "remindme", "remind", "reminders", "schedule", "scheduler", "schedulers" }
)
public class ReminderCommand extends Command implements OnReactionAddListener {

    private final String CANCEL_EMOJI = "❌";

    private Message message = null;
    private RemindersBean remindersBean = null;
    private boolean active = true;

    public ReminderCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        long minutes = 0;
        StringBuilder text = new StringBuilder();
        MentionList<ServerTextChannel> channelMention = MentionUtil.getTextChannels(event.getMessage(), followedString);
        followedString = channelMention.getResultMessageString();

        ArrayList<ServerTextChannel> channels = channelMention.getList();
        if (channels.size() > 1) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("twochannels"))).get();
            return false;
        }

        ServerTextChannel channel = channels.size() == 0 ? event.getServerTextChannel().get() : channels.get(0);
        if (!checkWriteInChannelWithLog(channel)) {
            String error = TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel", channel.getMentionTag());
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, error)).get();
            return false;
        }

        EmbedBuilder missingPermissionsEmbed = BotPermissionUtil.getUserAndBotPermissionMissingEmbed(
                getLocale(),
                event.getServer().get(),
                channel,
                event.getMessage().getUserAuthor().get(),
                PermissionDeprecated.READ_MESSAGES | PermissionDeprecated.SEND_MESSAGES,
                PermissionDeprecated.READ_MESSAGES | PermissionDeprecated.SEND_MESSAGES
        );
        if (missingPermissionsEmbed != null) {
            event.getChannel().sendMessage(missingPermissionsEmbed).get();
            return false;
        }

        if (!BotPermissionUtil.memberCanMentionRoles(channel, event.getMessageAuthor().asUser().get(), event.getMessageContent())) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("user_nomention"))).get();
            return false;
        }

        for(String part : followedString.split(" ")) {
            if (part.length() > 0) {
                long value = MentionUtil.getTimeMinutesExt(part);
                if (value > 0) {
                    minutes += value;
                } else {
                    text.append(part).append(" ");
                }
            } else {
                text.append(" ");
            }
        }

        if (minutes <= 0 || minutes > 999 * 24 * 60) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("notime"))).get();
            return false;
        }

        String messageText = text.toString().trim();
        if (messageText.isEmpty()) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("notext"))).get();
            return false;
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("template", CANCEL_EMOJI))
                .addInlineField(getString("channel"), channel.getMentionTag())
                .addInlineField(getString("timespan"), TimeUtil.getRemainingTimeString(getLocale(), minutes * 60 * 1000, false))
                .addField(getString("content"), StringUtil.shortenString(messageText, 1024));

        message = event.getChannel().sendMessage(eb).get();
        message.addReaction(CANCEL_EMOJI).get();
        insertReminderBean(channel, minutes, messageText);

        return true;
    }

    private void insertReminderBean(ServerTextChannel channel, long minutes, String messageText) {
        CustomObservableMap<Integer, RemindersBean> remindersBeans = DBReminders.getInstance().retrieve();

        remindersBean = new RemindersBean(
                channel.getServer().getId(),
                generateNewId(remindersBeans),
                channel.getId(),
                Instant.now().plus(minutes, ChronoUnit.MINUTES),
                messageText,
                this::cancel
        );

        remindersBeans.put(remindersBean.getId(), remindersBean);
        ReminderScheduler.getInstance().loadReminderBean(remindersBean);
    }

    private int generateNewId(CustomObservableMap<Integer, RemindersBean> remindersBeans) {
        int value = 0;
        while (remindersBeans.containsKey(value)) {
            value++;
        }
        return value;
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (event.getEmoji().isUnicodeEmoji() && event.getEmoji().asUnicodeEmoji().get().equals(CANCEL_EMOJI)) {
            cancel();
        }
    }

    private void cancel() {
        if (active) {
            remindersBean.stop();
            removeReactionListener();
            message.edit(EmbedFactory.getEmbedDefault(this, getString("canceled"))).exceptionally(ExceptionLogger.get());
            try {
                DBReminders.getInstance().retrieve().remove(remindersBean.getId(), remindersBean);
            } catch (Throwable e) {
                MainLogger.get().error("Could not load reminders", e);
            }
        }
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable {
        active = false;
    }

}
