package MySQL.Modules.NSFWFilter;

import Core.CustomObservableList;
import Core.DiscordApiCollection;
import MySQL.BeanWithServer;
import MySQL.Modules.Server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.server.Server;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Optional;

public class NSFWFiltersBean extends BeanWithServer {

    private final CustomObservableList<String> keywords;

    public NSFWFiltersBean(ServerBean serverBean, @NonNull ArrayList<String> keywords) {
        super(serverBean);
        this.keywords = new CustomObservableList<>(keywords);
    }


    /* Getters */

    public CustomObservableList<String> getKeywords() { return keywords; }

}
