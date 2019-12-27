package General.RunningCommands;

import CommandSupporters.Command;
import General.Pair;
import org.javacord.api.entity.user.User;

import java.time.Instant;
import java.util.ArrayList;

public class RunningCommandManager {
    private static RunningCommandManager ourInstance = new RunningCommandManager();

    private ArrayList<Pair<RunningCommand, Instant>> runningCommands = new ArrayList<>();

    public static RunningCommandManager getInstance() {
        return ourInstance;
    }

    private RunningCommandManager() {
        clear();
    }

    public boolean canUserRunCommand(User user, String commandTrigger) {
        Pair<RunningCommand, Instant> runningPair = find(user, commandTrigger);

        if (runningPair == null) {
            runningPair = new Pair<>(new RunningCommand(user, commandTrigger), Instant.now());
            runningCommands.add(runningPair);
            return true;
        } else {
            Instant time = runningPair.getValue();
            if (time.plusSeconds(60).isBefore(Instant.now())) {
                runningCommands.remove(runningPair);
                runningPair = new Pair<>(new RunningCommand(user, commandTrigger), Instant.now());
                runningCommands.add(runningPair);
                return true;
            }
            return false;
        }
    }

    public void add(User user, String commandTrigger) {
        canUserRunCommand(user, commandTrigger);
    }

    public void remove(User user, String commandTrigger) {
        Pair<RunningCommand, Instant> runningCommand = find(user, commandTrigger);

        if (runningCommand != null) {
            runningCommands.remove(runningCommand);
        }
    }

    public Pair<RunningCommand, Instant> find(User user, String commandTrigger) {
        for(Pair<RunningCommand, Instant> runningPair: runningCommands) {
            if (runningPair != null) {
                RunningCommand runningCommand = runningPair.getKey();
                if (runningCommand != null && runningCommand.getUser().getId() == user.getId() && runningCommand.getCommandTrigger().equals(commandTrigger))
                    return runningPair;
            }
        }
        return null;
    }

    public ArrayList<Pair<RunningCommand, Instant>> getRunningCommands() {
        return new ArrayList<>(runningCommands);
    }

    public void clear() {
        runningCommands = new ArrayList<>();
    }
}
