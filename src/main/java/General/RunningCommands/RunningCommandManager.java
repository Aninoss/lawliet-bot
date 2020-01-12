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

    public synchronized boolean canUserRunCommand(User user, String commandTrigger, int shardId) {
        Pair<RunningCommand, Instant> runningPair = find(user, commandTrigger);

        if (runningPair == null) {
            runningPair = new Pair<>(new RunningCommand(user, commandTrigger, Thread.currentThread(), shardId), Instant.now());
            runningCommands.add(runningPair);
            return true;
        } else {
            Instant time = runningPair.getValue();
            if (time.plusSeconds(60).isBefore(Instant.now())) {
                runningCommands.remove(runningPair);
                runningPair = new Pair<>(new RunningCommand(user, commandTrigger, Thread.currentThread(), shardId), Instant.now());
                runningCommands.add(runningPair);
                return true;
            }
            return false;
        }
    }

    public void add(User user, String commandTrigger, int shardId) {
        canUserRunCommand(user, commandTrigger, shardId);
    }

    public synchronized void remove(User user, String commandTrigger) {
        Pair<RunningCommand, Instant> runningCommand = find(user, commandTrigger);

        if (runningCommand != null) {
            runningCommands.remove(runningCommand);
        }
    }

    public synchronized Pair<RunningCommand, Instant> find(User user, String commandTrigger) {
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

    public synchronized void clearShard(int shardId) {
        for(Pair<RunningCommand, Instant> runningCommandPair: runningCommands) {
            RunningCommand runningCommand = runningCommandPair.getKey();
            if (runningCommand.getShardId() == shardId) {
                runningCommands.remove(runningCommandPair);
                runningCommand.stop();
            }
        }
    }

}
