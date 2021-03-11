package mysql.modules.nsfwfilter;

import java.util.ArrayList;
import core.CustomObservableList;
import mysql.BeanWithGuild;
import org.checkerframework.checker.nullness.qual.NonNull;

public class NSFWFiltersBean extends BeanWithGuild {

    private final CustomObservableList<String> keywords;

    public NSFWFiltersBean(long serverId, @NonNull ArrayList<String> keywords) {
        super(serverId);
        this.keywords = new CustomObservableList<>(keywords);
    }


    /* Getters */

    public CustomObservableList<String> getKeywords() { return keywords; }

}
