package DiscordListener;

import Constants.Settings;
import Core.CustomThread;
import Core.EmbedFactory;
import DiscordListener.ListenerTypeAbstracts.*;
import org.javacord.api.DiscordApi;
import org.javacord.api.event.channel.server.ServerChannelDeleteEvent;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.MessageDeleteEvent;
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
        Set<Class<? extends DiscordListenerAbstract>> listenerTypeAbstracts = reflections.getSubTypesOf(DiscordListenerAbstract.class);

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
                .sorted((o1, o2) -> Integer.compare(o2.getPriority().ordinal(), o1.getPriority().ordinal()))
                .forEach(listener -> putListener(listener, listenerTypeAbstracts));
    }

    private void putListener(DiscordListenerAbstract listener, Set<Class<? extends DiscordListenerAbstract>> listenerTypeAbstracts) {
        for(Class<?> clazz : listenerTypeAbstracts) {
            if (clazz.isInstance(listener)) {
                ArrayList<DiscordListenerAbstract> listenerList = listenerMap.computeIfAbsent(clazz, k -> new ArrayList<>());
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
    }

    private ArrayList<DiscordListenerAbstract> getListenerList(Class<? extends DiscordListenerAbstract> clazz) {
        return listenerMap.computeIfAbsent(clazz, k -> new ArrayList<>());
    }

}
