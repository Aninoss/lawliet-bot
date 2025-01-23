package events.discordevents.buttonclick;

import commands.Command;
import commands.CommandManager;
import commands.listeners.OnStaticButtonListener;
import core.CustomObservableMap;
import core.MemberCacheController;
import core.utils.BotPermissionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ButtonClickAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

@DiscordEvent
public class ButtonClickCommandsStatic extends ButtonClickAbstract {

    @Override
    public boolean onButtonClick(ButtonInteractionEvent event, EntityManagerWrapper entityManager) throws Throwable {
        if (event.getGuild() == null || !BotPermissionUtil.canWriteEmbed(event.getGuildChannel())) {
            return true;
        }

        CustomObservableMap<Long, StaticReactionMessageData> map = DBStaticReactionMessages.getInstance()
                .retrieve(event.getGuild().getIdLong());
        StaticReactionMessageData messageData = map.get(event.getMessageIdLong());

        if (messageData != null) {
            GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
            Command command = CommandManager.createCommandByTrigger(messageData.getCommand(), guildEntity.getLocale(), guildEntity.getPrefix()).get();
            if (command instanceof OnStaticButtonListener && map.containsKey(event.getMessageIdLong())) {
                if (command.getCommandProperties().requiresFullMemberCache()) {
                    MemberCacheController.getInstance().loadMembersFull(event.getGuild()).get();
                }
                command.setGuildEntity(guildEntity);
                command.setAtomicGuild(event.getGuild());
                ((OnStaticButtonListener) command).onStaticButton(event, messageData.getSecondaryId());
                return false;
            }
        }

        return true;
    }

}
