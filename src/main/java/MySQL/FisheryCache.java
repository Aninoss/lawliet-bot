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

    private Map<Long, ActivityUserData> activities = new HashMap<>(); //userId, channel
    private int messagePhase = 0;

    private Map<Long, Integer> userMessageCount = new HashMap<>();
    private Map<Long, Integer> userVCCount = new HashMap<>();

    private Instant nextMessageCheck = Instant.now(), nextVCCheck = Instant.now();

    public static FisheryCache getInstance() {
        return ourInstance;
    }

    private FisheryCache() {
        new Thread(this::messageCollector).start();
    }

    public void addActivity(User user, ServerTextChannel channel) {
        synchronized (this) {
            int count = 0;
            if (userMessageCount.containsKey(user.getId())) count = userMessageCount.get(user.getId());

            if (count < 300) {
                ActivityUserData activityUserData = activities.get(user.getId());
                if (activityUserData == null) activityUserData = new ActivityUserData(channel);
                if (activityUserData.register(messagePhase))
                    userMessageCount.put(user.getId(), count + 1);
                activities.put(user.getId(), activityUserData);
            }
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

            messagePhase++;
            if (messagePhase >= 3 * 10) {
                messagePhase = 0;

                System.out.println("Message Start");

                Map<Long, ActivityUserData> activitesClone;
                synchronized (this) {
                    activitesClone = new HashMap<>(activities);
                    activities.clear();
                }
                if (activitesClone.size() > 0) DBUser.addMessageFishBulk(activitesClone);

                System.out.println("Message End");
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

            System.out.println("VC Start");

            Map<Long, Long> userVCActivities = Collections.synchronizedMap(new HashMap<>()); //userId, serverId

            for(Server server: api.getServers()) {
                for(ServerVoiceChannel channel: server.getVoiceChannels()) {
                    int connectedUsers = 0;
                    for(User user: channel.getConnectedUsers())
                        if (!user.isBot()) connectedUsers++;

                    if (connectedUsers > 1 &&
                            (!server.getAfkChannel().isPresent() || channel.getId() != server.getAfkChannel().get().getId())
                    ) {
                        for(User user: channel.getConnectedUsers()) {
                            if (!user.isBot() &&
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

            System.out.println("VC End");
        }
    }

    public void startVCCollector(DiscordApi api) {
        new Thread(() -> VCCollector(api)).start();
    }

    public synchronized void reset() {
        userMessageCount = Collections.synchronizedMap(new HashMap<>());
        userVCCount = Collections.synchronizedMap(new HashMap<>());
    }

}
