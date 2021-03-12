package commands.runnables.utilitycategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnReactionListener;
import constants.Emojis;
import core.CustomObservableMap;
import core.EmbedFactory;
import core.TextManager;
import core.mention.MentionList;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import modules.schedulers.ReminderScheduler;
import mysql.modules.reminders.DBReminders;
import mysql.modules.reminders.ReminderSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "reminder",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "⏲️",
        executableWithoutArgs = false,
        releaseDate = { 2020, 10, 21 },
        aliases = { "remindme", "remind", "reminders", "schedule", "scheduler", "schedulers" }
)
public class ReminderCommand extends Command implements OnReactionListener {

    private ReminderSlot remindersBean = null;
    private boolean active = true;
    private boolean canceled = false;
    private EmbedBuilder eb;

    public ReminderCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        long minutes = 0;
        StringBuilder text = new StringBuilder();
        MentionList<TextChannel> channelMention = MentionUtil.getTextChannels(event.getMessage(), args);
        args = channelMention.getFilteredArgs();

        List<TextChannel> channels = channelMention.getList();
        if (channels.size() > 1) {
            event.getChannel().sendMessage(
                    EmbedFactory.getEmbedError(this, getString("twochannels")).build()
            ).queue();
            return false;
        }

        TextChannel channel = channels.size() == 0 ? event.getChannel() : channels.get(0);
        if (!BotPermissionUtil.canWriteEmbed(channel)) {
            String error = TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel", channel.getAsMention());
            event.getChannel().sendMessage(
                    EmbedFactory.getEmbedError(this, error).build()
            ).queue();
            return false;
        }

        EmbedBuilder missingPermissionsEmbed = BotPermissionUtil.getUserAndBotPermissionMissingEmbed(
                getLocale(),
                channel,
                event.getMember(),
                new Permission[0],
                new Permission[] { Permission.MESSAGE_WRITE },
                new Permission[0],
                new Permission[] { Permission.MESSAGE_WRITE }

        );
        if (missingPermissionsEmbed != null) {
            event.getChannel().sendMessage(missingPermissionsEmbed.build()).queue();
            return false;
        }

        if (!BotPermissionUtil.memberCanMentionRoles(channel, event.getMember(), args)) {
            event.getChannel().sendMessage(
                    EmbedFactory.getEmbedError(this, getString("user_nomention")).build()
            ).queue();
            return false;
        }

        for (String part : args.split(" ")) {
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
            event.getChannel().sendMessage(
                    EmbedFactory.getEmbedError(this, getString("notime")).build()
            ).queue();
            return false;
        }

        String messageText = text.toString().trim();
        if (messageText.isEmpty()) {
            event.getChannel().sendMessage(
                    EmbedFactory.getEmbedError(this, getString("notext")).build()
            ).queue();
            return false;
        }

        String CANCEL_EMOJI = Emojis.X;
        this.eb = EmbedFactory.getEmbedDefault(this, getString("template", CANCEL_EMOJI))
                .addField(getString("channel"), channel.getAsMention(), true)
                .addField(getString("timespan"), TimeUtil.getRemainingTimeString(getLocale(), minutes * 60 * 1000, false), true)
                .addField(getString("content"), StringUtil.shortenString(messageText, 1024), false);

        insertReminderBean(channel, minutes, messageText);
        registerReactionListener(CANCEL_EMOJI);
        return true;
    }

    private void insertReminderBean(TextChannel channel, long minutes, String messageText) {
        CustomObservableMap<Integer, ReminderSlot> remindersMap = DBReminders.getInstance()
                .retrieve(channel.getGuild().getIdLong());

        remindersBean = new ReminderSlot(
                channel.getGuild().getIdLong(),
                generateNewId(remindersMap),
                channel.getIdLong(),
                Instant.now().plus(minutes, ChronoUnit.MINUTES),
                messageText,
                () -> cancel(channel.getGuild().getIdLong())
        );

        remindersMap.put(remindersBean.getId(), remindersBean);
        ReminderScheduler.getInstance().loadReminderBean(remindersBean);
    }

    private int generateNewId(CustomObservableMap<Integer, ReminderSlot> remindersBeans) {
        int value = 0;
        while (remindersBeans.containsKey(value)) {
            value++;
        }
        return value;
    }

    private void cancel(long guildId) {
        if (active) {
            canceled = true;
            removeReactionListener();
            DBReminders.getInstance().retrieve(guildId)
                    .remove(remindersBean.getId(), remindersBean);
        }
    }

    @Override
    public boolean onReaction(GenericGuildMessageReactionEvent event) {
        cancel(event.getGuild().getIdLong());
        return true;
    }

    @Override
    public EmbedBuilder draw() {
        if (canceled) {
            return EmbedFactory.getEmbedDefault(this, getString("canceled"));
        } else {
            return eb;
        }
    }

    @Override
    public void onReactionTimeOut() {
        active = false;
    }

}
