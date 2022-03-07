package mysql.modules.commandmanagement;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import commands.Category;
import commands.Command;
import commands.CommandContainer;
import core.CustomObservableList;
import mysql.DataWithGuild;

public class CommandManagementData extends DataWithGuild {

    private final CustomObservableList<String> switchedOffElements;

    public CommandManagementData(long serverId, List<String> switchedOffElements) {
        super(serverId);
        this.switchedOffElements = new CustomObservableList<>(switchedOffElements);
    }

    public CustomObservableList<String> getSwitchedOffElements() {
        return switchedOffElements;
    }

    public List<Category> getSwitchedOffCategories() {
        return switchedOffElements.stream()
                .map(Category::fromId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<String> getSwitchedOffCommands() {
        return switchedOffElements.stream()
                .filter(element -> CommandContainer.getCommandMap().get(element) != null)
                .collect(Collectors.toList());
    }

    public boolean commandIsTurnedOn(Command command) {
        return !switchedOffElements.contains(command.getTrigger()) &&
                !switchedOffElements.contains(command.getCategory().getId());
    }

}