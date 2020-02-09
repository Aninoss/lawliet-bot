package MySQL;

import Constants.FishingCategoryInterface;
import Constants.PowerPlantStatus;
import General.Fishing.FishingProfile;
import General.Pair;
import General.Tools;
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

    private static HashMap<Integer, FisheryCache> ourInstances = new HashMap<>();

    private Map<Long, Map<Long, ActivityUserData>> activities = new HashMap<>(); //serverId, userId, activity
    private int messagePhase = 0;

    private Map<Long, Map<Long, Integer>> userMessageCount = new HashMap<>(); //serverId, userId, counter
    private Map<Long, Map<Long, Integer>> userVCCount = new HashMap<>(); //serverId, userId, counter

    private Instant nextMessageCheck = Instant.now(), nextVCCheck = Instant.now();

    private boolean active = true;
    private int shardId;

    public static FisheryCache getInstance(int shardId) {
        return ourInstances.computeIfAbsent(shardId, k -> new FisheryCache(shardId));
    }

    private FisheryCache(int shardId) {
        this.shardId = shardId;
        Thread t = new Thread(this::messageCollector);
        t.setName("message_collector");
        t.start();
    }

    public boolean addActivity(User user, ServerTextChannel channel) {
        int count = getUserMessageCount(channel.getServer(), user);

        if (count < 650) {
            try {
                Server server = channel.getServer();
                PowerPlantStatus powerPlantStatus = DBServer.getPowerPlantStatusFromServer(server);
                ArrayList<Long> powerPlantIgnoredChannelIds = DBServer.getPowerPlantIgnoredChannelIdsFromServer(server);

                boolean whiteListed = DBServer.isChannelWhitelisted(channel);
                if (powerPlantStatus == PowerPlantStatus.ACTIVE && !powerPlantIgnoredChannelIds.contains(channel.getId())) {
                    ActivityUserData activityUserData = getActivities(server, user);
                    boolean registered = activityUserData.registerMessage(messagePhase, whiteListed ? channel : null);
                    if (registered) {
                        setUserMessageCount(server, user, count + 1);
                    }
                    setActivities(server, user, activityUserData);
                    return registered;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    private void messageCollector() {
        final int MINUTES_INTERVAL = 20;

        while(active) {
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
                                        synchronized (FisheryCache.class) {
                                            ActivityUserData activityUserData = finalActivites.get(serverId).get(userId);
                                            if (activityUserData.getAmountVC() + activityUserData.getAmountMessage() > 0) {
                                                DBUser.addMessageSingle(serverId, userId, activityUserData);
                                                Thread.sleep(500);
                                            }
                                        }
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

    public long flush(Server server, User user, boolean clear) throws SQLException {
        return flush(server, user, clear, null);
    }

    public long flush(Server server, User user, boolean clear, FishingProfile fishingProfile) throws SQLException {
        if (activities.containsKey(server.getId()) && activities.get(server.getId()).containsKey(user.getId())) {
            ActivityUserData activityUserData = activities.get(server.getId()).get(user.getId());
            if (activityUserData.getAmountVC() + activityUserData.getAmountMessage() > 0) {
                if (fishingProfile == null) fishingProfile = DBUser.getFishingProfile(server, user, false);

                long fishMessage = activityUserData.getAmountMessage() * fishingProfile.getEffect(FishingCategoryInterface.PER_MESSAGE);
                long fishVC = activityUserData.getAmountVC() * fishingProfile.getEffect(FishingCategoryInterface.PER_VC);

                if (clear) activityUserData.reset();
                return fishMessage + fishVC;
            }
        }

        return 0L;
    }

    private void VCCollector(DiscordApi api) {
        while(active) {
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
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void startVCCollector(DiscordApi api) {
        Thread t = new Thread(() -> VCCollector(api));
        t.setPriority(1);
        t.setName("vc_collector_" + api.getCurrentShard());
        t.start();
    }

    private ActivityUserData getActivities(Server server, User user) {
        return getActivities(server.getId(), user.getId());
    }

    public ActivityUserData getActivities(long serverId, long userId) {
        Map<Long, ActivityUserData> serverMap = activities.computeIfAbsent(serverId, k -> new HashMap<>());
        return serverMap.computeIfAbsent(userId, k -> new ActivityUserData());
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

    public void turnOff() {
        ourInstances.remove(shardId);
        active = false;
    }

}
