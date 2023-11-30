package events.discordevents.stringselectmenu;

import commands.listeners.OnStringSelectMenuListener;
import events.discordevents.DiscordEvent;
import events.discordevents.InteractionListenerHandler;
import events.discordevents.eventtypeabstracts.StringSelectMenuAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

@DiscordEvent
public class StringSelectMenuCommands extends StringSelectMenuAbstract implements InteractionListenerHandler<StringSelectInteractionEvent> {

    @Override
    public boolean onStringSelectMenu(StringSelectInteractionEvent event, EntityManagerWrapper entityManager) {
        if (!(event.getChannel() instanceof StandardGuildMessageChannel)) {
            return true;
        }

        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        return handleInteraction(event, OnStringSelectMenuListener.class,
                listener -> ((OnStringSelectMenuListener) listener.getCommand()).processStringSelectMenu(event, guildEntity)
        );
    }

}
