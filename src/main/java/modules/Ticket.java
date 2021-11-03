package modules;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import commands.Category;
import commands.Command;
import commands.runnables.utilitycategory.TicketCommand;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.cache.TicketProtocolCache;
import core.components.ActionRows;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.MentionUtil;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import mysql.modules.ticket.DBTicket;
import mysql.modules.ticket.TicketChannel;
import mysql.modules.ticket.TicketData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.managers.ChannelManager;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

public class Ticket {

    public static Optional<ChannelAction<TextChannel>> createTicketChannel(TextChannel textChannel, Member member, TicketData ticketData) {
        Guild guild = textChannel.getGuild();
        GuildData guildBean = ticketData.getGuildData();
        if (PermissionCheckRuntime.botHasPermission(guildBean.getLocale(), TicketCommand.class, guild, Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL) &&
                PermissionCheckRuntime.botHasPermission(guildBean.getLocale(), TicketCommand.class, textChannel.getParent(), Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE, Permission.MANAGE_CHANNEL)
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
                PermissionCheckRuntime.botHasPermission(locale, TicketCommand.class, channel.getParent(), Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE) &&
                PermissionCheckRuntime.botHasPermission(locale, TicketCommand.class, channel, Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.MANAGE_PERMISSIONS)
        ) {
            ticketChannel.setAssigned();

            if (!DBTicket.getInstance().retrieve(guild.getIdLong()).getAssignToAll()) {
                ChannelManager channelManager = channel.getManager();
                List<Role> staffRoles = ticketData.getStaffRoleIds().transform(guild::getRoleById, ISnowflake::getIdLong);
                for (Role staffRole : staffRoles) {
                    channelManager = BotPermissionUtil.addPermission(channel, channelManager, staffRole, false, Permission.MESSAGE_WRITE);
                }
                BotPermissionUtil.addPermission(channel, channelManager, member, true, Permission.MESSAGE_WRITE)
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
        if (parentChannel.getParent() != null) {
            channelAction = parentChannel.getParent().createTextChannel(ticket);
        } else {
            channelAction = parentChannel.getGuild().createTextChannel(ticket);
        }

        channelAction = BotPermissionUtil.clearPermissionOverrides(channelAction);
        return addPermissions(parentChannel, channelAction, ticketData.getStaffRoleIds().transform(guild::getRoleById, ISnowflake::getIdLong), member);
    }

    private static ChannelAction<TextChannel> addPermissions(TextChannel parentChannel, ChannelAction<TextChannel> channelAction, List<Role> staffRoles, Member member) {
        channelAction = BotPermissionUtil.addPermission(parentChannel, channelAction, member.getGuild().getPublicRole(), false, Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE);
        for (PermissionOverride permissionOverride : parentChannel.getPermissionOverrides()) {
            channelAction = BotPermissionUtil.addPermission(parentChannel, channelAction, permissionOverride, false, Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE);
        }
        for (Role staffRole : staffRoles) {
            channelAction = BotPermissionUtil.addPermission(parentChannel, channelAction, staffRole, true,
                    Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_WRITE);
        }
        channelAction = BotPermissionUtil.addPermission(parentChannel, channelAction, member, true,
                Permission.VIEW_CHANNEL,Permission.MESSAGE_HISTORY, Permission.MESSAGE_WRITE);
        channelAction = BotPermissionUtil.addPermission(parentChannel, channelAction, parentChannel.getGuild().getSelfMember(), true,
                Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_WRITE);
        return channelAction;
    }

}
