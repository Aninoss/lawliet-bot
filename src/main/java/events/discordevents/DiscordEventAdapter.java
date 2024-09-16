package events.discordevents;

import commands.SlashCommandManager;
import constants.ExternalLinks;
import core.*;
import core.utils.JDAUtil;
import events.discordevents.eventtypeabstracts.*;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.DiscordSubscriptionEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateUserLimitEvent;
import net.dv8tion.jda.api.events.entitlement.EntitlementCreateEvent;
import net.dv8tion.jda.api.events.entitlement.EntitlementDeleteEvent;
import net.dv8tion.jda.api.events.entitlement.EntitlementUpdateEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.*;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdatePendingEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateTimeOutEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateBoostCountEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.http.HttpRequestEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.*;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.user.UserActivityStartEvent;
import net.dv8tion.jda.api.events.user.UserTypingEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

import java.time.Instant;
import java.util.*;

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
        GlobalThreadPool.submit(() -> DiscordConnector.onJDAJoin(event.getJDA()));
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        Instant startTime = Instant.now();
        if (event.getChannel() instanceof GuildMessageChannel) {
            GlobalThreadPool.submit(() -> GuildMessageReceivedAbstract.onGuildMessageReceivedStatic(event, getListenerList(GuildMessageReceivedAbstract.class), startTime));
        } else if (event.getChannel() instanceof PrivateChannel) {
            GlobalThreadPool.submit(() -> PrivateMessageReceivedAbstract.onPrivateMessageReceivedStatic(event, getListenerList(PrivateMessageReceivedAbstract.class)));
        }
    }

    @Override
    public void onUserTyping(@NotNull UserTypingEvent event) {
        if (event.getChannel() instanceof GuildMessageChannel) {
            GlobalThreadPool.submit(() -> UserTypingAbstract.onUserTypingStatic(event, getListenerList(UserTypingAbstract.class)));
        }
    }

    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if (event.getChannel() instanceof GuildMessageChannel) {
            GlobalThreadPool.submit(() -> GuildMessageUpdateAbstract.onGuildMessageUpdateStatic(event, getListenerList(GuildMessageUpdateAbstract.class)));
        }
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if (event.getChannel() instanceof GuildMessageChannel) {
            GlobalThreadPool.submit(() -> GuildMessageDeleteAbstract.onGuildMessageDeleteStatic(event, getListenerList(GuildMessageDeleteAbstract.class)));
        }
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (event.getChannel() instanceof GuildMessageChannel) {
            GlobalThreadPool.submit(() -> GuildMessageReactionAddAbstract.onGuildMessageReactionAddStatic(event, getListenerList(GuildMessageReactionAddAbstract.class)));
        }
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        if (event.getChannel() instanceof GuildMessageChannel) {
            GlobalThreadPool.submit(() -> GuildMessageReactionRemoveAbstract.onGuildMessageReactionRemoveStatic(event, getListenerList(GuildMessageReactionRemoveAbstract.class)));
        }
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        GlobalThreadPool.submit(() -> GuildJoinAbstract.onGuildJoinStatic(event, getListenerList(GuildJoinAbstract.class)));
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        GlobalThreadPool.submit(() -> GuildLeaveAbstract.onGuildLeaveStatic(event, getListenerList(GuildLeaveAbstract.class)));
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        GlobalThreadPool.submit(() -> GuildMemberJoinAbstract.onGuildMemberJoinStatic(event, getListenerList(GuildMemberJoinAbstract.class)));
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        GlobalThreadPool.submit(() -> GuildMemberRemoveAbstract.onGuildMemberRemoveStatic(event, getListenerList(GuildMemberRemoveAbstract.class)));
    }

    @Override
    public void onGuildUnban(@NotNull GuildUnbanEvent event) {
        GlobalThreadPool.submit(() -> GuildUnbanAbstract.onGuildUnbanStatic(event, getListenerList(GuildUnbanAbstract.class)));
    }

    @Override
    public void onChannelDelete(@NotNull ChannelDeleteEvent event) {
        if (event.getChannel() instanceof GuildMessageChannel) {
            GlobalThreadPool.submit(() -> MessageChannelDeleteAbstract.onMessageChannelDeleteStatic(event, getListenerList(MessageChannelDeleteAbstract.class)));
        }
        if (event.getChannel() instanceof VoiceChannel) {
            GlobalThreadPool.submit(() -> VoiceChannelDeleteAbstract.onVoiceChannelDeleteStatic(event, getListenerList(VoiceChannelDeleteAbstract.class)));
        }
    }

    @Override
    public void onChannelUpdateUserLimit(@NotNull ChannelUpdateUserLimitEvent event) {
        if (event.getChannel() instanceof VoiceChannel) {
            GlobalThreadPool.submit(() -> VoiceChannelUpdateUserLimitAbstract.onVoiceChannelUpdateUserLimitStatic(event, getListenerList(VoiceChannelUpdateUserLimitAbstract.class)));
        }
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        GlobalThreadPool.submit(() -> GuildVoiceUpdateAbstract.onGuildVoiceUpdateStatic(event, getListenerList(GuildVoiceUpdateAbstract.class)));
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        GlobalThreadPool.submit(() -> GuildMemberRoleAddAbstract.onGuildMemberRoleAddStatic(event, getListenerList(GuildMemberRoleAddAbstract.class)));
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        GlobalThreadPool.submit(() -> GuildMemberRoleRemoveAbstract.onGuildMemberRoleRemoveStatic(event, getListenerList(GuildMemberRoleRemoveAbstract.class)));
    }

    @Override
    public void onGenericGuildMember(@NotNull GenericGuildMemberEvent event) {
        GlobalThreadPool.submit(() -> GenericGuildMemberAbstract.onGuildMemberStatic(event, getListenerList(GenericGuildMemberAbstract.class)));
    }

    @Override
    public void onGuildUpdateBoostCount(@NotNull GuildUpdateBoostCountEvent event) {
        GlobalThreadPool.submit(() -> GuildUpdateBoostCountAbstract.onGuildUpdateBoostCountStatic(event, getListenerList(GuildUpdateBoostCountAbstract.class)));
    }

    @Override
    public void onUserActivityStart(@NotNull UserActivityStartEvent event) {
        GlobalThreadPool.submit(() -> UserActivityStartAbstract.onUserActivityStartStatic(event, getListenerList(UserActivityStartAbstract.class)));
    }

    @Override
    public void onGuildMemberUpdatePending(@NotNull GuildMemberUpdatePendingEvent event) {
        GlobalThreadPool.submit(() -> GuildMemberUpdatePendingAbstract.onGuildMemberUpdatePendingStatic(event, getListenerList(GuildMemberUpdatePendingAbstract.class)));
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Instant startTime = Instant.now();
        GlobalThreadPool.submit(() -> SlashCommandAbstract.onSlashCommandStatic(event, getListenerList(SlashCommandAbstract.class), startTime));
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        GlobalThreadPool.submit(() -> ButtonClickAbstract.onButtonClickStatic(event, getListenerList(ButtonClickAbstract.class)));
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        GlobalThreadPool.submit(() -> StringSelectMenuAbstract.onStringSelectMenuStatic(event, getListenerList(StringSelectMenuAbstract.class)));
    }

    @Override
    public void onEntitySelectInteraction(@NotNull EntitySelectInteractionEvent event) {
        GlobalThreadPool.submit(() -> EntitySelectMenuAbstract.onEntitySelectMenuStatic(event, getListenerList(EntitySelectMenuAbstract.class)));
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        GlobalThreadPool.submit(() -> ModalInteractionAbstract.onModalInteractionStatic(event, getListenerList(ModalInteractionAbstract.class)));
    }

    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        GlobalThreadPool.submit(() -> event.replyChoices(SlashCommandManager.retrieveChoices(event))
                .submit()
                .exceptionally(ExceptionLogger.get("10062"))
        );
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
        if (event.isFromGuild()) {
            GlobalThreadPool.submit(() -> GuildMessageContextInteractionAbstract.onGuildMessageContextInteractionStatic(event, getListenerList(GuildMessageContextInteractionAbstract.class)));
        }
    }

    @Override
    public void onUserContextInteraction(@NotNull UserContextInteractionEvent event) {
        if (event.isFromGuild()) {
            GlobalThreadPool.submit(() -> GuildUserContextInteractionAbstract.onGuildUserContextInteractionStatic(event, getListenerList(GuildUserContextInteractionAbstract.class)));
        }
    }

    @Override
    public void onGuildInviteCreate(@NotNull GuildInviteCreateEvent event) {
        GlobalThreadPool.submit(() -> GuildInviteCreateAbstract.onGuildInviteCreateStatic(event, getListenerList(GuildInviteCreateAbstract.class)));
    }

    @Override
    public void onGuildInviteDelete(@NotNull GuildInviteDeleteEvent event) {
        GlobalThreadPool.submit(() -> GuildInviteDeleteAbstract.onGuildInviteDeleteStatic(event, getListenerList(GuildInviteDeleteAbstract.class)));
    }

    @Override
    public void onGuildMemberUpdateTimeOut(@NotNull GuildMemberUpdateTimeOutEvent event) {
        GlobalThreadPool.submit(() -> GuildMemberUpdateTimeOutAbstract.onGuildMemberUpdateTimeOutAbstractStatic(event, getListenerList(GuildMemberUpdateTimeOutAbstract.class)));
    }

    @Override
    public void onApplicationCommandUpdatePrivileges(@NotNull ApplicationCommandUpdatePrivilegesEvent event) {
        GlobalThreadPool.submit(() -> ApplicationCommandUpdatePrivilegesAbstract.onApplicationCommandUpdatePrivilegesStatic(event, getListenerList(ApplicationCommandUpdatePrivilegesAbstract.class)));
    }

    @Override
    public void onApplicationUpdatePrivileges(@NotNull ApplicationUpdatePrivilegesEvent event) {
        GlobalThreadPool.submit(() -> ApplicationUpdatePrivilegesAbstract.onApplicationUpdatePrivilegesStatic(event, getListenerList(ApplicationUpdatePrivilegesAbstract.class)));
    }

    @Override
    public void onEntitlementCreate(@NotNull EntitlementCreateEvent event) {
        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(DiscordEventAdapter.class)) {
            DiscordSubscriptionEntity discordSubscriptionEntity = new DiscordSubscriptionEntity(event.getEntitlement().getId());
            discordSubscriptionEntity.setUserId(event.getEntitlement().getUserIdLong());
            discordSubscriptionEntity.setTimeEnding(event.getEntitlement().getTimeEnding() != null ? event.getEntitlement().getTimeEnding().toInstant() : null);

            entityManager.getTransaction().begin();
            entityManager.persist(discordSubscriptionEntity);
            entityManager.getTransaction().commit();
        }

        String text = "Thank you for your support!\n\nYou can [join](" + ExternalLinks.BETA_SERVER_INVITE + ") the private Discord server with direct bot support and check out the monthly [development votes](" + ExternalLinks.DEVELOPMENT_VOTES_URL + ")!";
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setDescription(text);
        JDAUtil.openPrivateChannel(ShardManager.getAnyJDA().get(), event.getEntitlement().getUserIdLong())
                .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()))
                .queue();
    }

    @Override
    public void onEntitlementUpdate(@NotNull EntitlementUpdateEvent event) {
        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(DiscordEventAdapter.class)) {
            DiscordSubscriptionEntity discordSubscriptionEntity = entityManager.find(DiscordSubscriptionEntity.class, event.getEntitlement().getId());
            if (discordSubscriptionEntity == null) {
                MainLogger.get().error("Cannot update entitlement with id {} because it doesn't exist!", event.getEntitlement().getId());
                return;
            }

            entityManager.getTransaction().begin();
            discordSubscriptionEntity.setTimeEnding(event.getEntitlement().getTimeEnding() != null ? event.getEntitlement().getTimeEnding().toInstant() : null);
            entityManager.getTransaction().commit();
        }
    }

    @Override
    public void onEntitlementDelete(@NotNull EntitlementDeleteEvent event) {
        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(DiscordEventAdapter.class)) {
            DiscordSubscriptionEntity discordSubscriptionEntity = entityManager.find(DiscordSubscriptionEntity.class, event.getEntitlement().getId());
            if (discordSubscriptionEntity == null) {
                MainLogger.get().error("Cannot delete entitlement with id {} because it doesn't exist!", event.getEntitlement().getId());
                return;
            }

            entityManager.getTransaction().begin();
            entityManager.remove(discordSubscriptionEntity);
            entityManager.getTransaction().commit();
        }
    }

    @Override
    public void onHttpRequest(@NotNull HttpRequestEvent event) {
        if (event.getResponse() != null && event.getResponse().code / 100 != 2) {
            MainLogger.get().warn("Http response code {} for {} {}", event.getResponse().code, event.getRequest().getRoute().getMethod(), event.getRequest().getRoute().getCompiledRoute());
        }

        String routeBased = event.getRequest().getRoute().getBaseRoute().getRoute();
        String[] routeBasedParts = routeBased.split("/");
        String[] routeCompiledParts = event.getRoute().getCompiledRoute().split("/");

        String route = event.getRoute().getMethod() + " " + routeBased;
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
