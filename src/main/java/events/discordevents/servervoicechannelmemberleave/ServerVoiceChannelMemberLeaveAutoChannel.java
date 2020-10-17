package events.discordevents.servervoicechannelmemberleave;

import commands.runnables.utilitycategory.AutoChannelCommand;
import constants.Permission;
import core.PermissionCheckRuntime;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ServerVoiceChannelMemberLeaveAbstract;
import mysql.modules.autochannel.AutoChannelBean;
import mysql.modules.autochannel.DBAutoChannel;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;
import org.javacord.api.event.channel.server.voice.ServerVoiceChannelMemberLeaveEvent;
import java.util.ArrayList;

@DiscordEvent(allowBots = true)
public class ServerVoiceChannelMemberLeaveAutoChannel extends ServerVoiceChannelMemberLeaveAbstract {

    @Override
    public boolean onServerVoiceChannelMemberLeave(ServerVoiceChannelMemberLeaveEvent event) throws Throwable {
        AutoChannelBean autoChannelBean = DBAutoChannel.getInstance().getBean(event.getServer().getId());

        for (long childChannelId: new ArrayList<>(autoChannelBean.getChildChannelIds())) {
            if (event.getChannel().getId() == childChannelId) {
                ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().getId());
                if (PermissionCheckRuntime.getInstance().botHasPermission(serverBean.getLocale(), AutoChannelCommand.class, event.getChannel(), Permission.MANAGE_CHANNEL | Permission.CONNECT)) {
                    if (event.getChannel().getConnectedUsers().size() == 0) {
                        event.getChannel().delete(); //No error log
                    }
                }
                break;
            }
        }

        return true;
    }

}
