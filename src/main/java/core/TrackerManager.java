package core;

import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.CommandProperties;
import commands.listeners.OnTrackerRequestListener;
import commands.runnables.utilitycategory.AlertsCommand;
import constants.Permission;
import mysql.modules.tracker.TrackerBean;
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

    private TrackerBean trackerBean;
    private boolean active = false;

    public synchronized void start(TrackerBean trackerBean) {
        if (active) return;
        active = true;

        this.trackerBean = trackerBean;
        new CustomThread(this::manageTrackerShard, "alerts").start();
    }

    private void manageTrackerShard() {
        IntervalBlock intervalBlock = Bot.isProductionMode() ? new IntervalBlock(1, ChronoUnit.MINUTES) : new IntervalBlock(5, ChronoUnit.SECONDS);
        while (intervalBlock.block() && active) {
            if (Bot.isProductionMode())
                LOGGER.info("Starting new alerts cycle");
            try {
                for (ArrayList<TrackerBeanSlot> trackerBeanSlots : getGroupedByCommandTrigger()) {
                    if (trackerBeanSlots.size() > 0) {
                        manageTrackerCommand(trackerBeanSlots);
                    }
                }
            } catch (Throwable e) {
                LOGGER.error("Exception on tracker", e);
            }
        }
    }

    private void manageTrackerCommand(ArrayList<TrackerBeanSlot> trackerBeanSlots) throws InterruptedException {
        for (TrackerBeanSlot slot : trackerBeanSlots) {
            if (!slot.getNextRequest().isAfter(Instant.now())) {
                try {
                    if (slot.getServerId() == 797610509259374603L) //TODO Debug
                        LOGGER.info("alert {}: 0", slot.getCommandKey());
                    manageTracker(slot);
                } catch (InterruptedException e) {
                    if (slot.getServerId() == 797610509259374603L) //TODO Debug
                        LOGGER.info("alert {}: interrupted", slot.getCommandKey());
                    throw e;
                } catch (Throwable throwable) {
                    LOGGER.error("Error in tracker \"{}\" with key \"{}\"", slot.getCommandTrigger(), slot.getCommandKey(), throwable);
                    if (throwable.toString().contains("Unknown Channel"))
                        slot.delete();
                }
            }
        }
    }

    private void manageTracker(TrackerBeanSlot slot) throws Throwable {
        OnTrackerRequestListener command = (OnTrackerRequestListener) CommandManager.createCommandByTrigger(slot.getCommandTrigger(), slot.getServerBean().getLocale(), slot.getServerBean().getPrefix()).get();
        if (slot.getServerId() == 797610509259374603L) //TODO Debug
            LOGGER.info("alert {}: 1", slot.getCommandKey());
        Optional<ServerTextChannel> channelOpt = slot.getChannel();
        if (slot.getServerId() == 797610509259374603L) //TODO Debug
            LOGGER.info("alert {}: 2", slot.getCommandKey());
        if (channelOpt.isPresent()) {
            if (slot.getServerId() == 797610509259374603L) //TODO Debug
                LOGGER.info("alert {}: 3", slot.getCommandKey());
            if (PermissionCheckRuntime.getInstance().botHasPermission(((Command) command).getLocale(), AlertsCommand.class, channelOpt.get(), Permission.READ_MESSAGES | Permission.SEND_MESSAGES | Permission.EMBED_LINKS)) {
                if (slot.getServerId() == 797610509259374603L) //TODO Debug
                    LOGGER.info("alert {}: 4", slot.getCommandKey());
                switch (command.onTrackerRequest(slot)) {
                    case STOP:
                        if (slot.getServerId() == 797610509259374603L) //TODO Debug
                            LOGGER.info("alert {}: 5 stop", slot.getCommandKey());
                        slot.stop();
                        break;

                    case STOP_AND_DELETE:
                        if (slot.getServerId() == 797610509259374603L) //TODO Debug
                            LOGGER.info("alert {}: 5 stop and delete", slot.getCommandKey());
                        slot.delete();
                        break;

                    case STOP_AND_SAVE:
                        if (slot.getServerId() == 797610509259374603L) //TODO Debug
                            LOGGER.info("alert {}: 5 stop and save", slot.getCommandKey());
                        slot.stop();
                        slot.save();
                        break;

                    case CONTINUE:
                        if (slot.getServerId() == 797610509259374603L) //TODO Debug
                            LOGGER.info("alert {}: 5 continue", slot.getCommandKey());
                        break;

                    case CONTINUE_AND_SAVE:
                        if (slot.getServerId() == 797610509259374603L) //TODO Debug
                            LOGGER.info("alert {}: 5 continue and save", slot.getCommandKey());
                        slot.save();
                        break;
                }
                if (slot.getServerId() == 797610509259374603L) //TODO Debug
                    LOGGER.info("alert {}: 6 stop", slot.getCommandKey());
            }
        } else if (slot.getServer().isPresent()) {
            if (slot.getServerId() == 797610509259374603L) //TODO Debug
                LOGGER.info("alert {}: channel not present", slot.getCommandKey());
            trackerBean.getSlots().stream()
                    .filter(s -> s.getChannelId() == slot.getChannelId())
                    .forEach(TrackerBeanSlot::delete);
        }
    }

    private ArrayList<ArrayList<TrackerBeanSlot>> getGroupedByCommandTrigger() {
        ArrayList<ArrayList<TrackerBeanSlot>> trackerCommandTriggerList = new ArrayList<>();

        for (Class<? extends OnTrackerRequestListener> clazz : CommandContainer.getInstance().getTrackerCommands()) {
            CommandProperties commandProps = Command.getClassProperties((Class<? extends Command>) clazz);
            String commandTrigger = commandProps.trigger();

            ArrayList<TrackerBeanSlot> groupedSlots = new ArrayList<>();
            new ArrayList<>(trackerBean.getSlots()).stream()
                    .filter(slot -> (slot.getCommandTrigger().equalsIgnoreCase(commandTrigger) || Arrays.stream(commandProps.aliases()).anyMatch(alias -> slot.getCommandTrigger().equalsIgnoreCase(alias))) && slot.isActive())
                    .forEach(groupedSlots::add);

            trackerCommandTriggerList.add(groupedSlots);
        }

        return trackerCommandTriggerList;
    }

}
