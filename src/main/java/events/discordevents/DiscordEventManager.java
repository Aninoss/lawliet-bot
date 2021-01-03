package events.discordevents;

import core.GlobalCachedThreadPool;
import events.discordevents.eventtypeabstracts.*;
import org.javacord.api.DiscordApi;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

public class DiscordEventManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(DiscordEventManager.class);

    private final HashMap<Class<?>, ArrayList<DiscordEventAbstract>> listenerMap;

    public DiscordEventManager() {
        listenerMap = new HashMap<>();

        Reflections reflections = new Reflections("events/discordevents");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(DiscordEvent.class);
        Set<Class<? extends DiscordEventAbstract>> listenerTypeAbstracts = reflections.getSubTypesOf(DiscordEventAbstract.class);

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
                .filter(obj -> obj instanceof DiscordEventAbstract)
                .map(obj -> (DiscordEventAbstract) obj)
                .forEach(listener -> putListener(listener, listenerTypeAbstracts));
    }

    private void putListener(DiscordEventAbstract listener, Set<Class<? extends DiscordEventAbstract>> listenerTypeAbstracts) {
        for(Class<?> clazz : listenerTypeAbstracts) {
            if (clazz.isInstance(listener)) {
                ArrayList<DiscordEventAbstract> listenerList = listenerMap.computeIfAbsent(clazz, k -> new ArrayList<>());
                listenerList.add(listener);
            }
        }
    }

    public void registerApi(DiscordApi api) {
        api.addMessageCreateListener(event -> GlobalCachedThreadPool.getExecutorService().submit(() -> MessageCreateAbstract.onMessageCreateStatic(event, getListenerList(MessageCreateAbstract.class))));
        api.addMessageEditListener(event -> GlobalCachedThreadPool.getExecutorService().submit(() -> MessageEditAbstract.onMessageEditStatic(event, getListenerList(MessageEditAbstract.class))));
        api.addMessageDeleteListener(event -> GlobalCachedThreadPool.getExecutorService().submit(() -> MessageDeleteAbstract.onMessageDeleteStatic(event, getListenerList(MessageDeleteAbstract.class))));

        api.addReactionAddListener(event -> GlobalCachedThreadPool.getExecutorService().submit(() -> ReactionAddAbstract.onReactionAddStatic(event, getListenerList(ReactionAddAbstract.class))));
        api.addReactionRemoveListener(event -> GlobalCachedThreadPool.getExecutorService().submit(() -> ReactionRemoveAbstract.onReactionRemoveStatic(event, getListenerList(ReactionRemoveAbstract.class))));

        api.addServerChannelDeleteListener(event -> GlobalCachedThreadPool.getExecutorService().submit(() -> ServerChannelDeleteAbstract.onServerChannelDeleteStatic(event, getListenerList(ServerChannelDeleteAbstract.class))));

        api.addServerJoinListener(event -> GlobalCachedThreadPool.getExecutorService().submit(() -> ServerJoinAbstract.onServerJoinStatic(event, getListenerList(ServerJoinAbstract.class))));
        api.addServerLeaveListener(event -> GlobalCachedThreadPool.getExecutorService().submit(() -> ServerLeaveAbstract.onServerLeaveStatic(event, getListenerList(ServerLeaveAbstract.class))));

        api.addServerMemberJoinListener(event -> GlobalCachedThreadPool.getExecutorService().submit(() -> ServerMemberJoinAbstract.onServerMemberJoinStatic(event, getListenerList(ServerMemberJoinAbstract.class))));
        api.addServerMemberLeaveListener(event -> GlobalCachedThreadPool.getExecutorService().submit(() -> ServerMemberLeaveAbstract.onServerMemberLeaveStatic(event, getListenerList(ServerMemberLeaveAbstract.class))));

        api.addServerVoiceChannelChangeUserLimitListener(event -> GlobalCachedThreadPool.getExecutorService().submit(() -> ServerVoiceChannelChangeUserLimitAbstract.onServerVoiceChannelChangeUserLimitStatic(event, getListenerList(ServerVoiceChannelChangeUserLimitAbstract.class))));
        api.addServerVoiceChannelMemberJoinListener(event -> GlobalCachedThreadPool.getExecutorService().submit(() -> ServerVoiceChannelMemberJoinAbstract.onServerVoiceChannelMemberJoinStatic(event, getListenerList(ServerVoiceChannelMemberJoinAbstract.class))));
        api.addServerVoiceChannelMemberLeaveListener(event -> GlobalCachedThreadPool.getExecutorService().submit(() -> ServerVoiceChannelMemberLeaveAbstract.onServerVoiceChannelMemberLeaveStatic(event, getListenerList(ServerVoiceChannelMemberLeaveAbstract.class))));

        api.addUserRoleAddListener(event -> GlobalCachedThreadPool.getExecutorService().submit(() -> UserRoleAddAbstract.onUserRoleAddStatic(event, getListenerList(UserRoleAddAbstract.class))));
        api.addUserRoleRemoveListener(event -> GlobalCachedThreadPool.getExecutorService().submit(() -> UserRoleRemoveAbstract.onUserRoleRemoveStatic(event, getListenerList(UserRoleRemoveAbstract.class))));

        api.addServerChangeBoostCountListener(event -> GlobalCachedThreadPool.getExecutorService().submit(() -> ServerChangeBoostCountAbstract.onServerChangeBoostCountStatic(event, getListenerList(ServerChangeBoostCountAbstract.class))));

        api.addUserChangeActivityListener(event -> GlobalCachedThreadPool.getExecutorService().submit(() -> UserChangeActivityAbstract.onUserChangeActivityStatic(event, getListenerList(UserChangeActivityAbstract.class))));
    }

    private ArrayList<DiscordEventAbstract> getListenerList(Class<? extends DiscordEventAbstract> clazz) {
        return listenerMap.computeIfAbsent(clazz, k -> new ArrayList<>());
    }

}
