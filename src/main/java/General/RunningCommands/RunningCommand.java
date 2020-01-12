package General.RunningCommands;

import org.javacord.api.entity.user.User;

public class RunningCommand {
    private User user;
    private String commandTrigger;
    private Thread thread;
    private int shardId;

    public RunningCommand(User user, String commandTrigger, Thread thread, int shardId) {
        this.user = user;
        this.commandTrigger = commandTrigger;
        this.thread = thread;
        this.shardId = shardId;
    }

    public User getUser() {
        return user;
    }

    public String getCommandTrigger() {
        return commandTrigger;
    }

    public int getShardId() {
        return shardId;
    }

    public void stop() {
        thread.interrupt();
    }

}
