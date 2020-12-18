package core;

import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.CommandProperties;
import commands.listeners.OnTrackerRequestListener;
import commands.runnables.utilitycategory.AlertsCommand;
import constants.Permission;
import mysql.modules.tracker.DBTracker;
import mysql.modules.tracker.TrackerBeanSlot;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class TrackerManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(TrackerManager.class);

    private static final TrackerManager ourInstance = new TrackerManager();
    public static TrackerManager getInstance() { return ourInstance; }
    private TrackerManager() { }

    private boolean active = false;
    private int size;

    public synchronized void start() {
        if (active) return;
        active = true;

        size = (DiscordApiManager.getInstance().getLocalShards() / 5) + 1;
        for (int i = 0; i < size; i++) {
            final int trackerShard = i;
            new CustomThread(() -> {
                manageTrackerShard(trackerShard);
            }, "trackers_" + i).start();
        }
    }

    private void manageTrackerShard(int trackerShard) {
        IntervalBlock intervalBlock = Bot.isProductionMode() ? new IntervalBlock(1, ChronoUnit.MINUTES) : new IntervalBlock(5, ChronoUnit.SECONDS);
        while (intervalBlock.block() && active) {
            try {
                for (ArrayList<TrackerBeanSlot> trackerBeanSlots : getGroupedByCommandTrigger()) {
                    if (trackerBeanSlots.size() > 0) {
                        manageTrackerCommand(trackerBeanSlots, trackerShard);
                    }
                }
            } catch (Throwable e) {
                LOGGER.error("Exception on tracker shard {}", trackerShard, e);
            }
        }
    }

    private void manageTrackerCommand(ArrayList<TrackerBeanSlot> trackerBeanSlots, int trackerShard) throws InterruptedException {
        for (TrackerBeanSlot slot : trackerBeanSlots) {
            if (!slot.getNextRequest().isAfter(Instant.now()) &&
                    trackerIsForShard(slot, trackerShard)
            ) {
                try {
                    if (slot != null) manageTracker(slot);
                } catch (InterruptedException e) {
                    throw e;
                } catch (Throwable throwable) {
                    LOGGER.error("Error in tracker \"{}\" with key \"{}\"", slot.getCommandTrigger(), slot.getCommandKey(), throwable);
                    if (throwable.toString().contains("Unknown Channel"))
                        slot.delete();
                }
            }
        }
    }

    private boolean trackerIsForShard(TrackerBeanSlot slot, int trackerShard) {
        return (slot.getServerId() >> 22) % size == trackerShard;
    }

    private void manageTracker(TrackerBeanSlot slot) throws Throwable {
        OnTrackerRequestListener command = (OnTrackerRequestListener) CommandManager.createCommandByTrigger(slot.getCommandTrigger(), slot.getServerBean().getLocale(), slot.getServerBean().getPrefix()).get();
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
            DBTracker.getInstance().getBean().getSlots().removeIf(s -> s.getChannelId() == slot.getChannelId());
        }
    }

    private ArrayList<ArrayList<TrackerBeanSlot>> getGroupedByCommandTrigger() {
        ArrayList<ArrayList<TrackerBeanSlot>> trackerCommandTriggerList = new ArrayList<>();

        for (Class<? extends OnTrackerRequestListener> clazz : CommandContainer.getInstance().getTrackerCommands()) {
            CommandProperties commandProps = Command.getClassProperties((Class<? extends Command>) clazz);
            String commandTrigger = commandProps.trigger();

            ArrayList<TrackerBeanSlot> groupedSlots = new ArrayList<>();
            new ArrayList<>(DBTracker.getInstance().getBean().getSlots()).stream()
                    .filter(slot -> (slot.getCommandTrigger().equalsIgnoreCase(commandTrigger) || Arrays.stream(commandProps.aliases()).anyMatch(alias -> slot.getCommandTrigger().equalsIgnoreCase(alias))) && slot.isActive())
                    .forEach(groupedSlots::add);

            trackerCommandTriggerList.add(groupedSlots);
        }

        return trackerCommandTriggerList;
    }

}
