package MySQL;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.user.User;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FisheryCache {

    private static FisheryCache ourInstance = new FisheryCache();
    private Map<Long, ServerTextChannel> activities = Collections.synchronizedMap(new HashMap<>());

    public static FisheryCache getInstance() {
        return ourInstance;
    }

    private FisheryCache() {
        new Thread(this::collector).start();
    }

    public void addActivity(User user, ServerTextChannel channel) {
        synchronized (this) {
            activities.put(user.getId(), channel);
        }
    }

    private void collector() {
        while(true) {
            synchronized (this) {
                if (activities.size() > 0) DBUser.addJouleBulk(activities);
                activities.clear();
            }

            try {
                Thread.sleep(20 * 1000); //Sleep for 20 seconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
