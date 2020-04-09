package MySQL.Modules.Tracker;

import CommandListeners.OnTrackerRequestListener;
import CommandSupporters.Command;
import CommandSupporters.CommandManager;
import Commands.ManagementCategory.TrackerCommand;
import Constants.Permission;
import Core.DiscordApiCollection;
import Core.PermissionCheckRuntime;
import MySQL.Modules.Server.ServerBean;
import javafx.util.Pair;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Observable;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class TrackerBeanSlot extends Observable {

    private long serverId, channelId;
    private Long messageId;
    private ServerBean serverBean;
    private String commandTrigger, commandKey, args;
    private Instant nextRequest;
    private Thread thread;

    public TrackerBeanSlot(long serverId, ServerBean serverBean, long channelId, String commandTrigger, Long messageId, String commandKey, Instant nextRequest, String args) {
        this.serverId = serverId;
        this.serverBean = serverBean;
        this.channelId = channelId;
        this.messageId = messageId;
        this.commandTrigger = commandTrigger;
        this.commandKey = commandKey;
        this.args = args;
        this.nextRequest = nextRequest;

        if (getServer().isPresent()) start();
    }


    /* Getters */

    public long getServerId() { return serverId; }

    public Optional<Server> getServer() { return DiscordApiCollection.getInstance().getServerById(serverId); }

    public ServerBean getServerBean() {
        return serverBean;
    }

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

    private void start() {
        Thread t = new Thread(() -> {
            synchronized (DBTracker.getInstance()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //Ignore
                }
            }

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
                        //Ignore
                        return;
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    firstTime = false;
                } while(cont);
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        });
        t.setName("tracker_" + commandTrigger);
        t.setPriority(1);
        t.start();
    }

    private boolean manageTracker(OnTrackerRequestListener command, boolean firstTime) throws Throwable {
        Duration duration = Duration.between(Instant.now(), nextRequest);
        Thread.sleep(Math.max(firstTime ? 0 : 5 * 60 * 1000, duration.getSeconds() * 1000 + duration.getNano() / 1000000));

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
                    notifyObservers();
                    return false;

                case CONTINUE:
                    return true;

                case CONTINUE_AND_SAVE:
                    notifyObservers();
                    return true;
            }
        }

        return false;
    }

    public void stop() {
        thread.interrupt();
    }

    public void delete() {
        try {
            DBTracker.getInstance().getBean().getMap().remove(new Pair<>(channelId, commandTrigger));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
