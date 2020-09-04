package Events.DiscordEvents;

import Core.CustomThread;
import Events.DiscordEvents.EventTypeAbstracts.*;
import MySQL.Modules.BannedUsers.DBBannedUsers;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class DiscordEventManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(DiscordEventManager.class);

    private final HashMap<Class<?>, ArrayList<DiscordEventAbstract>> listenerMap;

    public DiscordEventManager() {
        listenerMap = new HashMap<>();

        Reflections reflections = new Reflections("Events/DiscordEvents");
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
        api.addMessageCreateListener(event -> new CustomThread(() -> MessageCreateAbstract.onMessageCreateStatic(event, getListenerList(MessageCreateAbstract.class, event.getMessageAuthor().getId())), "message_create").start());
        api.addMessageEditListener(event -> new CustomThread(() -> MessageEditAbstract.onMessageEditStatic(event, getListenerList(MessageEditAbstract.class, event.getMessageAuthor())), "message_edit").start());
        api.addMessageDeleteListener(event -> new CustomThread(() -> MessageDeleteAbstract.onMessageDeleteStatic(event, getListenerList(MessageDeleteAbstract.class, event.getMessageAuthor())), "message_delete").start());

        api.addReactionAddListener(event -> new CustomThread(() -> ReactionAddAbstract.onReactionAddStatic(event, getListenerList(ReactionAddAbstract.class, event.getMessageAuthor())), "reaction_add").start());
        api.addReactionRemoveListener(event -> new CustomThread(() -> ReactionRemoveAbstract.onReactionRemoveStatic(event, getListenerList(ReactionRemoveAbstract.class, event.getMessageAuthor())), "reaction_remove").start());

        api.addServerChannelDeleteListener(event -> new CustomThread(() -> ServerChannelDeleteAbstract.onServerChannelDeleteStatic(event, getListenerList(ServerChannelDeleteAbstract.class)), "server_channel_delete").start());

        api.addServerJoinListener(event -> new CustomThread(() -> ServerJoinAbstract.onServerJoinStatic(event, getListenerList(ServerJoinAbstract.class)), "server_join").start());
        api.addServerLeaveListener(event -> new CustomThread(() -> ServerLeaveAbstract.onServerLeaveStatic(event, getListenerList(ServerLeaveAbstract.class)), "server_leave").start());

        api.addServerMemberJoinListener(event -> new CustomThread(() -> ServerMemberJoinAbstract.onServerMemberJoinStatic(event, getListenerList(ServerMemberJoinAbstract.class, event.getUser().getId())), "server_member_join").start());
        api.addServerMemberLeaveListener(event -> new CustomThread(() -> ServerMemberLeaveAbstract.onServerMemberLeaveStatic(event, getListenerList(ServerMemberLeaveAbstract.class, event.getUser().getId())), "server_member_leave").start());

        api.addServerVoiceChannelChangeUserLimitListener(event -> new CustomThread(() -> ServerVoiceChannelChangeUserLimitAbstract.onServerVoiceChannelChangeUserLimitStatic(event, getListenerList(ServerVoiceChannelChangeUserLimitAbstract.class)), "server_voice_channel_change_user_limit").start());
        api.addServerVoiceChannelMemberJoinListener(event -> new CustomThread(() -> ServerVoiceChannelMemberJoinAbstract.onServerVoiceChannelMemberJoinStatic(event, getListenerList(ServerVoiceChannelMemberJoinAbstract.class, event.getUser().getId())), "server_voice_channel_member_join").start());
        api.addServerVoiceChannelMemberLeaveListener(event -> new CustomThread(() -> ServerVoiceChannelMemberLeaveAbstract.onServerVoiceChannelMemberLeaveStatic(event, getListenerList(ServerVoiceChannelMemberLeaveAbstract.class, event.getUser().getId())), "server_voice_channel_member_leave").start());

        api.addUserRoleAddListener(event -> new CustomThread(() -> UserRoleAddAbstract.onUserRoleAddStatic(event, getListenerList(UserRoleAddAbstract.class, event.getUser().getId())), "user_role_add").start());
        api.addUserRoleRemoveListener(event -> new CustomThread(() -> UserRoleRemoveAbstract.onUserRoleRemoveStatic(event, getListenerList(UserRoleRemoveAbstract.class, event.getUser().getId())), "user_role_remove").start());

        api.addServerChangeBoostCountListener(event -> new CustomThread(() -> ServerChangeBoostCountAbstract.onServerChangeBoostCountStatic(event, getListenerList(ServerChangeBoostCountAbstract.class)), "server_change_boost_count").start());
    }

    private ArrayList<DiscordEventAbstract> getListenerList(Class<? extends DiscordEventAbstract> clazz, Optional<? extends DiscordEntity> entityOpt) {
        if (!entityOpt.isPresent()) return getListenerList(clazz);
        return getListenerList(clazz, entityOpt.get().getId());
    }

    private ArrayList<DiscordEventAbstract> getListenerList(Class<? extends DiscordEventAbstract> clazz) {
        return new ArrayList<>(listenerMap.computeIfAbsent(clazz, k -> new ArrayList<>()));
    }

    private ArrayList<DiscordEventAbstract> getListenerList(Class<? extends DiscordEventAbstract> clazz, long userId) {
        boolean banned = userIsBanned(userId);
        return listenerMap.computeIfAbsent(clazz, k -> new ArrayList<>())
                .stream()
                .filter(listener -> !banned || listener.isAllowingBannedUser())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean userIsBanned(long userId) {
        try {
            return DBBannedUsers.getInstance().getBean().getUserIds().contains(userId);
        } catch (SQLException throwables) {
            LOGGER.error("SQL error", throwables);
            return true;
        }
    }

}
