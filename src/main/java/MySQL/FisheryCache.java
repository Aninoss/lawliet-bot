package MySQL;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.user.UserStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
        Thread t = new Thread(this::messageCollector);
        t.setPriority(1);
        t.setName("message_collector");
        t.start();
    }

    public void addActivity(User user, ServerTextChannel channel) {
        //synchronized (this) {
            int count = 0;
            if (userMessageCount.containsKey(user.getId())) count = userMessageCount.get(user.getId());

            if (count < 300) {
                ActivityUserData activityUserData = activities.get(user.getId());
                if (activityUserData == null) activityUserData = new ActivityUserData(channel);
                if (activityUserData.register(messagePhase))
                    userMessageCount.put(user.getId(), count + 1);
                activities.put(user.getId(), activityUserData);
            }
        //}
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
            if (messagePhase >= 3 * 15) {
                messagePhase = 0;

                Map<Long, ActivityUserData> activitesClone = null;
                System.out.print("0");
                //synchronized (this) {
                boolean finished = false;
                while (!finished) {
                    try {
                        activitesClone = new HashMap<>(activities);
                        activities.clear();
                        finished = true;
                    } catch (Throwable e) {
                        e.printStackTrace();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
                
                //}
                System.out.print("1");
                if (activitesClone.size() > 0) DBUser.addMessageFishBulk(activitesClone);
                System.out.print("2");
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
                for (ServerVoiceChannel channel : server.getVoiceChannels()) {
                    ArrayList<User> validUsers = new ArrayList<>();
                    for (User user : channel.getConnectedUsers()) {
                        if (!user.isBot() &&
                                !user.isMuted(server) &&
                                !user.isDeafened(server) &&
                                !user.isSelfDeafened(server)
                        ) {
                            validUsers.add(user);
                        }
                    }

                    if (validUsers.size() > 1 &&
                            (!server.getAfkChannel().isPresent() || channel.getId() != server.getAfkChannel().get().getId())
                    ) {
                        for (User user : validUsers) {
                            int count = 0;
                            if (userVCCount.containsKey(user.getId())) count = userVCCount.get(user.getId());
                            count++;

                            userVCCount.put(user.getId(), count);
                            userVCActivities.put(user.getId(), server.getId());
                        }
                    }
                }
            }

            if (userVCActivities.size() > 0) DBUser.addVCFishBulk(userVCActivities, 5);
        }
    }

    public void startVCCollector(DiscordApi api) {
        Thread t = new Thread(() -> VCCollector(api));
        t.setPriority(1);
        t.setName("vc_collector");
        t.start();
    }

    public void reset() {
        userMessageCount = Collections.synchronizedMap(new HashMap<>());
        userVCCount = Collections.synchronizedMap(new HashMap<>());
    }

}
