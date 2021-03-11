package modules.schedulers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import commands.Command;
import commands.CommandManager;
import commands.listeners.OnTrackerRequestListener;
import commands.runnables.utilitycategory.AlertsCommand;
import core.CustomObservableMap;
import core.MainLogger;
import core.PermissionCheckRuntime;
import core.ShardManager;
import core.utils.TimeUtil;
import mysql.modules.tracker.DBTracker;
import mysql.modules.tracker.TrackerSlot;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;

public class AlertScheduler {

    private static final AlertScheduler ourInstance = new AlertScheduler();

    public static AlertScheduler getInstance() {
        return ourInstance;
    }

    private AlertScheduler() {
    }

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private boolean started = false;

    public void start() {
        if (started) return;
        started = true;

        try {
            DBTracker.getInstance().retrieveAll().forEach(this::loadAlert);
        } catch (Throwable e) {
            MainLogger.get().error("Could not start alerts", e);
        }
    }

    public void loadAlert(TrackerSlot slot) {
        loadAlert(slot.getGuildId(), slot.hashCode(), slot.getNextRequest());
    }

    public void loadAlert(long guildId, int hash, Instant due) {
        long millis = TimeUtil.getMillisBetweenInstants(Instant.now(), due);
        executorService.schedule(() -> {
            CustomObservableMap<Integer, TrackerSlot> map = DBTracker.getInstance().retrieve(guildId);
            if (map.containsKey(hash)) {
                TrackerSlot slot = map.get(hash);
                if (slot.isActive() && ShardManager.getInstance().guildIsManaged(slot.getGuildId()) && manageAlert(slot)) {
                    loadAlert(slot);
                }
            }
        }, millis, TimeUnit.MILLISECONDS);
    }

    private boolean manageAlert(TrackerSlot slot) {
        Instant minInstant = Instant.now().plus(1, ChronoUnit.MINUTES);

        try {
            processAlert(slot);
        } catch (Throwable throwable) {
            MainLogger.get().error("Error in tracker \"{}\" with key \"{}\"", slot.getCommandTrigger(), slot.getCommandKey(), throwable);
            minInstant = Instant.now().plus(10, ChronoUnit.MINUTES);
            if (throwable.toString().contains("Unknown Channel")) {
                slot.delete();
            }
        }

        if (slot.isActive()) {
            if (minInstant.isAfter(slot.getNextRequest())) {
                slot.setNextRequest(minInstant);
            }
            return true;
        }

        return false;
    }

    private void processAlert(TrackerSlot slot) throws Throwable {
        Optional<Command> commandOpt = CommandManager.createCommandByTrigger(slot.getCommandTrigger(), slot.getGuildBean().getLocale(), slot.getGuildBean().getPrefix());
        if (commandOpt.isEmpty()) {
            MainLogger.get().error("Invalid command for alert: {}", slot.getCommandTrigger());
            slot.stop();
            return;
        }

        OnTrackerRequestListener command = commandOpt.map(c -> (OnTrackerRequestListener) c).get();
        Optional<TextChannel> channelOpt = slot.getTextChannel();
        if (channelOpt.isPresent()) {
            if (PermissionCheckRuntime.getInstance().botHasPermission(((Command) command).getLocale(), AlertsCommand.class, channelOpt.get(), Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS)) {
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
        } else if (slot.getGuild().isPresent()) {
            slot.delete();
        }
    }

}
