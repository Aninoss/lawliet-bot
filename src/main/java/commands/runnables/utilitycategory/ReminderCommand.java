package commands.runnables.utilitycategory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import constants.Emojis;
import core.CustomObservableMap;
import core.EmbedFactory;
import core.TextManager;
import core.mention.MentionList;
import core.mention.MentionValue;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.schedulers.ReminderScheduler;
import mysql.modules.reminders.DBReminders;
import mysql.modules.reminders.ReminderData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.utils.TimeFormat;

@CommandProperties(
        trigger = "reminder",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "⏲️",
        executableWithoutArgs = false,
        releaseDate = { 2020, 10, 21 },
        aliases = { "remindme", "remind", "reminders", "schedule", "scheduler", "schedulers" }
)
public class ReminderCommand extends Command implements OnButtonListener {

    private ReminderData remindersBean = null;
    private boolean active = true;
    private boolean canceled = false;
    private EmbedBuilder eb;

    public ReminderCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        MentionList<TextChannel> channelMention = MentionUtil.getTextChannels(event.getMessage(), args);
        args = channelMention.getFilteredArgs();

        List<TextChannel> channels = channelMention.getList();
        if (channels.size() > 1) {
            event.getChannel().sendMessageEmbeds(
                    EmbedFactory.getEmbedError(this, getString("twochannels")).build()
            ).queue();
            return false;
        }

        TextChannel channel = channels.size() == 0 ? event.getChannel() : channels.get(0);
        if (!BotPermissionUtil.canWriteEmbed(channel)) {
            String error = TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel", channel.getAsMention());
            event.getChannel().sendMessageEmbeds(
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
            event.getChannel().sendMessageEmbeds(missingPermissionsEmbed.build()).queue();
            return false;
        }

        if (!BotPermissionUtil.memberCanMentionRoles(channel, event.getMember(), args)) {
            event.getChannel().sendMessageEmbeds(
                    EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "user_nomention")).build()
            ).queue();
            return false;
        }

        MentionValue<Long> timeMention = MentionUtil.getTimeMinutes(args);
        long minutes = timeMention.getValue();
        String messageText = timeMention.getFilteredArgs();

        if (minutes <= 0 || minutes > 999 * 24 * 60) {
            event.getChannel().sendMessageEmbeds(
                    EmbedFactory.getEmbedError(this, getString("notime")).build()
            ).queue();
            return false;
        }

        if (messageText.isEmpty()) {
            event.getChannel().sendMessageEmbeds(
                    EmbedFactory.getEmbedError(this, getString("notext")).build()
            ).queue();
            return false;
        }

        String CANCEL_EMOJI = Emojis.X;
        this.eb = EmbedFactory.getEmbedDefault(this, getString("template", CANCEL_EMOJI))
                .addField(getString("channel"), channel.getAsMention(), true)
                .addField(getString("timespan"), TimeFormat.RELATIVE.after(Duration.ofMinutes(minutes)).toString(), true)
                .addField(getString("content"), StringUtil.shortenString(messageText, 1024), false);

        insertReminderBean(channel, minutes, messageText);
        setButtons(Button.of(ButtonStyle.SECONDARY, "cancel", TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort")));
        registerButtonListener();
        return true;
    }

    private void insertReminderBean(TextChannel channel, long minutes, String messageText) {
        CustomObservableMap<Long, ReminderData> remindersMap = DBReminders.getInstance()
                .retrieve(channel.getGuild().getIdLong());

        remindersBean = new ReminderData(
                channel.getGuild().getIdLong(),
                System.nanoTime(),
                channel.getIdLong(),
                Instant.now().plus(minutes, ChronoUnit.MINUTES),
                messageText,
                () -> {
                    if (active) {
                        cancel(channel.getGuild().getIdLong());
                        setButtons();
                        drawMessage(draw());
                    }
                }
        );

        remindersMap.put(remindersBean.getId(), remindersBean);
        ReminderScheduler.getInstance().loadReminderBean(remindersBean);
    }

    private void cancel(long guildId) {
        canceled = true;
        DBReminders.getInstance().retrieve(guildId)
                .remove(remindersBean.getId(), remindersBean);
    }

    @Override
    public boolean onButton(ButtonClickEvent event) throws Throwable {
        if (active) {
            deregisterListenersWithButtons();
            cancel(event.getGuild().getIdLong());
        }
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
    public void onListenerTimeOut() {
        redrawMessageWithoutButtons();
        active = false;
    }

}
