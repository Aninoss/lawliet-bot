package core.mention;

import java.util.Collections;
import java.util.List;

public class MentionList<E> {

    private final List<E> list;
    private final String resultMessageString;

    public MentionList(String resultMessageString, List<E> list) {
        this.resultMessageString = resultMessageString;
        this.list = Collections.unmodifiableList(list);
    }

    public List<E> getList() {
        return list;
    }

    public String getResultMessageString() {
        return resultMessageString.trim();
    }

}
