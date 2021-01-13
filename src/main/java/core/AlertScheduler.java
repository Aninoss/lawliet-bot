package core;

import commands.Command;
import commands.CommandManager;
import commands.listeners.OnTrackerRequestListener;
import commands.runnables.utilitycategory.AlertsCommand;
import constants.Permission;
import core.utils.TimeUtil;
import mysql.modules.tracker.TrackerBean;
import mysql.modules.tracker.TrackerBeanSlot;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AlertScheduler {

    private final static Logger LOGGER = LoggerFactory.getLogger(AlertScheduler.class);

    private static final AlertScheduler ourInstance = new AlertScheduler();

    public static AlertScheduler getInstance() {
        return ourInstance;
    }

    private AlertScheduler() {
    }

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private boolean active = false;

    public synchronized void start(TrackerBean trackerBean) {
        if (active) return;
        active = true;

        LOGGER.info("Starting {} alerts", trackerBean.getSlots().size());
        new ArrayList<>(trackerBean.getSlots())
                .forEach(this::registerAlert);
    }

    public void registerAlert(TrackerBeanSlot slot) {
        executorService.schedule(() -> {
            if (slot.isActive() && DiscordApiManager.getInstance().serverIsManaged(slot.getServerId()) && manageAlert(slot)) {
                registerAlert(slot);
            }
        }, TimeUtil.getMilisBetweenInstants(Instant.now(), slot.getNextRequest()), TimeUnit.MILLISECONDS);
    }

    private boolean manageAlert(TrackerBeanSlot slot) {
        Instant minInstant = Instant.now().plus(1, ChronoUnit.MINUTES);

        try {
            processAlert(slot);
        } catch (Throwable throwable) {
            LOGGER.error("Error in tracker \"{}\" with key \"{}\"", slot.getCommandTrigger(), slot.getCommandKey(), throwable);
            minInstant = Instant.now().plus(10, ChronoUnit.MINUTES);
            if (throwable.toString().contains("Unknown Channel"))
                slot.delete();
        }

        if (slot.isActive()) {
            if (minInstant.isAfter(slot.getNextRequest()))
                slot.setNextRequest(minInstant);
            return true;
        }

        return false;
    }

    private void processAlert(TrackerBeanSlot slot) throws Throwable {
        Optional<Command> commandOpt = CommandManager.createCommandByTrigger(slot.getCommandTrigger(), slot.getServerBean().getLocale(), slot.getServerBean().getPrefix());
        if (commandOpt.isEmpty()) {
            LOGGER.error("Invalid command for alert: {}", slot.getCommandTrigger());
            slot.stop();
            return;
        }

        OnTrackerRequestListener command = commandOpt.map(c -> (OnTrackerRequestListener) c).get();
        Optional<ServerTextChannel> channelOpt = slot.getChannel();
        if (channelOpt.isPresent()) {
            if (PermissionCheckRuntime.getInstance().botHasPermission(((Command) command).getLocale(), AlertsCommand.class, channelOpt.get(), Permission.READ_MESSAGES | Permission.SEND_MESSAGES | Permission.EMBED_LINKS)) {
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
                        slot.save();
                        break;
                }
            }
        } else if (slot.getServer().isPresent()) {
            slot.delete();
        }
    }

}
