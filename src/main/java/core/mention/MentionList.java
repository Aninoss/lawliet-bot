package core.mention;
import core.utils.StringUtil;

import java.util.ArrayList;

public class MentionList<E> {

    private final ArrayList<E> list;
    private final String resultMessageString;

    public MentionList(String resultMessageString, ArrayList<E> list) {
        this.resultMessageString = resultMessageString;
        this.list = list;
    }

    public ArrayList<E> getList() {
        return list;
    }

    public String getResultMessageString() {
        return resultMessageString.trim();
    }
}
