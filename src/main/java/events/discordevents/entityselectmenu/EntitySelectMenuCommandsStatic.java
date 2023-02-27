package events.discordevents.entityselectmenu;

import commands.Command;
import commands.CommandManager;
import commands.listeners.OnStaticEntitySelectMenuListener;
import core.CustomObservableMap;
import core.MemberCacheController;
import core.utils.BotPermissionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.EntitySelectMenuAbstract;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;

@DiscordEvent
public class EntitySelectMenuCommandsStatic extends EntitySelectMenuAbstract {

    @Override
    public boolean onEntitySelectMenu(EntitySelectInteractionEvent event) throws Throwable {
        if (!BotPermissionUtil.canWriteEmbed(event.getGuildChannel())) {
            return true;
        }

        CustomObservableMap<Long, StaticReactionMessageData> map = DBStaticReactionMessages.getInstance()
                .retrieve(event.getGuild().getIdLong());
        StaticReactionMessageData messageData = map.get(event.getMessageIdLong());

        if (messageData != null) {
            GuildData guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
            Command command = CommandManager.createCommandByTrigger(messageData.getCommand(), guildBean.getLocale(), guildBean.getPrefix()).get();
            if (command instanceof OnStaticEntitySelectMenuListener && map.containsKey(event.getMessageIdLong())) {
                if (command.getCommandProperties().requiresFullMemberCache()) {
                    MemberCacheController.getInstance().loadMembersFull(event.getGuild()).get();
                }
                ((OnStaticEntitySelectMenuListener) command).onStaticEntitySelectMenu(event, messageData.getSecondaryId());
                return false;
            }
        }

        return true;
    }

}
