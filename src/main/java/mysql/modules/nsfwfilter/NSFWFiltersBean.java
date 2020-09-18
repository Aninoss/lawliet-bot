package mysql.modules.nsfwfilter;

import core.CustomObservableList;
import mysql.BeanWithServer;
import mysql.modules.server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;

public class NSFWFiltersBean extends BeanWithServer {

    private final CustomObservableList<String> keywords;

    public NSFWFiltersBean(ServerBean serverBean, @NonNull ArrayList<String> keywords) {
        super(serverBean);
        this.keywords = new CustomObservableList<>(keywords);
    }


    /* Getters */

    public CustomObservableList<String> getKeywords() { return keywords; }

}
