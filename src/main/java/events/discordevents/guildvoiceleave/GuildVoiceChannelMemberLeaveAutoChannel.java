package events.discordevents.guildvoiceleave;

import commands.runnables.utilitycategory.AutoChannelCommand;
import core.PermissionCheckRuntime;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildVoiceLeaveAbstract;
import mysql.modules.autochannel.AutoChannelBean;
import mysql.modules.autochannel.DBAutoChannel;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import java.util.ArrayList;

@DiscordEvent(allowBots = true)
public class GuildVoiceChannelMemberLeaveAutoChannel extends GuildVoiceLeaveAbstract {

    @Override
    public boolean onGuildVoiceLeave(GuildVoiceLeaveEvent event) throws Throwable {
        AutoChannelBean autoChannelBean = DBAutoChannel.getInstance().getBean(event.getServer().getId());

        for (long childChannelId: new ArrayList<>(autoChannelBean.getChildChannelIds())) {
            if (event.getChannel().getId() == childChannelId) {
                ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().getId());
                if (PermissionCheckRuntime.getInstance().botHasPermission(serverBean.getLocale(), AutoChannelCommand.class, event.getChannel(), PermissionDeprecated.MANAGE_CHANNEL | PermissionDeprecated.CONNECT)) {
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
