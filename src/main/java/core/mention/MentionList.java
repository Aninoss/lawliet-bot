package core.mention;

import java.util.Collections;
import java.util.List;

public class MentionList<E> {

    private final List<E> list;
    private final String filteredArgs;

    public MentionList(String filteredArgs, List<E> list) {
        this.filteredArgs = filteredArgs;
        this.list = Collections.unmodifiableList(list);
    }

    public List<E> getList() {
        return list;
    }

    public String getFilteredArgs() {
        return filteredArgs.trim().replace("  ", " ");
    }

}
