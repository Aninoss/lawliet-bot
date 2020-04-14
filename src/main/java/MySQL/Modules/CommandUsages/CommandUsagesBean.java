package MySQL.Modules.CommandUsages;

import java.util.Observable;

public class CommandUsagesBean extends Observable {

    private final String command;
    private long add = 0;

    public CommandUsagesBean(String command) {
        this.command = command;
    }


    /* Getters */

    public String getCommand() {
        return command;
    }

    public long getAdd() {
        return add;
    }


    /* Setters */

    public void increase() {
        this.add++;
        setChanged();
        notifyObservers();
    }

    public void reset() {
        this.add = 0;
    }

}
