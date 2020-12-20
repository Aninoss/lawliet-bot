package mysql.modules.commandusages;

import java.util.Observable;

public class CommandUsagesBean extends Observable {

    private final String command;
    private long value;
    private long increment = 0;

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

    public long flushIncrement() {
        long incTemp = increment;
        increment = 0;
        return incTemp;
    }


    /* Setters */

    public void increase() {
        this.value++;
        this.increment++;
        setChanged();
        notifyObservers();
    }

}
