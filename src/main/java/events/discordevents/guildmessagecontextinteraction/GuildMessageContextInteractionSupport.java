package events.discordevents.guildmessagecontextinteraction;

import constants.AssetIds;
import core.Program;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMessageContextInteractionAbstract;
import modules.SupportTemplates;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;

@DiscordEvent
public class GuildMessageContextInteractionSupport extends GuildMessageContextInteractionAbstract {

    @Override
    public boolean onGuildMessageContextInteraction(MessageContextInteractionEvent event, EntityManagerWrapper entityManager) {
        if (!Program.productionMode() || event.getMember().getRoles().stream().anyMatch(r -> r.getIdLong() == AssetIds.SUPPORT_ROLE_ID)) {
            SupportTemplates.process(event);
        }

        return true;
    }

}
