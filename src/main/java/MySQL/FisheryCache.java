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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FisheryCache {

    private static FisheryCache ourInstance = new FisheryCache();

    private Map<Long, Map<Long, ActivityUserData>> activities = new HashMap<>(); //serverId, userId, channel
    private int messagePhase = 0;

    private Map<Long, Map<Long, Integer>> userMessageCount = new HashMap<>(); //serverId, userId, counter
    private Map<Long, Map<Long, Integer>> userVCCount = new HashMap<>(); //serverId, userId, counter

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
        int count = getUserMessageCount(channel.getServer(), user);

        if (count < 300) {
            try {
                Server server = channel.getServer();
                PowerPlantStatus powerPlantStatus = DBServer.getPowerPlantStatusFromServer(server);
                ArrayList<Long> powerPlantIgnoredChannelIds = DBServer.getPowerPlantIgnoredChannelIdsFromServer(server);

                if (powerPlantStatus == PowerPlantStatus.ACTIVE && !powerPlantIgnoredChannelIds.contains(channel.getId())) {
                    ActivityUserData activityUserData = getActivities(server, user);
                    if (activityUserData == null) activityUserData = new ActivityUserData(channel);
                    if (activityUserData.register(messagePhase))
                        setUserMessageCount(server, user, count + 1);
                    setActivities(server, user, activityUserData);
                }
            } catch (SQLException e) {
                e.printStackTrace();
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
            if (messagePhase >= 3 * 15) {
                messagePhase = 0;

                Map<Long, Map<Long, ActivityUserData>> activitesClone = null;

                System.out.println("Message Collector START");

                boolean finished = false;
                while (!finished) {
                    try {
                        synchronized (activities) {
                            activitesClone = new HashMap<>(activities);
                            activities.clear();
                        }
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

                if (activitesClone.size() > 0) {
                    try {
                        DBUser.addMessageFishBulk(activitesClone);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }

                System.out.println("Message Collector END");
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

            ArrayList<Pair<Long, Long>> userVCActivities = new ArrayList<>(); //serverId, userId

            Instant testStart = Instant.now();
            System.out.println("VC Collector START");

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

                    if (validUsers.size() > 0 && //TODO
                            (!server.getAfkChannel().isPresent() || channel.getId() != server.getAfkChannel().get().getId())
                    ) {
                        for (User user : validUsers) {
                            int count = getUserVCCount(server, user);
                            count++;

                            setUserMessageCount(server, user, count);
                            userVCActivities.add(new Pair<>(server.getId(), user.getId()));
                        }
                    }
                }
            }

            if (userVCActivities.size() > 0) {
                try {
                    DBUser.addVCFishBulk(userVCActivities, 5);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            System.out.println("VC Collector END");
            Duration duration = Duration.between(testStart, Instant.now());
            System.out.println("####################### " + (duration.getNano() / 1000000000.0) + " s");
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
        synchronized (activities) {
            Map<Long, ActivityUserData> serverMap = activities.computeIfAbsent(server.getId(), k -> new HashMap<>());
            return serverMap.get(user.getId());
        }
    }

    private int getUserMessageCount(Server server, User user) {
        synchronized (userMessageCount) {
            Map<Long, Integer> serverMap = userMessageCount.computeIfAbsent(server.getId(), k -> new HashMap<>());
            return serverMap.computeIfAbsent(user.getId(), k -> 0);
        }
    }

    private int getUserVCCount(Server server, User user) {
        synchronized (userVCCount) {
            Map<Long, Integer> serverMap = userVCCount.computeIfAbsent(server.getId(), k -> new HashMap<>());
            return serverMap.computeIfAbsent(user.getId(), k -> 0);
        }
    }

    private void setActivities(Server server, User user, ActivityUserData activityUserData) {
        synchronized (activities) {
            Map<Long, ActivityUserData> serverMap = activities.computeIfAbsent(server.getId(), k -> new HashMap<>());
            serverMap.put(user.getId(), activityUserData);
        }
    }

    private void setUserMessageCount(Server server, User user, int amount) {
        synchronized (userMessageCount) {
            Map<Long, Integer> serverMap = userMessageCount.computeIfAbsent(server.getId(), k -> new HashMap<>());
            serverMap.put(user.getId(), amount);
        }
    }

    private void setUserVCCount(Server server, User user, int amount) {
        synchronized (userVCCount) {
            Map<Long, Integer> serverMap = userVCCount.computeIfAbsent(server.getId(), k -> new HashMap<>());
            serverMap.put(user.getId(), amount);
        }
    }

}
