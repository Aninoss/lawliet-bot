package events.discordevents;

import mysql.modules.bannedusers.DBBannedUsers;
import org.javacord.api.entity.user.User;
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

    protected static void execute(ArrayList<DiscordEventAbstract> listenerList, boolean multiThreadded, EventExecution function) {
        execute(listenerList, null, multiThreadded, function);
    }

    protected static void execute(ArrayList<DiscordEventAbstract> listenerList, User user, boolean multiThreadded, EventExecution function) {
        if (user != null && user.isYourself()) {
            return;
        }

        boolean banned = user != null && userIsBanned(user.getId());
        boolean bot = user != null && user.isBot();
        for (EventPriority priority : EventPriority.values())
            if (!runListenerPriority(listenerList, function, priority, banned, bot, multiThreadded))
                return;
    }

    private static boolean runListenerPriority(ArrayList<DiscordEventAbstract> listenerList, EventExecution function,
                                               EventPriority priority, boolean banned, boolean bot, boolean multiThreadded) {
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
            LOGGER.error("Uncaught Exception", throwable);
        }
        return true;
    }

    public interface EventExecution {

        boolean apply(DiscordEventAbstract discordEventAbstract) throws Throwable;

    }


}
