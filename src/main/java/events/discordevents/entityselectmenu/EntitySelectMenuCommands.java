package events.discordevents.entityselectmenu;

import commands.listeners.OnEntitySelectMenuListener;
import events.discordevents.DiscordEvent;
import events.discordevents.InteractionListenerHandler;
import events.discordevents.eventtypeabstracts.EntitySelectMenuAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.GuildEntity;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;

@DiscordEvent
public class EntitySelectMenuCommands extends EntitySelectMenuAbstract implements InteractionListenerHandler<EntitySelectInteractionEvent> {

    @Override
    public boolean onEntitySelectMenu(EntitySelectInteractionEvent event, EntityManagerWrapper entityManager) {
        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        return handleInteraction(event, OnEntitySelectMenuListener.class,
                listener -> ((OnEntitySelectMenuListener) listener.getCommand()).processEntitySelectMenu(event, guildEntity)
        );
    }

}
