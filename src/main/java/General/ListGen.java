package General;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public class ListGen<T> {

    public static final int SLOT_TYPE_NONE = 0;
    public static final int SLOT_TYPE_BULLET = 1;
    public static final int SLOT_TYPE_NUMBERED = 2;

    public ListGen() { }

    public String getList(ArrayList<T> objs, Locale locale, Function<T, String> getNames) {
        return getList(objs, locale, SLOT_TYPE_NONE, getNames);
    }

    public String getList(ArrayList<T> objs, String valueIfEmpty, Function<T, String> getNames) {
        return getList(objs, valueIfEmpty, SLOT_TYPE_NONE, getNames);
    }

    public String getList(ArrayList<T> objs, Function<T, String> getNames) {
        return getList(objs, SLOT_TYPE_NONE, getNames);
    }

    public String getList(ArrayList<T> objs, Locale locale, int slotType, Function<T, String> getNames) {
        String valueIfEmpty = "";
        try {
            valueIfEmpty = TextManager.getString(locale, TextManager.GENERAL, "notset");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return getList(objs, valueIfEmpty, slotType, getNames);
    }

    public String getList(ArrayList<T> objs, int slotType, Function<T, String> getNames) {
        return getList(objs, "", slotType, getNames);
    }

    public String getList(ArrayList<T> objs, String valueIfEmpty, int slotType, Function<T, String> getNames) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for(T obj: objs) {
            try {
                String value = getNames.apply(obj);
                switch (slotType) {
                    case SLOT_TYPE_BULLET:
                        sb.append("• ");
                        break;

                    case SLOT_TYPE_NUMBERED:
                        sb.append(i).append(") ");
                        break;
                }
                sb.append(value);
                sb.append("\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
            i++;
        }
        if (sb.toString().isEmpty()) return valueIfEmpty;
        return sb.toString();
    }

}
