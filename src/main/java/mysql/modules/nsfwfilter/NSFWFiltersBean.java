package mysql.modules.nsfwfilter;

import core.CustomObservableList;
import mysql.BeanWithServer;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;

public class NSFWFiltersBean extends BeanWithServer {

    private final CustomObservableList<String> keywords;

    public NSFWFiltersBean(long serverId, @NonNull ArrayList<String> keywords) {
        super(serverId);
        this.keywords = new CustomObservableList<>(keywords);
    }


    /* Getters */

    public CustomObservableList<String> getKeywords() { return keywords; }

}
