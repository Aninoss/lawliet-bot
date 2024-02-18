package events.discordevents;

import core.MainLogger;
import core.ShardManager;
import core.cache.UserBannedCache;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import net.dv8tion.jda.api.entities.User;

import java.util.ArrayList;

public abstract class DiscordEventAbstract {

    private final DiscordEvent discordEvent;

    public DiscordEventAbstract() {
        discordEvent = this.getClass().getAnnotation(DiscordEvent.class);
    }

    public EventPriority getPriority() {
        return discordEvent.priority();
    }

    public boolean isAllowingBannedUser() {
        return discordEvent.allowBannedUser();
    }

    public boolean isAllowingBots() {
        return discordEvent.allowBots();
    }

    protected static void execute(ArrayList<DiscordEventAbstract> listenerList, long guildId, EventExecution function) {
        execute(listenerList, null, guildId, function);
    }

    protected static void execute(ArrayList<DiscordEventAbstract> listenerList, User user, EventExecution function) {
        execute(listenerList, user, 0L, function);
    }

    protected static void execute(ArrayList<DiscordEventAbstract> listenerList, User user, long guildId, EventExecution function) {
        if ((user != null && user.getIdLong() == ShardManager.getSelfId()) ||
                !ShardManager.getJDABlocker().guildIsAvailable(guildId) ||
                (guildId != 0 && ShardManager.getLocalGuildById(guildId).isEmpty()) ||
                listenerList.isEmpty()
        ) {
            return;
        }

        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(DiscordEventAbstract.class)) {
            boolean banned = user != null && UserBannedCache.getInstance().isBanned(user.getIdLong());
            boolean bot = user != null && user.isBot();
            for (EventPriority priority : EventPriority.values()) {
                if (!runListenerPriority(listenerList, function, priority, entityManager, banned, bot)) {
                    return;
                }
            }
        }
    }

    private static boolean runListenerPriority(ArrayList<DiscordEventAbstract> listenerList, EventExecution function,
                                               EventPriority priority, EntityManagerWrapper entityManager,
                                               boolean banned, boolean bot
    ) {
        for (DiscordEventAbstract listener : listenerList) {
            if (listener.getPriority() == priority &&
                    (!banned || listener.isAllowingBannedUser()) &&
                    (!bot || listener.isAllowingBots()) &&
                    !run(function, listener, entityManager)
            ) {
                return false;
            }
        }

        return true;
    }

    private static boolean run(EventExecution function, DiscordEventAbstract listener, EntityManagerWrapper entityManager) {
        entityManager.setCallingClass(listener.getClass());
        try {
            return function.apply(listener, entityManager);
        } catch (Throwable throwable) {
            MainLogger.get().error("Uncaught Exception", throwable);
        }
        return true;
    }


    public interface EventExecution {

        boolean apply(DiscordEventAbstract discordEventAbstract, EntityManagerWrapper entityManager) throws Throwable;

    }

}
