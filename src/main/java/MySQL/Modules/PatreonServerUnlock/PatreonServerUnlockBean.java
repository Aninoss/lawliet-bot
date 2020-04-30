package MySQL.Modules.PatreonServerUnlock;

import Core.CustomObservableList;
import MySQL.BeanWithServer;
import MySQL.Modules.Server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;

public class PatreonServerUnlockBean extends BeanWithServer {

    private final CustomObservableList<PatreonUserSlot> userSlots;

    public PatreonServerUnlockBean(ServerBean serverBean, @NonNull ArrayList<PatreonUserSlot> userSlots) {
        super(serverBean);
        this.userSlots = new CustomObservableList<>(userSlots);
    }


    /* Getters */

    public CustomObservableList<PatreonUserSlot> getUserSlots() {
        return userSlots;
    }

}
