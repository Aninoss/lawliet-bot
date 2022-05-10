package modules;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import commands.Category;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.runnables.utilitycategory.TicketCommand;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.MainLogger;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.cache.TicketProtocolCache;
import core.components.ActionRows;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.MentionUtil;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import mysql.modules.ticket.DBTicket;
import mysql.modules.ticket.TicketChannel;
import mysql.modules.ticket.TicketData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

public class Ticket {

    public static void createTicket(TicketData ticketData, TextChannel channel, Member member, String userMessage) {
        Optional<TextChannel> existingTicketChannelOpt = findTicketChannelOfUser(ticketData, member);
        if (existingTicketChannelOpt.isEmpty()) {
            Ticket.createTicketChannel(channel, member, ticketData).ifPresent(channelAction -> {
                channelAction.queue(textChannel -> setupNewTicketChannel(ticketData, textChannel, member, userMessage));
            });
        } else {
            TextChannel existingTicketChannel = existingTicketChannelOpt.get();
            if (PermissionCheckRuntime.botHasPermission(ticketData.getGuildData().getLocale(), TicketCommand.class, existingTicketChannel, Permission.MESSAGE_SEND)) {
                String text = TextManager.getString(ticketData.getGuildData().getLocale(), commands.Category.UTILITY, "ticket_alreadyopen", member.getAsMention());
                existingTicketChannel.sendMessage(text).queue();
            }
        }
    }

    private static Optional<TextChannel> findTicketChannelOfUser(TicketData ticketData, Member member) {
        for (TicketChannel ticketChannel : ticketData.getTicketChannels().values()) {
            if (ticketChannel.getMemberId() == member.getIdLong()) {
                Optional<TextChannel> textChannelOpt = ticketChannel.getTextChannel();
                if (textChannelOpt.isPresent()) {
                    return textChannelOpt;
                }
            }
        }
        return Optional.empty();
    }

