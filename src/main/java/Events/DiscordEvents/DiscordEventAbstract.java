package Events.DiscordEvents;

import Core.CustomThread;
import Core.DiscordApiCollection;
import org.javacord.api.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class DiscordEventAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(DiscordEventAbstract.class);

    private final DiscordEvent discordEvent;

    public DiscordEventAbstract() {
        discordEvent = this.getClass().getAnnotation(DiscordEvent.class);
    }

    public EventPriority getPriority() { return discordEvent.priority(); }

    public boolean isAllowingBannedUser() { return discordEvent.allowBannedUser(); }

    protected static <T extends Event> void execute(T event, ArrayList<DiscordEventAbstract> listenerList, EventExecution function) {
        if (!DiscordApiCollection.getInstance().allShardsConnected())
            return;

        for(EventPriority priority : EventPriority.values())
            if (!runListenerPriority(event, listenerList, function, priority))
                return;
    }

    private static <T extends Event> boolean runListenerPriority(T event, ArrayList<DiscordEventAbstract> listenerList, EventExecution function, EventPriority priority) {
        List<DiscordEventAbstract> list = listenerList.stream()
                .filter(listener -> listener.getPriority() == priority)
                .collect(Collectors.toList());  /* filter list for priority */

        final CustomThread[] threads = new CustomThread[list.size()];
        final boolean[] cont = { true };

        for(int i = 0; i < list.size(); i++) {  /* initialize all threads */
            DiscordEventAbstract listener = list.get(i);
            threads[i] = new CustomThread(() -> {
                if (!run(function, listener))
                    cont[0] = false;
            }, Thread.currentThread().getName());
            threads[i].start();
        }

        Arrays.stream(threads).forEach(t -> {   /* wait until all threads have been completed */
            try {
                t.join();
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted", e);
            }
        });

        return cont[0];
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
