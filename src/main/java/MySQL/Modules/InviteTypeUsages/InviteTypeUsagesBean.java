package MySQL.Modules.InviteTypeUsages;

import Constants.InviteTypes;

import java.util.Observable;

public class InviteTypeUsagesBean extends Observable {

    private final InviteTypes type;
    private long value;

    public InviteTypeUsagesBean(InviteTypes type, long value) {
        this.type = type;
        this.value = value;
    }


    /* Getters */

    public InviteTypes getType() {
        return type;
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
