package General.RunningCommands;

import org.javacord.api.entity.user.User;

public class RunningCommand {
    private User user;
    private String commandTrigger;

    public RunningCommand(User user, String commandTrigger) {
        this.user = user;
        this.commandTrigger = commandTrigger;
    }

    public User getUser() {
        return user;
    }

    public String getCommandTrigger() {
        return commandTrigger;
    }
}
