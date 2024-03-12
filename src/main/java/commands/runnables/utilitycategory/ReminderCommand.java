package commands.runnables.utilitycategory;

import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnStaticButtonListener;
import constants.Emojis;
import constants.Language;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.atomicassets.AtomicStandardGuildMessageChannel;
import core.mention.MentionValue;
import core.utils.*;
import modules.schedulers.ReminderScheduler;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.ReminderEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "reminder",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "⏲️",
        executableWithoutArgs = false,
        releaseDate = {2020, 10, 21},
        aliases = {"remindme", "remind", "reminders", "schedule", "scheduler", "schedulers"}
)
public class ReminderCommand extends Command implements OnStaticButtonListener {

    public ReminderCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        StandardGuildMessageChannel channel = null;
        CommandUtil.ChannelResponse response = CommandUtil.differentChannelExtract(this, event, channel, args);
        if (response != null) {
            args = response.getArgs();
            channel = response.getChannel();
        } else {
            return false;
        }

        if (channel != null) {
            EmbedBuilder missingPermissionsEmbed = BotPermissionUtil.getUserAndBotPermissionMissingEmbed(
                    getLocale(),
                    event.getMember(),
                    new Permission[]{Permission.MANAGE_SERVER},
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

        Message confirmationMessage = drawMessageNew(generateEmbed(getLocale(), getPrefix(), channel, time, messageText, null)).get();
        insertReminder(channel, event.getMember(), time, messageText, confirmationMessage);

        return true;
    }

    @Override
    public void onStaticButton(ButtonInteractionEvent event, String secondaryId) {
        EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("buttonsnotsupported"));
        event.replyEmbeds(eb.build())
                .setEphemeral(true)
                .queue();
    }

    private void insertReminder(GuildMessageChannel targetChannel, Member member, Instant time, String messageText, Message confirmationMessage) {
        ReminderEntity reminderEntity;
        if (targetChannel != null) {
            reminderEntity = ReminderEntity.createGuildReminder(
                    targetChannel.getGuild().getIdLong(),
                    targetChannel.getIdLong(),
                    time,
                    messageText,
                    confirmationMessage
            );
        } else {
            reminderEntity = ReminderEntity.createDmReminder(
                    member.getIdLong(),
                    time,
                    messageText,
                    Language.from(getLocale()),
                    confirmationMessage
            );
        }

        EntityManagerWrapper entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        entityManager.persist(reminderEntity);
        if (targetChannel != null) {
            BotLogEntity.log(entityManager, BotLogEntity.Event.REMINDERS_ADD, member, "\"" + StringUtil.shortenString(reminderEntity.getMessage(), 40) + "\"");
        }
        entityManager.getTransaction().commit();

        ReminderScheduler.loadReminder(reminderEntity);
    }

    public static EmbedBuilder generateEmbed(Locale locale, String prefix, StandardGuildMessageChannel channel, Instant time, String messageText, Integer interval) {
        String intervalText = TextManager.getString(locale, Category.UTILITY, "reminder_norep");
        if (interval != null && interval > 0) {
            intervalText = TimeUtil.getRemainingTimeString(locale, Duration.ofMinutes(interval).toMillis(), false);
        }

        String channelStr = channel != null
                ? new AtomicStandardGuildMessageChannel(channel).getPrefixedNameInField(locale)
                : TextManager.getString(locale, Category.UTILITY, "reminder_channel_dms");

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setDescription(TextManager.getString(locale, Category.UTILITY, "reminder_template", Emojis.X.getFormatted()))
                .addField(TextManager.getString(locale, Category.UTILITY, "reminder_channel"), channelStr, true)
                .addField(TextManager.getString(locale, Category.UTILITY, "reminder_timespan"), TimeFormat.RELATIVE.atInstant(time).toString(), true);

        String add = channel != null ? " " + Emojis.COMMAND_ICON_PREMIUM.getFormatted() : "";
        eb.addField(TextManager.getString(locale, Category.UTILITY, "reminder_repeatafter") + add, intervalText, true)
                .addField(TextManager.getString(locale, Category.UTILITY, "reminder_content"), StringUtil.shortenString(messageText, 1024), false);

        EmbedUtil.addLog(eb, TextManager.getString(locale, Category.UTILITY, "reminder_footer").replace("{PREFIX}", prefix));
        return eb;
    }

}
