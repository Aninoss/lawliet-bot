package General.Mention;
import General.StringTools;

import java.util.ArrayList;

public class MentionList<E> {

    private ArrayList<E> list;
    private String resultMessageString;

    public MentionList(String resultMessageString, ArrayList<E> list) {
        this.resultMessageString = resultMessageString;
        this.list = list;
    }

    public ArrayList<E> getList() {
        return list;
    }

    public String getResultMessageString() {
        return StringTools.trimString(resultMessageString);
    }
}
