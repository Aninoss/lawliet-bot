package events.discordevents;

import core.DiscordApiManager;
import mysql.modules.bannedusers.DBBannedUsers;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;

public abstract class DiscordEventAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(DiscordEventAbstract.class);

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

    protected static void execute(ArrayList<DiscordEventAbstract> listenerList, long serverId, EventExecution function) {
        execute(listenerList, null, serverId, function);
    }

    protected static void execute(ArrayList<DiscordEventAbstract> listenerList, User user, EventExecution function) {
        execute(listenerList, user, 0L, function);
    }

    protected static void execute(ArrayList<DiscordEventAbstract> listenerList, User user, long serverId, EventExecution function) {
        if ((user != null && DiscordApiManager.getInstance().getSelfId() == user.getIdLong()) || !DiscordApiManager.getInstance().getDiscordApiBlocker().serverIsAvailable(serverId)) {
            return;
        }

        boolean banned = user != null && userIsBanned(user.getIdLong());
        boolean bot = user != null && user.isBot();
        for (EventPriority priority : EventPriority.values())
            if (!runListenerPriority(listenerList, function, priority, banned, bot))
                return;
    }

    private static boolean runListenerPriority(ArrayList<DiscordEventAbstract> listenerList, EventExecution function,
                                               EventPriority priority, boolean banned, boolean bot) {
        for (DiscordEventAbstract listener : listenerList) {
            if (listener.getPriority() == priority && (!banned || listener.isAllowingBannedUser()) &&
                    (!bot || listener.isAllowingBots()) && !run(function, listener)
            ) {
                return false;
            }
        }

        return true;
    }

    private static boolean userIsBanned(long userId) {
        return DBBannedUsers.getInstance().getBean().getUserIds().contains(userId);
    }

    private static boolean run(EventExecution function, DiscordEventAbstract listener) {
        try {
            return function.apply(listener);
        } catch (Throwable throwable) {
            MainLogger.get().error("Uncaught Exception", throwable);
        }
        return true;
    }

    public interface EventExecution {

        boolean apply(DiscordEventAbstract discordEventAbstract) throws Throwable;

    }


}
