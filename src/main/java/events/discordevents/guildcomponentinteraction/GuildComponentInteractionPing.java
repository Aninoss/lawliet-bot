package events.discordevents.guildcomponentinteraction;

import core.buttons.GuildComponentInteractionEvent;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildComponentInteractionAbstract;

@DiscordEvent
public class GuildComponentInteractionPing extends GuildComponentInteractionAbstract {

    @Override
    public boolean onGuildComponentInteraction(GuildComponentInteractionEvent event) {
        System.out.println("PING");
        return true;
    }

}
