package modules;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import commands.Command;
import commands.runnables.utilitycategory.TicketCommand;
import constants.Category;
import core.EmbedFactory;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.atomicassets.AtomicMember;
import core.utils.BotPermissionUtil;
import mysql.modules.guild.GuildData;
import mysql.modules.ticket.TicketChannel;
import mysql.modules.ticket.TicketData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

public class Ticket {

    public static Optional<ChannelAction<TextChannel>> createTicketChannel(TextChannel textChannel, Member member, TicketData ticketData) {
        Guild guild = textChannel.getGuild();
        GuildData guildBean = ticketData.getGuildBean();
        if (PermissionCheckRuntime.getInstance().botHasPermission(guildBean.getLocale(), TicketCommand.class, guild, Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL) &&
                PermissionCheckRuntime.getInstance().botHasPermission(guildBean.getLocale(), TicketCommand.class, textChannel.getParent(), Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL)
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
            Locale locale = ticketChannel.getGuildBean().getLocale();
            String title = Command.getCommandProperties(clazz).emoji() + " " + Command.getCommandLanguage(clazz, locale).getTitle();
            AtomicMember atomicMember = new AtomicMember(ticketChannel.getGuildId(), ticketChannel.getMemberId());
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setTitle(title)
                    .setDescription(TextManager.getString(locale, Category.UTILITY, "ticket_announcement_closed", ticketTextChannel.getName(), atomicMember.getAsMention()));
            textChannel.editMessageById(ticketChannel.getAnnouncementMessageId(), " ")
                    .setEmbeds(eb.build())
                    .queue();
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
        channelAction = BotPermissionUtil.addPermission(parentChannel, channelAction, member.getGuild().getPublicRole(), false, Permission.VIEW_CHANNEL);
        for (PermissionOverride permissionOverride : parentChannel.getPermissionOverrides()) {
            channelAction = BotPermissionUtil.addPermission(parentChannel, channelAction, permissionOverride, false, Permission.VIEW_CHANNEL);
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
