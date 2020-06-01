package DiscordEvents;

import Core.CustomThread;
import DiscordEvents.EventTypeAbstracts.*;
import org.javacord.api.DiscordApi;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;

public class DiscordEventManager {

    private static final DiscordEventManager ourInstance = new DiscordEventManager();
    public static DiscordEventManager getInstance() { return ourInstance; }

    final static Logger LOGGER = LoggerFactory.getLogger(DiscordEventManager.class);

    private final HashMap<Class<?>, ArrayList<DiscordEventAbstract>> listenerMap;

    private DiscordEventManager() {
        listenerMap = new HashMap<>();

        Reflections reflections = new Reflections("DiscordEvents");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(DiscordEventAnnotation.class);
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
                .sorted((o1, o2) -> Integer.compare(o2.getPriority().ordinal(), o1.getPriority().ordinal()))
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

    public void addApi(DiscordApi api) {
        api.addMessageCreateListener(event -> new CustomThread(() -> MessageCreateAbstract.onMessageCreateStatic(event, getListenerList(MessageCreateAbstract.class)), "message_create").start());
        api.addMessageEditListener(event -> new CustomThread(() -> MessageEditAbstract.onMessageEditStatic(event, getListenerList(MessageEditAbstract.class)), "message_edit").start());
        api.addMessageDeleteListener(event -> new CustomThread(() -> MessageDeleteAbstract.onMessageDeleteStatic(event, getListenerList(MessageDeleteAbstract.class)), "message_delete").start());

        api.addReactionAddListener(event -> new CustomThread(() -> ReactionAddAbstract.onReactionAddStatic(event, getListenerList(ReactionAddAbstract.class)), "reaction_add").start());
        api.addReactionRemoveListener(event -> new CustomThread(() -> ReactionRemoveAbstract.onReactionRemoveStatic(event, getListenerList(ReactionRemoveAbstract.class)), "reaction_remove").start());

        api.addServerChannelDeleteListener(event -> new CustomThread(() -> ServerChannelDeleteAbstract.onServerChannelDeleteStatic(event, getListenerList(ServerChannelDeleteAbstract.class)), "server_channel_delete").start());

        api.addServerJoinListener(event -> new CustomThread(() -> ServerJoinAbstract.onServerJoinStatic(event, getListenerList(ServerJoinAbstract.class)), "server_join").start());
        api.addServerLeaveListener(event -> new CustomThread(() -> ServerLeaveAbstract.onServerLeaveStatic(event, getListenerList(ServerLeaveAbstract.class)), "server_leave").start());

        api.addServerMemberJoinListener(event -> new CustomThread(() -> ServerMemberJoinAbstract.onServerMemberJoinStatic(event, getListenerList(ServerMemberJoinAbstract.class)), "server_member_join").start());
        api.addServerMemberLeaveListener(event -> new CustomThread(() -> ServerMemberLeaveAbstract.onServerMemberLeaveStatic(event, getListenerList(ServerMemberLeaveAbstract.class)), "server_member_leave").start());

        api.addServerVoiceChannelChangeUserLimitListener(event -> new CustomThread(() -> ServerVoiceChannelChangeUserLimitAbstract.onServerVoiceChannelChangeUserLimitStatic(event, getListenerList(ServerVoiceChannelChangeUserLimitAbstract.class)), "server_voice_channel_change_user_limit").start());
        api.addServerVoiceChannelMemberJoinListener(event -> new CustomThread(() -> ServerVoiceChannelMemberJoinAbstract.onServerVoiceChannelMemberJoinStatic(event, getListenerList(ServerVoiceChannelMemberJoinAbstract.class)), "server_voice_channel_member_join").start());
        api.addServerVoiceChannelMemberLeaveListener(event -> new CustomThread(() -> ServerVoiceChannelMemberLeaveAbstract.onServerVoiceChannelMemberLeaveStatic(event, getListenerList(ServerVoiceChannelMemberLeaveAbstract.class)), "server_voice_channel_member_leave").start());

        api.addUserRoleAddListener(event -> new CustomThread(() -> UserRoleAddAbstract.onUserRoleAddStatic(event, getListenerList(UserRoleAddAbstract.class)), "user_role_add").start());
        api.addUserRoleRemoveListener(event -> new CustomThread(() -> UserRoleRemoveAbstract.onUserRoleRemoveStatic(event, getListenerList(UserRoleRemoveAbstract.class)), "user_role_remove").start());

        api.addServerChangeBoostCountListener(event -> new CustomThread(() -> ServerChangeBoostCountAbstract.onServerChangeBoostCountStatic(event, getListenerList(ServerChangeBoostCountAbstract.class)), "server_change_boost_count").start());
    }

    private ArrayList<DiscordEventAbstract> getListenerList(Class<? extends DiscordEventAbstract> clazz) {
        return listenerMap.computeIfAbsent(clazz, k -> new ArrayList<>());
    }

}
