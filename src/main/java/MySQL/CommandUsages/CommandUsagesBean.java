package MySQL.CommandUsages;

import General.DiscordApiCollection;
import MySQL.Server.ServerBean;
import org.javacord.api.entity.server.Server;

import java.util.Observable;
import java.util.Optional;

public class CommandUsagesBean extends Observable {

    private String command;
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
