package mysql.modules.commandusages;

import java.util.Observable;

public class CommandUsagesBean extends Observable {

    private final String command;
    private long value;

    public CommandUsagesBean(String command, long value) {
        this.command = command;
        this.value = value;
    }


    /* Getters */

    public String getCommand() {
        return command;
    }

    public long getValue() {
        return value;
    }


    /* Setters */

    public void increase() {
        this.value++;
        setChanged();
        notifyObservers();
    }

}
