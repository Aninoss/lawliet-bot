package General.Mention;
import General.Tools;

import java.util.ArrayList;
import java.util.List;

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
        return Tools.cutSpaces(resultMessageString);
    }
}
