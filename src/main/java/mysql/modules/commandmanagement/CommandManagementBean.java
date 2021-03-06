package mysql.modules.commandmanagement;

import commands.Command;
import core.CustomObservableList;
import mysql.BeanWithGuild;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;

public class CommandManagementBean extends BeanWithGuild {

    private final CustomObservableList<String> switchedOffElements;

    public CommandManagementBean(long serverId, @NonNull ArrayList<String> switchedOffElements) {
        super(serverId);
        this.switchedOffElements = new CustomObservableList<>(switchedOffElements);
    }


    /* Getters */

    public CustomObservableList<String> getSwitchedOffElements() { return switchedOffElements; }

    public boolean commandIsTurnedOn(Command command) {
        return !switchedOffElements.contains(command.getTrigger()) && !switchedOffElements.contains(command.getCategory());
    }

}