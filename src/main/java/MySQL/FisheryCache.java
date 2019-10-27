package MySQL;

import Constants.PowerPlantStatus;
import General.Pair;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FisheryCache {

    private static FisheryCache ourInstance = new FisheryCache();

    private Map<Long, Map<Long, ActivityUserData>> activities = new HashMap<>(); //serverId, userId, activity
    private int messagePhase = 0;

    private Map<Long, Map<Long, Integer>> userMessageCount = new HashMap<>(); //serverId, userId, counter
    private Map<Long, Map<Long, Integer>> userVCCount = new HashMap<>(); //serverId, userId, counter

    private Instant nextMessageCheck = Instant.now(), nextVCCheck = Instant.now();

    public static FisheryCache getInstance() {
        return ourInstance;
    }

    private FisheryCache() {
        Thread t = new Thread(this::messageCollector);
        t.setName("message_collector");
        t.start();
    }

    public void addActivity(User user, ServerTextChannel channel) {
        int count = getUserMessageCount(channel.getServer(), user);

        if (count < 650) {
            try {
                Server server = channel.getServer();
                PowerPlantStatus powerPlantStatus = DBServer.getPowerPlantStatusFromServer(server);
                ArrayList<Long> powerPlantIgnoredChannelIds = DBServer.getPowerPlantIgnoredChannelIdsFromServer(server);

                boolean whiteListed = DBServer.isChannelWhitelisted(channel);
                if (powerPlantStatus == PowerPlantStatus.ACTIVE && !powerPlantIgnoredChannelIds.contains(channel.getId())) {
                    ActivityUserData activityUserData = getActivities(server, user);
                    if (activityUserData.registerMessage(messagePhase, whiteListed ? channel : null)) {
                        setUserMessageCount(server, user, count + 1);
                    }
                    setActivities(server, user, activityUserData);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void messageCollector() {
        final int MINUTES_INTERVAL = 5;

        while(true) {
            try {
                Duration duration = Duration.between(Instant.now(), nextMessageCheck);
                Thread.sleep(Math.max(1, duration.getSeconds() * 1000 + duration.getNano() / 1000000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            nextMessageCheck = Instant.now().plusSeconds(20);

            messagePhase++;
            if (messagePhase >= 3 * MINUTES_INTERVAL) {
                messagePhase = 0;

                Map<Long, Map<Long, ActivityUserData>> finalActivites = activities;
                activities = new HashMap<>();

                synchronized (this) {
                    if (finalActivites.size() > 0) {
                        Thread t = new Thread(() -> {
                            System.out.println("Collector START");

                            for(long serverId: finalActivites.keySet()) {
                                for(long userId: finalActivites.get(serverId).keySet()) {
                                    try {
                                        DBUser.addMessageSingle(serverId, userId, finalActivites.get(serverId).get(userId));
                                        Thread.sleep(200);
                                    } catch (SQLException | InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            System.out.println("Collector END");
                        });

                        t.setName("message_collector_db");
                        t.start();
                    }
                }
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
            nextVCCheck = Instant.now().plusSeconds(60);

            for (Server server : api.getServers()) {
                try {
                    if (DBServer.getPowerPlantStatusFromServer(server) == PowerPlantStatus.ACTIVE) {
                        for (ServerVoiceChannel channel : server.getVoiceChannels()) {
                            ArrayList<User> validUsers = new ArrayList<>();
                            for (User user : channel.getConnectedUsers()) {
                                if (!user.isBot() &&
                                        !user.isMuted(server) &&
                                        !user.isDeafened(server) &&
                                        !user.isSelfDeafened(server) &&
                                        !user.isSelfMuted(server)
                                ) {
                                    validUsers.add(user);
                                }
                            }

                            if (validUsers.size() > 1 &&
                                    (!server.getAfkChannel().isPresent() || channel.getId() != server.getAfkChannel().get().getId())
                            ) {
                                for (User user : validUsers) {
                                    int count = getUserVCCount(server, user);
                                    if (count < 300) {
                                        setUserVCCount(server, user, count + 1);
                                        ActivityUserData activityUserData = getActivities(server, user);
                                        activityUserData.registerVC();
                                        setActivities(server, user, activityUserData);
                                    }
                                }
                            }
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void startVCCollector(DiscordApi api) {
        Thread t = new Thread(() -> VCCollector(api));
        t.setPriority(1);
        t.setName("vc_collector");
        t.start();
    }

    public void reset() {
        userMessageCount = new HashMap<>();
        userVCCount = new HashMap<>();
    }

    private ActivityUserData getActivities(Server server, User user) {
        Map<Long, ActivityUserData> serverMap = activities.computeIfAbsent(server.getId(), k -> new HashMap<>());
        return serverMap.computeIfAbsent(user.getId(), k -> new ActivityUserData());
    }

    private int getUserMessageCount(Server server, User user) {
        Map<Long, Integer> serverMap = userMessageCount.computeIfAbsent(server.getId(), k -> new HashMap<>());
        return serverMap.computeIfAbsent(user.getId(), k -> 0);
    }

    private int getUserVCCount(Server server, User user) {
        Map<Long, Integer> serverMap = userVCCount.computeIfAbsent(server.getId(), k -> new HashMap<>());
        return serverMap.computeIfAbsent(user.getId(), k -> 0);
    }

    private void setActivities(Server server, User user, ActivityUserData activityUserData) {
        Map<Long, ActivityUserData> serverMap = activities.computeIfAbsent(server.getId(), k -> new HashMap<>());
        serverMap.putIfAbsent(user.getId(), activityUserData);
    }

    private void setUserMessageCount(Server server, User user, int amount) {
        Map<Long, Integer> serverMap = userMessageCount.computeIfAbsent(server.getId(), k -> new HashMap<>());
        serverMap.put(user.getId(), amount);
    }

    private void setUserVCCount(Server server, User user, int amount) {
        Map<Long, Integer> serverMap = userVCCount.computeIfAbsent(server.getId(), k -> new HashMap<>());
        serverMap.put(user.getId(), amount);
    }

}
