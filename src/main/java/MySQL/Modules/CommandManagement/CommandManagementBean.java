package MySQL.Modules.CommandManagement;

import CommandSupporters.Command;
import Core.CustomObservableList;
import MySQL.BeanWithServer;
import MySQL.Modules.Server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;

public class CommandManagementBean extends BeanWithServer {

    private final CustomObservableList<String> switchedOffElements;

    public CommandManagementBean(ServerBean serverBean, @NonNull ArrayList<String> switchedOffElements) {
        super(serverBean);
        this.switchedOffElements = new CustomObservableList<>(switchedOffElements);
    }


    /* Getters */

    public CustomObservableList<String> getSwitchedOffElements() { return switchedOffElements; }

    public boolean commandIsTurnedOn(Command command) {
        return !switchedOffElements.contains(command.getClassTrigger()) && !switchedOffElements.contains(command.getCategory());
    }

}