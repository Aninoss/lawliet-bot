package modules;

import java.util.List;
import commands.runnables.utilitycategory.TicketCommand;
import core.PermissionCheckRuntime;
import core.utils.BotPermissionUtil;
import mysql.modules.guild.GuildData;
import mysql.modules.ticket.DBTicket;
import mysql.modules.ticket.TicketData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

public class Ticket {

    public static void createTicketChannel(TextChannel textChannel, Member member) {
        TicketData ticketData = DBTicket.getInstance().retrieve(textChannel.getGuild().getIdLong());
        Guild guild = textChannel.getGuild();
        GuildData guildBean = ticketData.getGuildBean();
        if (PermissionCheckRuntime.getInstance().botHasPermission(guildBean.getLocale(), TicketCommand.class, guild, Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL) &&
                PermissionCheckRuntime.getInstance().botHasPermission(guildBean.getLocale(), TicketCommand.class, textChannel.getParent(), Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL)
        ) {
            createNewVoice(ticketData, textChannel, member).queue();
        }
    }

    private static ChannelAction<TextChannel> createNewVoice(TicketData ticketData, TextChannel parentChannel, Member member) {
        Guild guild = parentChannel.getGuild();
        ChannelAction<TextChannel> channelAction;
        if (parentChannel.getParent() != null) {
            channelAction = parentChannel.getParent().createTextChannel(createNewVoiceName(ticketData));
        } else {
            channelAction = parentChannel.getGuild().createTextChannel(createNewVoiceName(ticketData));
        }

        channelAction = BotPermissionUtil.clearPermissionOverrides(channelAction);
        return addPermissions(parentChannel, channelAction, ticketData.getStaffRoleIds().transform(guild::getRoleById, ISnowflake::getIdLong), member);
    }

    private static String createNewVoiceName(TicketData ticketData) {
        return String.format("%04d", ticketData.increaseCounterAndGet());
    }

    private static ChannelAction<TextChannel> addPermissions(TextChannel parentChannel, ChannelAction<TextChannel> channelAction, List<Role> staffRoles, Member member) {
        for (PermissionOverride permissionOverride : parentChannel.getPermissionOverrides()) {
            channelAction = BotPermissionUtil.addPermission(parentChannel, channelAction, permissionOverride.getPermissionHolder(), false, Permission.VIEW_CHANNEL);
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
