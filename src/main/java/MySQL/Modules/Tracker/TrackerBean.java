package MySQL.Modules.Tracker;

import CommandListeners.OnTrackerRequestListener;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import CommandSupporters.CommandManager;
import Commands.ManagementCategory.TrackerCommand;
import Constants.Permission;
import Core.*;
import Core.Utils.TimeUtil;
import ServerStuff.Discordbotlist;
import javafx.util.Pair;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class TrackerBean extends Observable {

    final static Logger LOGGER = LoggerFactory.getLogger(TrackerBean.class);

    private final CustomObservableMap<Pair<Long, String>, TrackerBeanSlot> slots;
    private boolean active = false;

    public TrackerBean(@NonNull HashMap<Pair<Long, String>, TrackerBeanSlot> slots) {
        this.slots = new CustomObservableMap<>(slots);
    }

    public void start() {
        if (active) return;
        active = true;

        new CustomThread(() -> {
            IntervalBlock intervalBlock = new IntervalBlock(1, ChronoUnit.MINUTES);
            try {
                while (intervalBlock.block() && active) {
                    for (ArrayList<TrackerBeanSlot> trackerBeanSlots : getGroupedByCommandTrigger()) {
                        if (trackerBeanSlots.size() > 0) manageTrackerCommand(trackerBeanSlots);
                    }
                }
            } catch (InterruptedException e) {
                LOGGER.error("All trackers interrupted", e);
            }
        }, "trackers", 1).start();
    }

    private void manageTrackerCommand(ArrayList<TrackerBeanSlot> trackerBeanSlots) throws InterruptedException {
        for(TrackerBeanSlot slot : trackerBeanSlots) {
            if (!slot.getNextRequest().isAfter(Instant.now())) {
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

    private void manageTracker(TrackerBeanSlot slot) throws Throwable {
        OnTrackerRequestListener command = (OnTrackerRequestListener) CommandManager.createCommandByTrigger(slot.getCommandTrigger(), slot.getServerBean().getLocale(), slot.getServerBean().getPrefix());
        Optional<ServerTextChannel> channelOpt = slot.getChannel();
        if (channelOpt.isPresent() &&
                PermissionCheckRuntime.getInstance().botHasPermission(((Command) command).getLocale(), TrackerCommand.class, channelOpt.get(),  Permission.READ_MESSAGES | Permission.SEND_MESSAGES | Permission.EMBED_LINKS)
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

    public void stop() { active = false; }




    /* Getters */

    public ArrayList<ArrayList<TrackerBeanSlot>> getGroupedByCommandTrigger() {
        ArrayList<ArrayList<TrackerBeanSlot>> trackerCommandTriggerList = new ArrayList<>();

        for(Class<? extends OnTrackerRequestListener> clazz : CommandContainer.getInstance().getTrackerCommands()) {
            String commandTrigger = Command.getTrigger((Class<? extends Command>) clazz);

            ArrayList<TrackerBeanSlot> groupedSlots = new ArrayList<>();
            getMap().values().stream()
                    .filter(slot -> slot.getCommandTrigger().equalsIgnoreCase(commandTrigger) && slot.isActive())
                    .forEach(groupedSlots::add);

            trackerCommandTriggerList.add(groupedSlots);
        }

        return trackerCommandTriggerList;
    }

    public CustomObservableMap<Pair<Long, String>, TrackerBeanSlot> getMap() { return slots; }

}
