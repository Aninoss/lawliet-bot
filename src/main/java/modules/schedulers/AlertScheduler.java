package modules.schedulers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import commands.Command;
import commands.CommandManager;
import commands.listeners.OnAlertListener;
import commands.runnables.utilitycategory.AlertsCommand;
import constants.Settings;
import core.*;
import core.cache.ServerPatreonBoostCache;
import core.utils.EmbedUtil;
import core.utils.ExceptionUtil;
import core.utils.TimeUtil;
import mysql.modules.tracker.DBTracker;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.internal.utils.concurrent.CountingThreadFactory;

public class AlertScheduler {

    private static ScheduledExecutorService executorService;

    public static void start() {
        try {
            executorService = Executors.newScheduledThreadPool(5, new CountingThreadFactory(() -> "Main", "Alerts", false));
            DBTracker.getInstance().retrieveAll().forEach(AlertScheduler::loadAlert);
        } catch (Throwable e) {
            MainLogger.get().error("Could not start alerts", e);
        }
    }

    public static void reset() {
        executorService.shutdownNow();
        start();
    }

    public static void loadAlert(TrackerData slot) {
        loadAlert(slot.getGuildId(), slot.hashCode(), slot.getNextRequest());
    }

    public static void loadAlert(long guildId, int hash, Instant due) {
        long millis = TimeUtil.getMillisBetweenInstants(Instant.now(), due);
        executorService.schedule(() -> {
            CustomObservableMap<Integer, TrackerData> map = DBTracker.getInstance().retrieve(guildId);
            if (map.containsKey(hash)) {
                TrackerData slot = map.get(hash);
                try (AsyncTimer asyncTimer = new AsyncTimer(Duration.ofMinutes(5))) {
                    asyncTimer.setTimeOutListener(t -> {
                        asyncTimer.interrupt();
                        MainLogger.get().error("Alert stuck: {} with key {}", slot.getCommandTrigger(), slot.getCommandKey(), ExceptionUtil.generateForStack(t));
                    });

                    if (slot.isActive() && manageAlert(slot)) {
                        loadAlert(slot);
                    }
                } catch (InterruptedException e) {
                    MainLogger.get().error("Interrupted", e);
                    loadAlert(slot);
                }
            }
        }, millis, TimeUnit.MILLISECONDS);
    }

    private static boolean manageAlert(TrackerData slot) {
        Instant minInstant = Instant.now().plus(1, ChronoUnit.MINUTES);

        try {
            processAlert(slot);
        } catch (Throwable throwable) {
            MainLogger.get().error("Error in tracker \"{}\" with key \"{}\"", slot.getCommandTrigger(), slot.getCommandKey(), throwable);
            minInstant = Instant.now().plus(10, ChronoUnit.MINUTES);
        }

        if (slot.isActive()) {
            if (minInstant.isAfter(slot.getNextRequest())) {
                slot.setNextRequest(minInstant);
            }
            return true;
        }

        return false;
    }

    private static void processAlert(TrackerData slot) throws Throwable {
        Optional<Command> commandOpt = CommandManager.createCommandByTrigger(slot.getCommandTrigger(), slot.getGuildData().getLocale(), slot.getGuildData().getPrefix());
        if (commandOpt.isEmpty()) {
            MainLogger.get().error("Invalid alert for command: {}", slot.getCommandTrigger());
            slot.delete();
            return;
        }

        OnAlertListener command = (OnAlertListener) commandOpt.get();
        Optional<StandardGuildMessageChannel> channelOpt = slot.getStandardGuildMessageChannel();
        if (channelOpt.isPresent()) {
            StandardGuildMessageChannel channel = channelOpt.get();
            if (PermissionCheckRuntime.botHasPermission(((Command) command).getLocale(), AlertsCommand.class, channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
                if (checkNSFW(slot, channel, (Command) command) ||
                        checkPatreon(slot, channel, (Command) command) ||
                        checkReleased(slot, channel, (Command) command)
                ) {
                    return;
                }

                switch (command.onTrackerRequest(slot)) {
                    case STOP:
                        slot.stop();
                        break;

                    case STOP_AND_DELETE:
                        slot.delete();
                        break;

                    case STOP_AND_SAVE:
                        slot.stop();
                        slot.save();
                        break;

                    case CONTINUE:
                        break;

                    case CONTINUE_AND_SAVE:
                        Instant minIntervalInstant = Instant.now().plus(slot.getMinInterval(), ChronoUnit.MINUTES);
                        if (slot.getMinInterval() > 0 &&
                                minIntervalInstant.isAfter(slot.getNextRequest()) &&
                                ServerPatreonBoostCache.get(channel.getGuild().getIdLong())
                        ) {
                            slot.setNextRequest(minIntervalInstant);
                        }
                        slot.save();
                        break;
                }
            }
        } else {
            if (slot.getGuild().isPresent()) {
                slot.delete();
            } else {
                slot.setNextRequest(Instant.now().plus(10, ChronoUnit.MINUTES));
            }
        }
    }

    private static boolean checkNSFW(TrackerData slot, StandardGuildMessageChannel channel, Command command) throws InterruptedException {
        if (command.getCommandProperties().nsfw() && !channel.isNSFW()) {
            EmbedBuilder eb = EmbedFactory.getNSFWBlockEmbed(command.getLocale());
            EmbedUtil.addTrackerRemoveLog(eb, command.getLocale());
            slot.sendMessage(false, eb.build(), ActionRow.of(EmbedFactory.getNSFWBlockButton(command.getLocale())));
            slot.delete();
            return true;
        }
        return false;
    }

    private static boolean checkPatreon(TrackerData slot, StandardGuildMessageChannel channel, Command command) throws InterruptedException {
        if (command.getCommandProperties().patreonRequired() &&
                !ServerPatreonBoostCache.get(channel.getGuild().getIdLong())
        ) {
            EmbedBuilder eb = EmbedFactory.getPatreonBlockEmbed(command.getLocale());
            EmbedUtil.addTrackerRemoveLog(eb, command.getLocale());
            slot.sendMessage(false, eb.build());
            slot.delete();
            return true;
        }
        return false;
    }

    private static boolean checkReleased(TrackerData slot, StandardGuildMessageChannel channel, Command command) throws InterruptedException {
        LocalDate releaseDate = command.getReleaseDate().orElse(LocalDate.now());
        if (releaseDate.isAfter(LocalDate.now()) &&
                !ServerPatreonBoostCache.get(channel.getGuild().getIdLong())
        ) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setColor(Settings.PREMIUM_COLOR)
                    .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_beta_title"))
                    .setDescription(TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_beta_description"));

            EmbedUtil.addTrackerRemoveLog(eb, command.getLocale());
            slot.sendMessage(false, eb.build());
            slot.delete();
            return true;
        }
        return false;
    }

}