    private static void setupNewTicketChannel(TicketData ticketData, TextChannel textChannel, Member member, String userMessage) {
        Locale locale = ticketData.getGuildData().getLocale();
        CommandProperties commandProperties = Command.getCommandProperties(TicketCommand.class);
        String title = commandProperties.emoji() + " " + Command.getCommandLanguage(TicketCommand.class, locale).getTitle();

        if (PermissionCheckRuntime.botHasPermission(ticketData.getGuildData().getLocale(), TicketCommand.class, textChannel, Permission.MESSAGE_SEND)) {
            /* member greeting */
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setTitle(title)
                    .setDescription(TextManager.getString(locale, Category.UTILITY, "ticket_greeting", TicketCommand.TICKET_CLOSE_EMOJI));
            textChannel.sendMessageEmbeds(eb.build())
                    .setActionRows(ActionRows.of(Button.of(ButtonStyle.DANGER, TicketCommand.BUTTON_ID_CLOSE, TextManager.getString(locale, Category.UTILITY, "ticket_button_close"))))
                    .content(member.getAsMention())
                    .queue(message -> DBStaticReactionMessages.getInstance()
                            .retrieve(message.getGuild().getIdLong())
                            .put(message.getIdLong(), new StaticReactionMessageData(message, commandProperties.trigger()))
                    );

            /* user message */
            if (userMessage != null) {
                EmbedBuilder userMessageEmbed = EmbedFactory.getEmbedDefault()
                        .setDescription(userMessage);
                userMessageEmbed = EmbedUtil.setMemberAuthor(userMessageEmbed, member);
                textChannel.sendMessageEmbeds(userMessageEmbed.build())
                        .queue();
            }

            /* create message */
            ticketData.getCreateMessage().ifPresent(createMessage -> {
                if (PermissionCheckRuntime.botHasPermission(ticketData.getGuildData().getLocale(), TicketCommand.class, textChannel, Permission.MESSAGE_SEND)) {
                    textChannel.sendMessage(createMessage)
                            .allowedMentions(null)
                            .queue();
                }
            });
        }

        /* post announcement to staff channel */
        AtomicBoolean announcementNotPosted = new AtomicBoolean(true);
        ticketData.getAnnouncementTextChannel().ifPresent(announcementChannel -> {
            if (PermissionCheckRuntime.botHasPermission(ticketData.getGuildData().getLocale(), TicketCommand.class, announcementChannel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
                announcementNotPosted.set(false);
                EmbedBuilder ebAnnouncement = EmbedFactory.getEmbedDefault()
                        .setTitle(title)
                        .setDescription(TextManager.getString(locale, Category.UTILITY, "ticket_announcement_open", member.getAsMention(), textChannel.getAsMention()));
                announcementChannel.sendMessage(ticketData.getPingStaff() ? getRolePing(textChannel.getGuild(), ticketData) : " ")
                        .setEmbeds(ebAnnouncement.build())
                        .allowedMentions(Collections.singleton(Message.MentionType.ROLE))
                        .queue(m -> {
                            ticketData.getTicketChannels().put(textChannel.getIdLong(), new TicketChannel(
                                    textChannel.getGuild().getIdLong(),
                                    textChannel.getIdLong(),
                                    member.getIdLong(),
                                    announcementChannel.getIdLong(),
                                    m.getIdLong(),
                                    false
                            ));
                        }, e -> {
                            MainLogger.get().error("Ticket announcement error", e);
                            ticketData.getTicketChannels().put(textChannel.getIdLong(), new TicketChannel(
                                    textChannel.getGuild().getIdLong(),
                                    textChannel.getIdLong(),
                                    member.getIdLong(),
                                    0L,
                                    0L,
                                    false
                            ));
                        });
            }
        });

        if (announcementNotPosted.get()) {
            ticketData.getTicketChannels().put(textChannel.getIdLong(), new TicketChannel(
                    textChannel.getGuild().getIdLong(),
                    textChannel.getIdLong(),
                    member.getIdLong(),
                    0L,
                    0L,
                    false
            ));
        }
    }

    private static String getRolePing(Guild guild, TicketData ticketData) {
        StringBuilder pings = new StringBuilder();
        ticketData.getStaffRoleIds()
                .transform(guild::getRoleById, ISnowflake::getIdLong)
                .forEach(role -> {
                    pings.append(role.getAsMention()).append(" ");
                });

        if (pings.isEmpty()) {
            return " ";
        }
        return pings.toString();
    }

    private static Optional<ChannelAction<TextChannel>> createTicketChannel(TextChannel textChannel, Member member, TicketData ticketData) {
        Guild guild = textChannel.getGuild();
        GuildData guildBean = ticketData.getGuildData();
        if (PermissionCheckRuntime.botHasPermission(guildBean.getLocale(), TicketCommand.class, guild, Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL) &&
                PermissionCheckRuntime.botHasPermission(guildBean.getLocale(), TicketCommand.class, textChannel.getParentCategory(), Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MANAGE_CHANNEL)
        ) {
            String ticket = String.format("%04d", ticketData.increaseCounterAndGet());
            return Optional.of(createNewChannel(ticketData, textChannel, member, ticket));
        }

        return Optional.empty();
    }

    public static void removeTicket(TextChannel ticketTextChannel, TicketData ticketData, TicketChannel ticketChannel) {
        ticketData.getTicketChannels().remove(ticketChannel.getTextChannelId());
        TextChannel textChannel = ticketTextChannel.getGuild().getTextChannelById(ticketChannel.getAnnouncementChannelId());
        if (textChannel != null) {
            Class<TicketCommand> clazz = TicketCommand.class;
            String csvUrl = TicketProtocolCache.getUrl(ticketChannel.getTextChannelId());
            Locale locale = ticketChannel.getGuildData().getLocale();
            String title = Command.getCommandProperties(clazz).emoji() + " " + Command.getCommandLanguage(clazz, locale).getTitle();
            String memberMention = MentionUtil.getUserAsMention(ticketChannel.getMemberId(), true);
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setTitle(title)
                    .setDescription(TextManager.getString(locale, Category.UTILITY, "ticket_announcement_closed", ticketTextChannel.getName(), memberMention));
            if (csvUrl != null) {
                EmbedUtil.addLog(eb, LogStatus.WARNING, TextManager.getString(locale, Category.UTILITY, "ticket_csv_warning"));
            }

            MessageAction messageAction = textChannel.editMessageById(ticketChannel.getAnnouncementMessageId(), Emojis.ZERO_WIDTH_SPACE)
                    .setEmbeds(eb.build());
            if (csvUrl != null) {
                Button button = Button.of(ButtonStyle.LINK, csvUrl, TextManager.getString(locale, Category.UTILITY, "ticket_csv_download"));
                messageAction = messageAction.setActionRows(ActionRows.of(button));
            }
            messageAction.queue();
        }
    }

    public static synchronized void assignTicket(Member member, TextChannel channel, TicketData ticketData, TicketChannel ticketChannel) {
        Guild guild = member.getGuild();
        GuildData guildData = DBGuild.getInstance().retrieve(guild.getIdLong());
        Locale locale = guildData.getLocale();
        if (!ticketChannel.isAssigned() && ticketChannel.getMemberId() != member.getIdLong() &&
                PermissionCheckRuntime.botHasPermission(locale, TicketCommand.class, channel.getParentCategory(), Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND) &&
                PermissionCheckRuntime.botHasPermission(locale, TicketCommand.class, channel, Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.MANAGE_PERMISSIONS)
        ) {
            ticketChannel.setAssigned();

            if (!DBTicket.getInstance().retrieve(guild.getIdLong()).getAssignToAll()) {
                TextChannelManager channelManager = channel.getManager();
                List<Role> staffRoles = ticketData.getStaffRoleIds().transform(guild::getRoleById, ISnowflake::getIdLong);
                for (Role staffRole : staffRoles) {
                    channelManager = (TextChannelManager) BotPermissionUtil.addPermission(channel, channelManager, staffRole, false, Permission.MESSAGE_SEND);
                }
                BotPermissionUtil.addPermission(channel, channelManager, member, true, Permission.MESSAGE_SEND)
                        .reason(Command.getCommandLanguage(TicketCommand.class, locale).getTitle())
                        .queue();

                TextChannel announcementChannel = guild.getTextChannelById(ticketChannel.getAnnouncementChannelId());
                if (announcementChannel != null) {
                    String title = Command.getCommandProperties(TicketCommand.class).emoji() + " " + Command.getCommandLanguage(TicketCommand.class, locale).getTitle();
                    String memberMention = MentionUtil.getUserAsMention(ticketChannel.getMemberId(), true);
                    EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                            .setTitle(title)
                            .setDescription(TextManager.getString(locale, Category.UTILITY, "ticket_announcement_assigned", channel.getAsMention(), memberMention, member.getAsMention()));
                    announcementChannel.editMessageById(ticketChannel.getAnnouncementMessageId(), " ")
                            .setEmbeds(eb.build())
                            .queue();
                }
            }
        }
    }

    private static ChannelAction<TextChannel> createNewChannel(TicketData ticketData, TextChannel parentChannel, Member member, String ticket) {
        Guild guild = parentChannel.getGuild();
        ChannelAction<TextChannel> channelAction;
        if (parentChannel.getParentCategory() != null) {
            channelAction = parentChannel.getParentCategory().createTextChannel(ticket);
        } else {
            channelAction = parentChannel.getGuild().createTextChannel(ticket);
        }

        channelAction = BotPermissionUtil.clearPermissionOverrides(channelAction);
        return addPermissions(parentChannel, channelAction, ticketData.getStaffRoleIds().transform(guild::getRoleById, ISnowflake::getIdLong), member);
    }

    private static ChannelAction<TextChannel> addPermissions(TextChannel parentChannel, ChannelAction<TextChannel> channelAction, List<Role> staffRoles, Member member) {
        channelAction = BotPermissionUtil.addPermission(parentChannel, channelAction, member.getGuild().getPublicRole(), false, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND);
        for (PermissionOverride permissionOverride : parentChannel.getPermissionOverrides()) {
            channelAction = BotPermissionUtil.addPermission(parentChannel, channelAction, permissionOverride, false, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND);
        }
        for (Role staffRole : staffRoles) {
            channelAction = BotPermissionUtil.addPermission(parentChannel, channelAction, staffRole, true,
                    Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND);
        }
        channelAction = BotPermissionUtil.addPermission(parentChannel, channelAction, member, true,
                Permission.VIEW_CHANNEL,Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND);
        channelAction = BotPermissionUtil.addPermission(parentChannel, channelAction, parentChannel.getGuild().getSelfMember(), true,
                Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND);
        return channelAction;
    }

}
