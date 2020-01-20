package General.Cooldown;

import org.javacord.api.entity.user.User;

import java.util.ArrayList;

public class Cooldown {

    public static final int COOLDOWN_TIME_IN_SECONDS = 6;
    public static final int MAX_ALLOWED = 3;

    private static Cooldown ourInstance = new Cooldown();
    private ArrayList<CooldownData> dataList;

    public static Cooldown getInstance() {
        return ourInstance;
    }

    private Cooldown() {
        dataList = new ArrayList<>();
    }

    public boolean canPost(User user) {
        CooldownData data = addValue(user);
        return data.canPost();
    }

    private synchronized CooldownData addValue(User user) {
        CooldownData data = find(user);

        if (data == null) {
            data = new CooldownData(user);
            dataList.add(data);
        } else data.plus();

        return data;
    }

    private synchronized CooldownData find(User user) {
        for(CooldownData data: dataList) {
            if (data.getUserId() == user.getId()) return data;
        }
        return null;
    }

    public void setBotIsSending(User user, boolean botIsSending) {
        CooldownData data = find(user);
        if (data != null ) data.setBotIsSending(botIsSending);
    }

    public boolean isBotIsSending(User user) {
        CooldownData data = find(user);
        if (data != null ) return data.isBotIsSending();
        else return false;
    }

    public synchronized void clean() {
        dataList.removeIf(CooldownData::canPost);
    }
}
