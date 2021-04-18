package mysql.modules.commandmanagement;

import java.util.ArrayList;
import commands.Command;
import core.CustomObservableList;
import mysql.DataWithGuild;
import org.checkerframework.checker.nullness.qual.NonNull;

public class CommandManagementData extends DataWithGuild {

    private final CustomObservableList<String> switchedOffElements;

    public CommandManagementData(long serverId, @NonNull ArrayList<String> switchedOffElements) {
        super(serverId);
        this.switchedOffElements = new CustomObservableList<>(switchedOffElements);
    }

    public CustomObservableList<String> getSwitchedOffElements() {
        return switchedOffElements;
    }

    public boolean commandIsTurnedOn(Command command) {
        return !switchedOffElements.contains(command.getTrigger()) && !switchedOffElements.contains(command.getCategory());
    }

}