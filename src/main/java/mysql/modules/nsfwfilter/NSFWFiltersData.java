package mysql.modules.nsfwfilter;

import java.util.List;
import core.CustomObservableList;
import mysql.DataWithGuild;

public class NSFWFiltersData extends DataWithGuild {

    private final CustomObservableList<String> keywords;

    public NSFWFiltersData(long serverId, List<String> keywords) {
        super(serverId);
        this.keywords = new CustomObservableList<>(keywords);
    }

    public CustomObservableList<String> getKeywords() {
        return keywords;
    }

}
