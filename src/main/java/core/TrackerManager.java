package core;

import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.OnTrackerRequestListener;
import commands.runnables.managementcategory.TrackerCommand;
import constants.Permission;
import constants.Settings;
import mysql.modules.tracker.DBTracker;
import mysql.modules.tracker.TrackerBeanSlot;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;

public class TrackerManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(TrackerManager.class);

    private static final TrackerManager ourInstance = new TrackerManager();
    public static TrackerManager getInstance() { return ourInstance; }
    private TrackerManager() { }

    private boolean active = false;

    public void start() {
        if (active) return;
        active = true;

        for (int i = 0; i < Settings.TRACKER_SHARDS; i++) {
            final int trackerShard = i;
            new CustomThread(() -> {
                manageTrackerShard(trackerShard);
            }, "trackers_" + i).start();
        }
    }

    private void manageTrackerShard(int trackerShard) {
        IntervalBlock intervalBlock = new IntervalBlock(1, ChronoUnit.MINUTES);
        while (intervalBlock.block() && active) {
            try {
                for (ArrayList<TrackerBeanSlot> trackerBeanSlots : getGroupedByCommandTrigger()) {
                    if (trackerBeanSlots.size() > 0) {
                        manageTrackerCommand(trackerBeanSlots, trackerShard);
                    }
                }
            } catch (Throwable e) {
                LOGGER.error("Exception on get grouped by command trigger", e);
            }
        }
    }

    private void manageTrackerCommand(ArrayList<TrackerBeanSlot> trackerBeanSlots, int trackerShard) throws InterruptedException {
        for (TrackerBeanSlot slot : trackerBeanSlots) {
            if (!slot.getNextRequest().isAfter(Instant.now()) &&
                    trackerIsForShard(slot, trackerShard)
            ) {
                try {
                    manageTracker(slot);
                } catch (InterruptedException e) {
                    throw e;
                } catch (Throwable throwable) {
                    LOGGER.error("Error in tracker", throwable);
                    if (throwable.toString().contains("Unknown Channel"))
                        slot.delete();
                }
            }
        }
    }

    private boolean trackerIsForShard(TrackerBeanSlot slot, int trackerShard) {
        return slot.getServerId() % Settings.TRACKER_SHARDS == trackerShard;
    }

    private void manageTracker(TrackerBeanSlot slot) throws Throwable {
        OnTrackerRequestListener command = (OnTrackerRequestListener) CommandManager.createCommandByTrigger(slot.getCommandTrigger(), slot.getServerBean().getLocale(), slot.getServerBean().getPrefix()).get();
        Optional<ServerTextChannel> channelOpt = slot.getChannel();
        if (channelOpt.isPresent() &&
                PermissionCheckRuntime.getInstance().botHasPermission(((Command) command).getLocale(), TrackerCommand.class, channelOpt.get(), Permission.READ_MESSAGES | Permission.SEND_MESSAGES | Permission.EMBED_LINKS)
        ) {
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
    }

    private ArrayList<ArrayList<TrackerBeanSlot>> getGroupedByCommandTrigger() throws SQLException {
        ArrayList<ArrayList<TrackerBeanSlot>> trackerCommandTriggerList = new ArrayList<>();

        for (Class<? extends OnTrackerRequestListener> clazz : CommandContainer.getInstance().getTrackerCommands()) {
            String commandTrigger = Command.getClassProperties((Class<? extends Command>) clazz).trigger();

            ArrayList<TrackerBeanSlot> groupedSlots = new ArrayList<>();
            DBTracker.getInstance().getBean().getSlots().stream()
                    .filter(slot -> slot.getCommandTrigger().equalsIgnoreCase(commandTrigger) && slot.isActive())
                    .forEach(groupedSlots::add);

            trackerCommandTriggerList.add(groupedSlots);
        }

        return trackerCommandTriggerList;
    }

    public void stop() {
        active = false;
    }

}
