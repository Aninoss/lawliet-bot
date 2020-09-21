package mysql.modules.tracker;

import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.OnTrackerRequestListener;
import commands.runnables.managementcategory.TrackerCommand;
import constants.Permission;
import constants.Settings;
import core.CustomObservableList;
import core.CustomThread;
import core.IntervalBlock;
import core.PermissionCheckRuntime;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Optional;

public class TrackerBean extends Observable {

    private final static Logger LOGGER = LoggerFactory.getLogger(TrackerBean.class);

    private final CustomObservableList<TrackerBeanSlot> slots;
    private boolean active = false;

    public TrackerBean(@NonNull ArrayList<TrackerBeanSlot> slots) {
        this.slots = new CustomObservableList<>(slots);
    }

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
        try {
            while (intervalBlock.block() && active) {
                for (ArrayList<TrackerBeanSlot> trackerBeanSlots : getGroupedByCommandTrigger()) {
                    if (trackerBeanSlots.size() > 0) {
                        manageTrackerCommand(trackerBeanSlots, trackerShard);
                    }
                }
            }
        } catch (InterruptedException e) {
            LOGGER.error("All trackers interrupted", e);
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

    public void stop() {
        active = false;
    }



    /* Getters */

    public ArrayList<ArrayList<TrackerBeanSlot>> getGroupedByCommandTrigger() {
        ArrayList<ArrayList<TrackerBeanSlot>> trackerCommandTriggerList = new ArrayList<>();

        for (Class<? extends OnTrackerRequestListener> clazz : CommandContainer.getInstance().getTrackerCommands()) {
            String commandTrigger = Command.getClassProperties((Class<? extends Command>) clazz).trigger();

            ArrayList<TrackerBeanSlot> groupedSlots = new ArrayList<>();
            getSlots().stream()
                    .filter(slot -> slot.getCommandTrigger().equalsIgnoreCase(commandTrigger) && slot.isActive())
                    .forEach(groupedSlots::add);

            trackerCommandTriggerList.add(groupedSlots);
        }

        return trackerCommandTriggerList;
    }

    public CustomObservableList<TrackerBeanSlot> getSlots() {
        return slots;
    }

}
