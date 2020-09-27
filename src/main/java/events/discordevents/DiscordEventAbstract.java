package events.discordevents;

import core.CustomThread;
import core.DiscordApiCollection;
import mysql.modules.bannedusers.DBBannedUsers;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        if (!DiscordApiCollection.getInstance().isStarted() ||
                (user != null && user.isYourself())
        ) {
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
        if (multiThreadded && false) {
            List<DiscordEventAbstract> list = listenerList.stream()
                    .filter(listener -> listener.getPriority() == priority && (!banned || listener.isAllowingBannedUser()) && (!bot || listener.isAllowingBots()))
                    .collect(Collectors.toList());

            final CustomThread[] threads = new CustomThread[list.size()];
            final boolean[] cont = { true };

            for (int i = 0; i < list.size(); i++) {
                DiscordEventAbstract listener = list.get(i);
                threads[i] = new CustomThread(() -> {
                    if (!run(function, listener))
                        cont[0] = false;
                }, Thread.currentThread().getName());
                threads[i].start();
            }

            for (CustomThread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted", e);
                }
            }

            return cont[0];
        } else {
            for (DiscordEventAbstract listener : listenerList) {
                if (listener.getPriority() == priority && (!banned || listener.isAllowingBannedUser()) &&
                        (!bot || listener.isAllowingBots()) && !run(function, listener)
                ) {
                    return false;
                }
            }

            return true;
        }
    }

    private static boolean userIsBanned(long userId) {
        try {
            return DBBannedUsers.getInstance().getBean().getUserIds().contains(userId);
        } catch (SQLException throwables) {
            LOGGER.error("SQL error", throwables);
            return true;
        }
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
