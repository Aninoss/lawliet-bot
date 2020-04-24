package MySQL.Modules.Tracker;

import CommandListeners.OnTrackerRequestListener;
import CommandSupporters.Command;
import CommandSupporters.CommandManager;
import Commands.ManagementCategory.TrackerCommand;
import Constants.Permission;
import Core.CustomThread;
import Core.PermissionCheckRuntime;
import Core.Utils.TimeUtil;
import MySQL.BeanWithServer;
import MySQL.Modules.Server.ServerBean;
import javafx.util.Pair;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class TrackerBeanSlot extends BeanWithServer {

    final static Logger LOGGER = LoggerFactory.getLogger(TrackerBeanSlot.class);

    private final long channelId;
    private Long messageId;
    private final String commandTrigger, commandKey;
    private String args;
    private Instant nextRequest;
    private Thread thread = null;

    public TrackerBeanSlot(ServerBean serverBean, long channelId, String commandTrigger, Long messageId, String commandKey, Instant nextRequest, String args) {
        super(serverBean);
        this.channelId = channelId;
        this.messageId = messageId;
        this.commandTrigger = commandTrigger;
        this.commandKey = commandKey;
        this.args = args;
        this.nextRequest = nextRequest;

        if (getServer().isPresent()) start(serverBean);
    }


    /* Getters */

    public long getChannelId() { return channelId; }

    public Optional<ServerTextChannel> getChannel() { return getServer().flatMap(server -> server.getTextChannelById(channelId)); }

    public Optional<Long> getMessageId() { return Optional.ofNullable(messageId); }

    public Optional<Message> getMessage() {
        return getChannel().flatMap(channel -> {
            try {
                return Optional.ofNullable(channel.getMessageById(messageId != null ? messageId : 0L).get());
            } catch (InterruptedException | ExecutionException e) {
                //Ignore
            }
            return Optional.empty();
        });
    }

    public String getCommandTrigger() { return commandTrigger; }

    public Optional<String> getCommandKey() { return Optional.ofNullable(commandKey); }

    public Optional<String> getArgs() { return Optional.ofNullable(args); }

    public Instant getNextRequest() { return nextRequest; }

    public Thread getThread() { return thread; }


    /* Setters */

    public void setMessageId(Long messageId) {
        if (this.messageId == null || !this.messageId.equals(messageId)) {
            this.messageId = messageId;
            setChanged();
        }
    }

    public void setArgs(String args) {
        if (this.args == null || !this.args.equals(args)) {
            this.args = args;
            setChanged();
        }
    }

    public void setNextRequest(Instant nextRequest) {
        if (this.nextRequest == null || !this.nextRequest.equals(nextRequest)) {
            this.nextRequest = nextRequest;
            setChanged();
        }
    }


    /* Actions */

    private void start(ServerBean serverBean) {
        Thread t = new CustomThread(() -> {
            thread = Thread.currentThread();
            Locale locale = serverBean.getLocale();
            String prefix = serverBean.getPrefix();
            boolean firstTime = true;
            try {
                OnTrackerRequestListener command = (OnTrackerRequestListener) CommandManager.createCommandByTrigger(commandTrigger, locale, prefix);
                boolean cont = true;
                do {
                    try {
                        cont = manageTracker(command, firstTime);
                    } catch (InterruptedException e) {
                        LOGGER.info("Tracker {} on server {} interrupted", commandTrigger, getServerId());
                        return;
                    } catch (Throwable e) {
                        LOGGER.error("Could not manage tracker", e);
                    }
                    firstTime = false;
                } while(cont);
            } catch (IllegalAccessException | InstantiationException e) {
                LOGGER.error("Could not create command", e);
            }
        }, "tracker_" + serverBean.getServerId() + "_" + commandTrigger, 1);
        t.start();
    }

    private boolean manageTracker(OnTrackerRequestListener command, boolean firstTime) throws Throwable {
        Thread.sleep(Math.max(firstTime ? 0 : 5 * 60 * 1000, TimeUtil.getMilisBetweenInstants(Instant.now(), nextRequest)));

        Optional<ServerTextChannel> channelOpt = getChannel();
        if (channelOpt.isPresent() &&
                PermissionCheckRuntime.getInstance().botHasPermission(((Command)command).getLocale(), TrackerCommand.class, channelOpt.get(),  Permission.READ_MESSAGES | Permission.SEND_MESSAGES | Permission.EMBED_LINKS)
        ) {
            switch (command.onTrackerRequest(this)) {
                case STOP:
                    return false;

                case STOP_AND_DELETE:
                    delete();
                    stop();
                    return false;

                case STOP_AND_SAVE:
                    while (countObservers() == 0) Thread.sleep(100);
                    setChanged();
                    notifyObservers();
                    return false;

                case CONTINUE:
                    return true;

                case CONTINUE_AND_SAVE:
                    while (countObservers() == 0) Thread.sleep(100);
                    setChanged();
                    notifyObservers();
                    return true;
            }
        }

        return false;
    }

    public void stop() {
        if (thread != null) thread.interrupt();
    }

    public void delete() {
        try {
            DBTracker.getInstance().getBean().getMap().remove(new Pair<>(channelId, commandTrigger));
        } catch (SQLException e) {
            LOGGER.error("Could not remove tracker", e);
        }
    }

}
