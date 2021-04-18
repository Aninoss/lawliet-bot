package mysql.modules.nsfwfilter;

import java.util.ArrayList;
import core.CustomObservableList;
import mysql.DataWithGuild;
import org.checkerframework.checker.nullness.qual.NonNull;

public class NSFWFiltersData extends DataWithGuild {

    private final CustomObservableList<String> keywords;

    public NSFWFiltersData(long serverId, @NonNull ArrayList<String> keywords) {
        super(serverId);
        this.keywords = new CustomObservableList<>(keywords);
    }

    public CustomObservableList<String> getKeywords() {
        return keywords;
    }

}
