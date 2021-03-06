package events.discordevents.guildvoiceleave;

import commands.runnables.utilitycategory.AutoChannelCommand;
import core.PermissionCheckRuntime;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildVoiceLeaveAbstract;
import mysql.modules.autochannel.AutoChannelBean;
import mysql.modules.autochannel.DBAutoChannel;
import mysql.modules.server.DBServer;
import mysql.modules.server.GuildBean;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import java.util.ArrayList;

@DiscordEvent(allowBots = true)
public class GuildVoiceChannelMemberLeaveAutoChannel extends GuildVoiceLeaveAbstract {

    @Override
    public boolean onGuildVoiceLeave(GuildVoiceLeaveEvent event) throws Throwable {
        AutoChannelBean autoChannelBean = DBAutoChannel.getInstance().retrieve(event.getServer().getId());

        for (long childChannelId: new ArrayList<>(autoChannelBean.getChildChannelIds())) {
            if (event.getChannel().getId() == childChannelId) {
                GuildBean guildBean = DBServer.getInstance().retrieve(event.getServer().getId());
                if (PermissionCheckRuntime.getInstance().botHasPermission(guildBean.getLocale(), AutoChannelCommand.class, event.getChannel(), PermissionDeprecated.MANAGE_CHANNEL | PermissionDeprecated.CONNECT)) {
                    if (event.getChannel().getConnectedUserIds().size() == 0) {
                        event.getChannel().delete(); //No error log
                    }
                }
                break;
            }
        }

        return true;
    }

}
