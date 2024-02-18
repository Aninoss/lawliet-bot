package modules.schedulers;

import commands.Category;
import commands.Command;
import commands.CommandManager;
import commands.listeners.OnAlertListener;
import commands.runnables.configurationcategory.AlertsCommand;
import constants.ExternalLinks;
import constants.Settings;
import core.*;
import core.cache.ServerPatreonBoostCache;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.utils.EmbedUtil;
import core.utils.ExceptionUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.modules.tracker.DBTracker;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.internal.utils.concurrent.CountingThreadFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AlertScheduler {

    private static ScheduledExecutorService executorService;

    public static void start() {
        try {
            executorService = Executors.newSingleThreadScheduledExecutor(new CountingThreadFactory(() -> "Main", "Alerts", false));
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
                GlobalThreadPool.submit(() -> {
                    try (AsyncTimer asyncTimer = new AsyncTimer(Duration.ofMinutes(5))) {
                        asyncTimer.setTimeOutListener(t -> {
                            MainLogger.get().error("Alert stuck: {} with key {}", slot.getCommandTrigger(), slot.getCommandKey(), ExceptionUtil.generateForStack(t));
                        });

                        if (slot.isActive() && manageAlert(slot)) {
                            loadAlert(slot);
                        }
                    }
                });
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    MainLogger.get().error("Interrupted", e);
                }
            }
        }, millis, TimeUnit.MILLISECONDS);
    }

    private static boolean manageAlert(TrackerData slot) {
        Instant minInstant = Instant.now().plus(1, ChronoUnit.MINUTES);

        try (GuildEntity guildEntity = HibernateManager.findGuildEntity(slot.getGuildId(), AlertScheduler.class)) {
            processAlert(guildEntity, slot);
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

    private static void processAlert(GuildEntity guildEntity, TrackerData slot) throws Throwable {
        Optional<Command> commandOpt = CommandManager.createCommandByTrigger(slot.getCommandTrigger(), guildEntity.getLocale(), guildEntity.getPrefix());
        if (commandOpt.isEmpty()) {
            MainLogger.get().error("Invalid alert for command: {}", slot.getCommandTrigger());
            slot.delete();
            return;
        }

        Command command = commandOpt.get();
        command.setGuildEntity(guildEntity);

        OnAlertListener alertCommand = (OnAlertListener) command;
        Optional<StandardGuildMessageChannel> channelOpt = slot.getStandardGuildMessageChannel();
        if (channelOpt.isPresent()) {
            StandardGuildMessageChannel channel = channelOpt.get();
            if (channel.getGuild().getMember(channel.getJDA().getSelfUser()) == null) {
                MainLogger.get().warn("Guild {} does not have a self member", channel.getGuild().getIdLong());
                return;
            }

            boolean premium = ServerPatreonBoostCache.get(channel.getGuild().getIdLong());
            CustomObservableMap<Integer, TrackerData> alerts = DBTracker.getInstance().retrieve(slot.getGuildId());
            if (!PermissionCheckRuntime.botHasPermission(((Command) alertCommand).getLocale(), AlertsCommand.class, channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS) ||
                    checkNSFW(slot, channel, (Command) alertCommand) ||
                    checkPatreon(slot, (Command) alertCommand, premium) ||
                    checkReleased(slot, (Command) alertCommand, premium) ||
                    checkServerLimit(((Command) alertCommand).getLocale(), slot, alerts, premium) ||
                    checkChannelLimit(((Command) alertCommand).getLocale(), slot, alerts, premium)
            ) {
                return;
            }

            switch (alertCommand.onTrackerRequest(slot)) {
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
                        FeatureLogger.inc(PremiumFeature.ALERTS, slot.getGuildId());
                        slot.setNextRequest(minIntervalInstant);
                    }
                    slot.save();
                    break;
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
            EmbedBuilder eb = EmbedFactory.getNSFWBlockEmbed(command.getLocale(), command.getPrefix());
            EmbedUtil.addTrackerRemoveLog(eb, command.getLocale());
            slot.sendMessage(command.getLocale(), false, eb.build());
            slot.delete();
            return true;
        }
        return false;
    }

    private static boolean checkPatreon(TrackerData slot, Command command, boolean premium) throws InterruptedException {
        if (command.getCommandProperties().patreonRequired() && !premium) {
            EmbedBuilder eb = EmbedFactory.getPatreonBlockEmbed(command.getLocale());
            EmbedUtil.addTrackerRemoveLog(eb, command.getLocale());
            slot.sendMessage(command.getLocale(), false, eb.build());
            slot.delete();
            return true;
        }
        return false;
    }

    private static boolean checkReleased(TrackerData slot, Command command, boolean premium) throws InterruptedException {
        LocalDate releaseDate = command.getReleaseDate().orElse(LocalDate.now());
        if (releaseDate.isAfter(LocalDate.now()) && !premium) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setColor(Settings.PREMIUM_COLOR)
                    .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_beta_title"))
                    .setDescription(TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_beta_description"));

            EmbedUtil.addTrackerRemoveLog(eb, command.getLocale());
            slot.sendMessage(command.getLocale(), false, eb.build());
            slot.delete();
            return true;
        }
        return false;
    }

    private static boolean checkServerLimit(Locale locale, TrackerData slot, CustomObservableMap<Integer, TrackerData> alerts, boolean premium) throws InterruptedException {
        if (alerts.size() > AlertsCommand.LIMIT_SERVER) {
            if (premium) {
                FeatureLogger.inc(PremiumFeature.ALERTS, slot.getGuildId());
            } else {
                EmbedBuilder eb = EmbedFactory.getEmbedError()
                        .setTitle(TextManager.getString(locale, Category.CONFIGURATION, "alerts_scheduler_toomuch_title"))
                        .setDescription(TextManager.getString(locale, Category.CONFIGURATION, "alerts_scheduler_toomuch_server",
                                StringUtil.numToString(AlertsCommand.LIMIT_SERVER),
                                ExternalLinks.PREMIUM_WEBSITE
                        ));

                slot.sendMessage(locale, false, eb.build());
                slot.setNextRequest(Instant.now().plus(Duration.ofHours(1)));
                slot.save();
                return true;
            }
        }
        return false;
    }

    private static boolean checkChannelLimit(Locale locale, TrackerData slot, CustomObservableMap<Integer, TrackerData> alerts, boolean premium) throws InterruptedException {
        long channelId = slot.getStandardGuildMessageChannelId();
        if (alerts.values().stream().filter(a -> a.getStandardGuildMessageChannelId() == channelId).count() > AlertsCommand.LIMIT_CHANNEL) {
            if (premium) {
                FeatureLogger.inc(PremiumFeature.ALERTS, slot.getGuildId());
            } else {
                EmbedBuilder eb = EmbedFactory.getEmbedError()
                        .setTitle(TextManager.getString(locale, Category.CONFIGURATION, "alerts_scheduler_toomuch_title"))
                        .setDescription(TextManager.getString(locale, Category.CONFIGURATION, "alerts_scheduler_toomuch_channel",
                                StringUtil.numToString(AlertsCommand.LIMIT_CHANNEL),
                                ExternalLinks.PREMIUM_WEBSITE
                        ));

                slot.sendMessage(locale, false, eb.build());
                slot.setNextRequest(Instant.now().plus(Duration.ofHours(1)));
                slot.save();
                return true;
            }
        }
        return false;
    }

}
