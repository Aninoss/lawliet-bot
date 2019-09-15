package MySQL;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.user.UserStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FisheryCache {

    private static FisheryCache ourInstance = new FisheryCache();

    private Map<Long, ServerTextChannel> activities = Collections.synchronizedMap(new HashMap<>()); //userId, channel

    private Map<Long, Integer> userMessageCount = Collections.synchronizedMap(new HashMap<>());
    private Map<Long, Integer> userVCCount = Collections.synchronizedMap(new HashMap<>());

    private Instant nextMessageCheck = Instant.now(), nextVCCheck = Instant.now();

    public static FisheryCache getInstance() {
        return ourInstance;
    }

    private FisheryCache() {
        new Thread(this::messageCollector).start();
    }

    public void addActivity(User user, ServerTextChannel channel) {
        synchronized (this) {
            if (!userMessageCount.containsKey(user.getId()) || userMessageCount.get(user.getId()) < 300) activities.put(user.getId(), channel);
        }
    }

    private void messageCollector() {
        while(true) {
            try {
                Duration duration = Duration.between(Instant.now(), nextMessageCheck);
                Thread.sleep(Math.max(1, duration.getSeconds() * 1000 + duration.getNano() / 1000000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            nextMessageCheck = Instant.now().plusSeconds(20);

            synchronized (this) {
                for(long userId: activities.keySet()) {
                    int count = 0;
                    if (userMessageCount.containsKey(userId)) count = userMessageCount.get(userId);
                    count++;

                    userMessageCount.put(userId, count);
                }

                if (activities.size() > 0) DBUser.addMessageFishBulk(activities);
                activities.clear();
            }
        }
    }

    private void VCCollector(DiscordApi api) {
        while(true) {
            try {
                Duration duration = Duration.between(Instant.now(), nextVCCheck);
                Thread.sleep(Math.max(1, duration.getSeconds() * 1000 + duration.getNano() / 1000000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            nextVCCheck = Instant.now().plusSeconds(5 * 60);

            Map<Long, Long> userVCActivities = Collections.synchronizedMap(new HashMap<>()); //userId, serverId

            for(Server server: api.getServers()) {
                for(ServerVoiceChannel channel: server.getVoiceChannels()) {
                    if (channel.getConnectedUsers().size() > 1 &&
                            (!server.getAfkChannel().isPresent() || channel.getId() != server.getAfkChannel().get().getId())
                    ) {
                        for(User user: channel.getConnectedUsers()) {
                            if (user.getStatus() != UserStatus.IDLE &&
                                    (!userVCCount.containsKey(user.getId()) || userVCCount.get(user.getId()) < 60)
                            ) {
                                int count = 0;
                                if (userVCCount.containsKey(user.getId())) count = userVCCount.get(user.getId());
                                count++;

                                userVCCount.put(user.getId(), count);
                                userVCActivities.put(user.getId(), server.getId());
                            }
                        }
                    }
                }
            }

            if (userVCActivities.size() > 0) DBUser.addVCFishBulk(userVCActivities, 5);
        }
    }

    public void startVCCollector(DiscordApi api) {
        new Thread(() -> VCCollector(api)).start();
    }

    public void reset() {
        userMessageCount = Collections.synchronizedMap(new HashMap<>());
        userVCCount = Collections.synchronizedMap(new HashMap<>());
    }

}
