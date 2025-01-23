package events.discordevents.stringselectmenu;

import commands.Command;
import commands.CommandManager;
import commands.listeners.OnStaticStringSelectMenuListener;
import core.CustomObservableMap;
import core.MemberCacheController;
import core.utils.BotPermissionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.StringSelectMenuAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

@DiscordEvent
public class StringSelectMenuCommandsStatic extends StringSelectMenuAbstract {

    @Override
    public boolean onStringSelectMenu(StringSelectInteractionEvent event, EntityManagerWrapper entityManager) throws Throwable {
        if (event.getGuild() == null || !BotPermissionUtil.canWriteEmbed(event.getGuildChannel())) {
            return true;
        }

        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        CustomObservableMap<Long, StaticReactionMessageData> map = DBStaticReactionMessages.getInstance()
                .retrieve(event.getGuild().getIdLong());
        StaticReactionMessageData messageData = map.get(event.getMessageIdLong());

        if (messageData != null) {
            Command command = CommandManager.createCommandByTrigger(messageData.getCommand(), guildEntity.getLocale(), guildEntity.getPrefix()).get();
            if (command instanceof OnStaticStringSelectMenuListener && map.containsKey(event.getMessageIdLong())) {
                if (command.getCommandProperties().requiresFullMemberCache()) {
                    MemberCacheController.getInstance().loadMembersFull(event.getGuild()).get();
                }
                command.setGuildEntity(guildEntity);
                command.setAtomicGuild(event.getGuild());
                ((OnStaticStringSelectMenuListener) command).onStaticStringSelectMenu(event, messageData.getSecondaryId());
                return false;
            }
        }

        return true;
    }

}
