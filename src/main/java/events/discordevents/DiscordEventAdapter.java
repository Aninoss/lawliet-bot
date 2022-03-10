package events.discordevents;

import java.util.*;
import core.*;
import events.discordevents.eventtypeabstracts.*;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateUserLimitEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdatePendingEvent;
import net.dv8tion.jda.api.events.guild.override.GenericPermissionOverrideEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateBoostCountEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.http.HttpRequestEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.user.UserActivityStartEvent;
import net.dv8tion.jda.api.events.user.UserTypingEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

public class DiscordEventAdapter extends ListenerAdapter {

    private final HashMap<Class<?>, ArrayList<DiscordEventAbstract>> listenerMap;

    public DiscordEventAdapter() {
        listenerMap = new HashMap<>();

        Reflections reflections = new Reflections("events/discordevents");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(DiscordEvent.class);
        Set<Class<? extends DiscordEventAbstract>> listenerTypeAbstracts = reflections.getSubTypesOf(DiscordEventAbstract.class);

        annotated.stream()
                .map(clazz -> {
                    try {
                        return clazz.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        MainLogger.get().error("Error when creating listener class", e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .filter(obj -> obj instanceof DiscordEventAbstract)
                .map(obj -> (DiscordEventAbstract) obj)
                .forEach(listener -> putListener(listener, listenerTypeAbstracts));
    }

    private void putListener(DiscordEventAbstract listener, Set<Class<? extends DiscordEventAbstract>> listenerTypeAbstracts) {
        for (Class<?> clazz : listenerTypeAbstracts) {
            if (clazz.isInstance(listener)) {
                ArrayList<DiscordEventAbstract> listenerList = listenerMap.computeIfAbsent(clazz, k -> new ArrayList<>());
                listenerList.add(listener);
            }
        }
    }

    private ArrayList<DiscordEventAbstract> getListenerList(Class<? extends DiscordEventAbstract> clazz) {
        return listenerMap.computeIfAbsent(clazz, k -> new ArrayList<>());
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        ShardManager.initAssetIds(event.getJDA());
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> DiscordConnector.onJDAJoin(event.getJDA()));
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.isFromGuild()) {
            GlobalThreadPool.getExecutorService()
                    .submit(() -> GuildMessageReceivedAbstract.onGuildMessageReceivedStatic(event, getListenerList(GuildMessageReceivedAbstract.class)));
        } else {
            GlobalThreadPool.getExecutorService()
                    .submit(() -> PrivateMessageReceivedAbstract.onPrivateMessageReceivedStatic(event, getListenerList(PrivateMessageReceivedAbstract.class)));
        }
    }

    @Override
    public void onUserTyping(@NotNull UserTypingEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> UserTypingAbstract.onUserTypingStatic(event, getListenerList(UserTypingAbstract.class)));
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if (event.isFromGuild()) {
            GlobalThreadPool.getExecutorService()
                    .submit(() -> GuildMessageUpdateAbstract.onGuildMessageUpdateStatic(event, getListenerList(GuildMessageUpdateAbstract.class)));
        }
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if (event.isFromGuild()) {
            GlobalThreadPool.getExecutorService()
                    .submit(() -> GuildMessageDeleteAbstract.onGuildMessageDeleteStatic(event, getListenerList(GuildMessageDeleteAbstract.class)));
        }
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (event.isFromGuild()) {
            GlobalThreadPool.getExecutorService()
                    .submit(() -> GuildMessageReactionAddAbstract.onGuildMessageReactionAddStatic(event, getListenerList(GuildMessageReactionAddAbstract.class)));
        }
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        if (event.isFromGuild()) {
            GlobalThreadPool.getExecutorService()
                    .submit(() -> GuildMessageReactionRemoveAbstract.onGuildMessageReactionRemoveStatic(event, getListenerList(GuildMessageReactionRemoveAbstract.class)));
        }
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> GuildJoinAbstract.onGuildJoinStatic(event, getListenerList(GuildJoinAbstract.class)));
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> GuildLeaveAbstract.onGuildLeaveStatic(event, getListenerList(GuildLeaveAbstract.class)));
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> GuildMemberJoinAbstract.onGuildMemberJoinStatic(event, getListenerList(GuildMemberJoinAbstract.class)));
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> GuildMemberRemoveAbstract.onGuildMemberRemoveStatic(event, getListenerList(GuildMemberRemoveAbstract.class)));
    }

    @Override
    public void onGuildUnban(@NotNull GuildUnbanEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> GuildUnbanAbstract.onGuildUnbanStatic(event, getListenerList(GuildUnbanAbstract.class)));
    }

    @Override
    public void onChannelCreate(@NotNull ChannelCreateEvent event) {
        if (event.getChannelType().isMessage()) {
            GlobalThreadPool.getExecutorService()
                    .submit(() -> TextChannelCreateAbstract.onTextChannelCreateStatic(event, getListenerList(TextChannelCreateAbstract.class)));
        } else if (event.getChannelType().isAudio()) {
            GlobalThreadPool.getExecutorService()
                    .submit(() -> VoiceChannelCreateAbstract.onVoiceChannelCreateStatic(event, getListenerList(VoiceChannelCreateAbstract.class)));
        }
    }

    @Override
    public void onChannelDelete(@NotNull ChannelDeleteEvent event) {
        if (event.getChannelType().isMessage()) {
            GlobalThreadPool.getExecutorService()
                    .submit(() -> TextChannelDeleteAbstract.onTextChannelDeleteStatic(event, getListenerList(TextChannelDeleteAbstract.class)));
        } else if (event.getChannelType().isAudio()) {
            GlobalThreadPool.getExecutorService()
                    .submit(() -> VoiceChannelDeleteAbstract.onVoiceChannelDeleteStatic(event, getListenerList(VoiceChannelDeleteAbstract.class)));
        }
    }

    @Override
    public void onGenericPermissionOverride(@NotNull GenericPermissionOverrideEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> GenericPermissionOverrideAbstract.onGenericPermissionOverrideStatic(event, getListenerList(GenericPermissionOverrideAbstract.class)));
    }



    @Override
    public void onChannelUpdateUserLimit(@NotNull ChannelUpdateUserLimitEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> VoiceChannelUpdateUserLimitAbstract.onVoiceChannelUpdateUserLimitStatic(event, getListenerList(VoiceChannelUpdateUserLimitAbstract.class)));
    }

    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> GuildVoiceJoinAbstract.onGuildVoiceJoinStatic(event, getListenerList(GuildVoiceJoinAbstract.class)));
    }

    @Override
    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> GuildVoiceLeaveAbstract.onGuildVoiceLeaveStatic(event, getListenerList(GuildVoiceLeaveAbstract.class)));
    }

    @Override
    public void onGuildVoiceMove(@NotNull GuildVoiceMoveEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> GuildVoiceMoveAbstract.onGuildVoiceMoveStatic(event, getListenerList(GuildVoiceMoveAbstract.class)));
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> GuildMemberRoleAddAbstract.onGuildMemberRoleAddStatic(event, getListenerList(GuildMemberRoleAddAbstract.class)));
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> GuildMemberRoleRemoveAbstract.onGuildMemberRoleRemoveStatic(event, getListenerList(GuildMemberRoleRemoveAbstract.class)));
    }

    @Override
    public void onGuildUpdateBoostCount(@NotNull GuildUpdateBoostCountEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> GuildUpdateBoostCountAbstract.onGuildUpdateBoostCountStatic(event, getListenerList(GuildUpdateBoostCountAbstract.class)));
    }

    @Override
    public void onUserActivityStart(@NotNull UserActivityStartEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> UserActivityStartAbstract.onUserActivityStartStatic(event, getListenerList(UserActivityStartAbstract.class)));
    }

    @Override
    public void onGuildMemberUpdatePending(@NotNull GuildMemberUpdatePendingEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> GuildMemberUpdatePendingAbstract.onGuildMemberUpdatePendingStatic(event, getListenerList(GuildMemberUpdatePendingAbstract.class)));
    }



    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> SlashCommandAbstract.onSlashCommandStatic(event, getListenerList(SlashCommandAbstract.class)));
    }



    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> ButtonClickAbstract.onButtonClickStatic(event, getListenerList(ButtonClickAbstract.class)));
    }

    @Override
    public void onSelectMenuInteraction(@NotNull SelectMenuInteractionEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> SelectionMenuAbstract.onSelectionMenuStatic(event, getListenerList(SelectionMenuAbstract.class)));
    }

    @Override
    public void onHttpRequest(@NotNull HttpRequestEvent event) {
        String routeBased = event.getRequest().getRoute().getBaseRoute().getRoute();
        String[] routeBasedParts = routeBased.split("/");
        String[] routeCompiledParts = event.getRoute().getCompiledRoute().split("/");

        String route = event.getRoute().getMethod().toString() + " " + routeBased;
        RequestRouteLogger.logRoute(route, event.isRateLimit());

        Long guildId = null;
        for (int i = 0; i < routeBasedParts.length; i++) {
            if (routeBasedParts[i].equals("{channel_id}")) {
                long channelId = Long.parseLong(routeCompiledParts[i]);
                Optional<GuildChannel> channelOpt = ShardManager.getLocalGuildChannelById(channelId);
                if (channelOpt.isPresent()) {
                    guildId = channelOpt.get().getGuild().getIdLong();
                }
                break;
            } else if (routeBasedParts[i].equals("{guild_id}")) {
                guildId = Long.parseLong(routeCompiledParts[i]);
                break;
            }
        }

        RestLogger.insert(guildId);
    }

}
