package events.discordevents.guildcomponentinteraction;

import commands.Command;
import commands.CommandManager;
import commands.listeners.OnStaticButtonListener;
import core.CustomObservableMap;
import core.buttons.GuildComponentInteractionEvent;
import core.utils.BotPermissionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildComponentInteractionAbstract;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;

@DiscordEvent
public class GuildComponentInteractionCommandsStatic extends GuildComponentInteractionAbstract {

    @Override
    public boolean onGuildComponentInteraction(GuildComponentInteractionEvent event) throws Throwable {
        if (!BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            return true;
        }

        CustomObservableMap<Long, StaticReactionMessageData> map = DBStaticReactionMessages.getInstance()
                .retrieve(event.getGuild().getIdLong());
        StaticReactionMessageData messageData = map.get(event.getMessageIdLong());

        if (messageData != null) {
            GuildData guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
            Command command = CommandManager.createCommandByTrigger(messageData.getCommand(), guildBean.getLocale(), guildBean.getPrefix()).get();
            if (command instanceof OnStaticButtonListener && map.containsKey(event.getMessageIdLong())) {
                ((OnStaticButtonListener) command).onStaticButton(event);
            }
        }

        return true;
    }

}
