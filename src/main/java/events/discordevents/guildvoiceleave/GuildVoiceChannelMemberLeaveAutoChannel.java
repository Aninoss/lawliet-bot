package events.discordevents.guildvoiceleave;

import commands.runnables.utilitycategory.AutoChannelCommand;
import core.PermissionCheckRuntime;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildVoiceLeaveAbstract;
import mysql.modules.autochannel.AutoChannelBean;
import mysql.modules.autochannel.DBAutoChannel;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import java.util.ArrayList;

@DiscordEvent(allowBots = true)
public class GuildVoiceChannelMemberLeaveAutoChannel extends GuildVoiceLeaveAbstract {

    @Override
    public boolean onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        AutoChannelBean autoChannelBean = DBAutoChannel.getInstance().retrieve(event.getGuild().getIdLong());

        for (long childChannelId: new ArrayList<>(autoChannelBean.getChildChannelIds())) {
            if (event.getChannelLeft().getIdLong() == childChannelId) {
                if (PermissionCheckRuntime.getInstance().botHasPermission(autoChannelBean.getGuildBean().getLocale(), AutoChannelCommand.class, event.getChannelLeft(), Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL)) {
                    if (event.getChannelLeft().getMembers().size() == 0) {
                        event.getChannelLeft().delete().queue();
                    }
                }
                break;
            }
        }

        return true;
    }

}
