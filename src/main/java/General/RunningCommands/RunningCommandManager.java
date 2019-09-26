package General.RunningCommands;

import CommandSupporters.Command;
import org.javacord.api.entity.user.User;

import java.util.ArrayList;

public class RunningCommandManager {
    private static RunningCommandManager ourInstance = new RunningCommandManager();

    private ArrayList<RunningCommand> runningCommands;

    public static RunningCommandManager getInstance() {
        return ourInstance;
    }

    private RunningCommandManager() {
        clear();
    }

    public boolean canUserRunCommand(User user, String commandTrigger) {
        RunningCommand runningCommand = find(user, commandTrigger);

        if (runningCommand == null) {
            runningCommand = new RunningCommand(user, commandTrigger);
            runningCommands.add(runningCommand);
            return true;
        } else {
            return false;
        }
    }

    public void add(User user, String commandTrigger) {
        canUserRunCommand(user, commandTrigger);
    }

    public void remove(User user, String commandTrigger) {
        RunningCommand runningCommand = find(user, commandTrigger);

        if (runningCommand != null) {
            runningCommands.remove(runningCommand);
        }
    }

    public RunningCommand find(User user, String commandTrigger) {
        for(RunningCommand runningCommand: runningCommands) {
            if (runningCommand != null && runningCommand.getUser().getId() == user.getId() && runningCommand.getCommandTrigger().equals(commandTrigger)) return runningCommand;
        }
        return null;
    }

    public ArrayList<RunningCommand> getRunningCommands() {
        return (ArrayList<RunningCommand>) runningCommands.clone();
    }

    public void clear() {
        runningCommands = new ArrayList<>();
    }
}
