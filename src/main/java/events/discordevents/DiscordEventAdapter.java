package events.discordevents;

import java.util.*;
import core.*;
import core.buttons.GuildComponentInteractionEvent;
import core.cache.MessageCache;
import core.internet.HttpProperty;
import core.internet.HttpRequest;
import events.discordevents.eventtypeabstracts.*;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.RawGatewayEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdateUserLimitEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdatePendingEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateBoostCountEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.http.HttpRequestEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.user.UserActivityStartEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.GuildImpl;
import net.dv8tion.jda.internal.requests.Route;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.reflections.Reflections;

public class DiscordEventAdapter extends ListenerAdapter {

    private final static Route INTERACTION_CALLBACK_ROUTE = Route.post("https://discord.com/api/v8/interactions/{interaction_id}/{interaction_token}/callback");

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
        ShardManager.getInstance().initAssetIds(event.getJDA());
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        DiscordConnector.getInstance().onJDAJoin(event.getJDA());
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        MessageCache.getInstance().put(event.getMessage());
        GlobalThreadPool.getExecutorService()
                .submit(() -> GuildMessageReceivedAbstract.onGuildMessageReceivedStatic(event, getListenerList(GuildMessageReceivedAbstract.class)));
    }

    @Override
    public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent event) {
        PrivateMessageReceivedAbstract.onPrivateMessageReceivedStatic(event, getListenerList(PrivateMessageReceivedAbstract.class));
    }

    @Override
    public void onGuildMessageUpdate(@NotNull GuildMessageUpdateEvent event) {
        MessageCache.getInstance().put(event.getMessage());
        GlobalThreadPool.getExecutorService()
                .submit(() -> GuildMessageUpdateAbstract.onGuildMessageUpdateStatic(event, getListenerList(GuildMessageUpdateAbstract.class)));
    }

    @Override
    public void onGuildMessageDelete(@NotNull GuildMessageDeleteEvent event) {
        GuildMessageDeleteAbstract.onGuildMessageDeleteStatic(event, getListenerList(GuildMessageDeleteAbstract.class));
    }

    @Override
    public void onGuildMessageReactionAdd(@NotNull GuildMessageReactionAddEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> GuildMessageReactionAddAbstract.onGuildMessageReactionAddStatic(event, getListenerList(GuildMessageReactionAddAbstract.class)));
    }

    @Override
    public void onGuildMessageReactionRemove(@NotNull GuildMessageReactionRemoveEvent event) {
        GlobalThreadPool.getExecutorService()
                .submit(() -> GuildMessageReactionRemoveAbstract.onGuildMessageReactionRemoveStatic(event, getListenerList(GuildMessageReactionRemoveAbstract.class)));
    }

    @Override
    public void onVoiceChannelDelete(@NotNull VoiceChannelDeleteEvent event) {
        VoiceChannelDeleteAbstract.onVoiceChannelDeleteStatic(event, getListenerList(VoiceChannelDeleteAbstract.class));
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        GuildJoinAbstract.onGuildJoinStatic(event, getListenerList(GuildJoinAbstract.class));
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        GuildLeaveAbstract.onGuildLeaveStatic(event, getListenerList(GuildLeaveAbstract.class));
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        GuildMemberJoinAbstract.onGuildMemberJoinStatic(event, getListenerList(GuildMemberJoinAbstract.class));
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        GuildMemberRemoveAbstract.onGuildMemberRemoveStatic(event, getListenerList(GuildMemberRemoveAbstract.class));
    }

    @Override
    public void onGuildUnban(@NotNull GuildUnbanEvent event) {
        GuildUnbanAbstract.onGuildUnbanStatic(event, getListenerList(GuildUnbanAbstract.class));
    }

    @Override
    public void onTextChannelDelete(@NotNull TextChannelDeleteEvent event) {
        TextChannelDeleteAbstract.onTextChannelDeleteStatic(event, getListenerList(TextChannelDeleteAbstract.class));
    }

    @Override
    public void onVoiceChannelUpdateUserLimit(@NotNull VoiceChannelUpdateUserLimitEvent event) {
        VoiceChannelUpdateUserLimitAbstract.onVoiceChannelUpdateUserLimitStatic(event, getListenerList(VoiceChannelUpdateUserLimitAbstract.class));
    }

    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
        GuildVoiceJoinAbstract.onGuildVoiceJoinStatic(event, getListenerList(GuildVoiceJoinAbstract.class));
    }

    @Override
    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
        GuildVoiceLeaveAbstract.onGuildVoiceLeaveStatic(event, getListenerList(GuildVoiceLeaveAbstract.class));
    }

    @Override
    public void onGuildVoiceMove(@NotNull GuildVoiceMoveEvent event) {
        GuildVoiceMoveAbstract.onGuildVoiceMoveStatic(event, getListenerList(GuildVoiceMoveAbstract.class));
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        GuildMemberRoleAddAbstract.onGuildMemberRoleAddStatic(event, getListenerList(GuildMemberRoleAddAbstract.class));
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        GuildMemberRoleRemoveAbstract.onGuildMemberRoleRemoveStatic(event, getListenerList(GuildMemberRoleRemoveAbstract.class));
    }

    @Override
    public void onGuildUpdateBoostCount(@NotNull GuildUpdateBoostCountEvent event) {
        GuildUpdateBoostCountAbstract.onGuildUpdateBoostCountStatic(event, getListenerList(GuildUpdateBoostCountAbstract.class));
    }

    @Override
    public void onUserActivityStart(@NotNull UserActivityStartEvent event) {
        UserActivityStartAbstract.onUserActivityStartStatic(event, getListenerList(UserActivityStartAbstract.class));
    }

    @Override
    public void onGuildMemberUpdatePending(@NotNull GuildMemberUpdatePendingEvent event) {
        GuildMemberUpdatePendingAbstract.onGuildMemberUpdatePendingStatic(event, getListenerList(GuildMemberUpdatePendingAbstract.class));
    }

    public void onGuildComponentInteraction(@NonNull GuildComponentInteractionEvent event) {
        GuildComponentInteractionAbstract.onGuildComponentInteractionStatic(event, getListenerList(GuildComponentInteractionAbstract.class));
    }

    @Override
    public void onHttpRequest(@NotNull HttpRequestEvent event) {
        String routeBased = event.getRequest().getRoute().getBaseRoute().getRoute();
        String[] routeBasedParts = routeBased.split("/");
        String[] routeCompiledParts = event.getRoute().getCompiledRoute().split("/");

        String route = event.getRoute().getMethod().toString() + " " + routeBased;
        RequestRouteLogger.getInstance().logRoute(route, event.isRateLimit());

        Long guildId = null;
        for (int i = 0; i < routeBasedParts.length; i++) {
            if (routeBasedParts[i].equals("{channel_id}")) {
                long channelId = Long.parseLong(routeCompiledParts[i]);
                Optional<GuildChannel> channelOpt = ShardManager.getInstance().getLocalGuildChannelById(channelId);
                if (channelOpt.isPresent()) {
                    guildId = channelOpt.get().getGuild().getIdLong();
                }
                break;
            } else if (routeBasedParts[i].equals("{guild_id}")) {
                guildId = Long.parseLong(routeCompiledParts[i]);
                break;
            }
        }

        RestLogger.getInstance().insert(guildId);
    }

    @Override
    public void onRawGateway(@NotNull RawGatewayEvent event) {
        if (event.getPackage().getString("t").equals("INTERACTION_CREATE") &&
                event.getPayload().getInt("type") == 3 &&
                event.getPayload().getObject("message").getInt("flags") == 0
        ) {
            DataObject payload = event.getPayload();
            DataObject data = payload.getObject("data");
            Message message = ((JDAImpl) event.getJDA()).getEntityBuilder().createMessage(payload.getObject("message"), true);
            Member member = ((JDAImpl) event.getJDA()).getEntityBuilder().createMember((GuildImpl) message.getGuild(), payload.getObject("member"));

            GuildComponentInteractionEvent interactionEvent = new GuildComponentInteractionEvent(
                    event.getJDA(),
                    event.getResponseNumber(),
                    member,
                    message,
                    data.getInt("component_type"),
                    data.getString("custom_id"),
                    Long.parseUnsignedLong(payload.getString("id")),
                    payload.getString("token")
            );

            sendInteractionResponse(interactionEvent, 6);
            onGuildComponentInteraction(interactionEvent);
        }
    }

    private void sendInteractionResponse(GuildComponentInteractionEvent event, int code) {
        Route.CompiledRoute route = INTERACTION_CALLBACK_ROUTE.compile(event.getId(), event.getToken());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", code);

        HttpRequest.getData(route.getCompiledRoute(), "POST", jsonObject.toString(), new HttpProperty("Content-Type", "application/json"))
                .exceptionally(ExceptionLogger.get());
    }

}
