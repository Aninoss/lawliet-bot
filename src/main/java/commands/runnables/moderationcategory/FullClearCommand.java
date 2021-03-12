package commands.runnables.moderationcategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import constants.TrackerResult;
import core.EmbedFactory;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.schedule.MainScheduler;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import modules.ClearResults;
import mysql.modules.tracker.TrackerSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "fullclear",
        botChannelPermissions = { Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY },
        userChannelPermissions = { Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY },
        emoji = "\uD83E\uDDF9",
        executableWithoutArgs = true,
        maxCalculationTimeSec = 10 * 60,
        aliases = { "fclear", "allclear", "clearall" }
)
public class FullClearCommand extends Command implements OnAlertListener {

    public FullClearCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        Optional<Integer> hoursMin = extractHoursMin(event.getChannel(), args);
        if (hoursMin.isPresent()) {
            addLoadingReactionInstantly();
            ClearResults clearResults = fullClear(event.getChannel(), hoursMin.get(), event.getMessage().getIdLong());

            String key = clearResults.getRemaining() > 0 ? "finished_too_old" : "finished_description";
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString(key, clearResults.getDeleted() != 1, String.valueOf(clearResults.getDeleted())));
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "deleteTime", "8"));
            if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
                event.getChannel().sendMessage(eb.build())
                        .queue(m -> {
                            if (BotPermissionUtil.can(event.getGuild(), Permission.MESSAGE_MANAGE)) {
                                MainScheduler.getInstance().schedule(8, ChronoUnit.SECONDS, "fullclear_confirmation_autoremove", () -> event.getChannel().purgeMessages(m, event.getMessage()));
                            }
                        });
            }
            return true;
        } else {
            return false;
        }
    }

    private Optional<Integer> extractHoursMin(TextChannel channel, String str) {
        if (str.length() > 0) {
            if (StringUtil.stringIsLong(str) && Long.parseLong(str) >= 0 && Long.parseLong(str) <= 20159) {
                return Optional.of(Integer.parseInt(str));
            } else {
                channel.sendMessage(
                        EmbedFactory.getEmbedError(this, getString("wrong_args", "0", "20159")).build()
                ).queue();
                return Optional.empty();
            }
        } else {
            return Optional.of(0);
        }
    }

    private ClearResults fullClear(TextChannel channel, int hours) {
        return fullClear(channel, hours, 0L);
    }

    private ClearResults fullClear(TextChannel channel, int hours, long messageIdIgnore) {
        int deleted = 0;
        boolean tooOld = false;

        MessageHistory messageHistory = channel.getHistory();
        do {
            //Check for message date and therefore permissions
            List<Message> messageSet = messageHistory.retrievePast(100).complete();
            if (messageSet.isEmpty()) {
                break;
            }

            ArrayList<Message> messagesDelete = new ArrayList<>();
            for (Message message : messageSet) {
                if (!message.getTimeCreated().toInstant().isAfter(Instant.now().minus(14, ChronoUnit.DAYS))) {
                    tooOld = true;
                    break;
                } else if (!message.isPinned() && message.getIdLong() != messageIdIgnore) {
                    if (!message.getTimeCreated().toInstant().isAfter(Instant.now().minus(hours, ChronoUnit.HOURS))) {
                        messagesDelete.add(message);
                        deleted++;
                    }
                }

                if (messagesDelete.size() >= 1) {
                    if (messagesDelete.size() == 1) {
                        messagesDelete.get(0).delete().complete();
                    } else {
                        channel.deleteMessages(messagesDelete).complete();
                    }
                }
            }
        } while (!tooOld);

        return new ClearResults(deleted, tooOld ? 1 : 0);
    }

    @Override
    public TrackerResult onTrackerRequest(TrackerSlot slot) throws Throwable {
        TextChannel textChannel = slot.getTextChannel().get();
        if (PermissionCheckRuntime.getInstance().botHasPermission(getLocale(), getClass(), textChannel, Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE)) {
            Optional<Integer> hoursMin = extractHoursMin(textChannel, slot.getCommandKey());
            if (hoursMin.isPresent()) {
                fullClear(textChannel, hoursMin.get());
                slot.setNextRequest(Instant.now().plus(1, ChronoUnit.HOURS));
                return TrackerResult.CONTINUE_AND_SAVE;
            }
        }

        return TrackerResult.STOP_AND_DELETE;
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

}
