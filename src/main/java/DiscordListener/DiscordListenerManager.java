package DiscordListener;

import Constants.Settings;
import Core.CustomThread;
import Core.EmbedFactory;
import DiscordListener.ListenerTypeAbstracts.MessageCreateAbstract;
import DiscordListener.ListenerTypeAbstracts.MessageEditAbstract;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.MessageEditEvent;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DiscordListenerManager {

    private static final DiscordListenerManager ourInstance = new DiscordListenerManager();
    public static DiscordListenerManager getInstance() { return ourInstance; }

    final static Logger LOGGER = LoggerFactory.getLogger(DiscordListenerManager.class);

    private final HashMap<Class<?>, ArrayList<DiscordListenerAbstract>> listenerMap;

    private DiscordListenerManager() {
        listenerMap = new HashMap<>();

        Reflections reflections = new Reflections("DiscordListener");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(DiscordListenerAnnotation.class);

        annotated.stream()
                .map(clazz -> {
                    try {
                        return clazz.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        LOGGER.error("Error when creating listener class", e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .filter(obj -> obj instanceof DiscordListenerAbstract)
                .map(obj -> (DiscordListenerAbstract) obj)
                .sorted((o1, o2) -> Boolean.compare(o2.hasHighPriority(), o1.hasHighPriority()))
                .forEach(this::putListener);
    }

    private void putListener(DiscordListenerAbstract listener) {
        /* message create */
        if (listener instanceof MessageCreateAbstract) {
            ArrayList<DiscordListenerAbstract> listenerList = listenerMap.computeIfAbsent(MessageCreateAbstract.class, k -> new ArrayList<>());
            listenerList.add(listener);
        }

        /* message edit */
        if (listener instanceof MessageEditAbstract) {
            ArrayList<DiscordListenerAbstract> listenerList = listenerMap.computeIfAbsent(MessageEditAbstract.class, k -> new ArrayList<>());
            listenerList.add(listener);
        }
    }

    public void addApi(DiscordApi api) {
        api.addMessageCreateListener(event -> new CustomThread(() -> onMessageCreate(event), "message_create").start());
        api.addMessageEditListener(event -> new CustomThread(() -> onMessageEdit(event), "message_edit").start());
    }

    private void onMessageCreate(MessageCreateEvent event) {
        if (!event.getMessage().getUserAuthor().isPresent() ||
                event.getMessage().getAuthor().isYourself() ||
                event.getMessage().getUserAuthor().get().isBot()
        ) return;

        if (!event.getServer().isPresent()) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError()
                    .setTitle("❌ Not Supported!".toUpperCase())
                    .setDescription(String.format("Commands via dm aren't supported, you need to [\uD83D\uDD17 invite](%s) Lawliet into a server!", Settings.BOT_INVITE_URL)));
            return;
        }

        ArrayList<DiscordListenerAbstract> listenerList = listenerMap.computeIfAbsent(MessageCreateAbstract.class, k -> new ArrayList<>());

        for(DiscordListenerAbstract listener : listenerList) {
            if (listener instanceof MessageCreateAbstract) {
                MessageCreateAbstract messageCreateAbstract = (MessageCreateAbstract) listener;

                try {
                    boolean cont = messageCreateAbstract.onMessageCreate(event);
                    if (!cont) return;
                } catch (InterruptedException interrupted) {
                    LOGGER.error("Interrupted", interrupted);
                    return;
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }
    }

    private void onMessageEdit(MessageEditEvent event) {
        if (!event.getMessage().isPresent() ||
                !event.getMessage().get().getUserAuthor().isPresent() ||
                event.getMessage().get().getAuthor().isYourself() ||
                !event.getServer().isPresent() ||
                event.getMessage().get().getUserAuthor().get().isBot()
        ) return;

        ArrayList<DiscordListenerAbstract> listenerList = listenerMap.computeIfAbsent(MessageEditAbstract.class, k -> new ArrayList<>());

        for(DiscordListenerAbstract listener : listenerList) {
            if (listener instanceof MessageEditAbstract) {
                MessageEditAbstract messageEditAbstract = (MessageEditAbstract) listener;

                try {
                    boolean cont = messageEditAbstract.onMessageEdit(event);
                    if (!cont) return;
                } catch (InterruptedException interrupted) {
                    LOGGER.error("Interrupted", interrupted);
                    return;
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }
    }

}
