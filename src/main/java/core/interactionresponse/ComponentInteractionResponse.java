package core.interactionresponse;

import java.util.Collection;
import java.util.List;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.requests.RestAction;

public class ComponentInteractionResponse extends InteractionResponse {

    private final GenericComponentInteractionCreateEvent event;

    public ComponentInteractionResponse(GenericComponentInteractionCreateEvent event) {
        this.event = event;
    }

    @Override
    public RestAction<Message> editMessageEmbeds(List<MessageEmbed> embeds, Collection<ActionRow> actionRows) {
        if (!event.isAcknowledged()) {
            Message message = event.getMessage();
            return event.editMessageEmbeds(embeds)
                    .setComponents(actionRows)
                    .map(h -> message);
        } else {
            return event.getHook().editOriginalEmbeds(embeds)
                    .setComponents(actionRows);
        }
    }

    @Override
    public RestAction<Message> replyEmbeds(List<MessageEmbed> embeds, Collection<ActionRow> actionRows, boolean ephemeral) {
        if (!event.isAcknowledged()) {
            return event.replyEmbeds(embeds)
                    .setComponents(actionRows)
                    .setEphemeral(ephemeral)
                    .flatMap(InteractionHook::retrieveOriginal);
        } else {
            return event.getHook().sendMessageEmbeds(embeds)
                    .setComponents(actionRows)
                    .setEphemeral(ephemeral);
        }
    }

    @Override
    public boolean isValid() {
        return !event.getHook().isExpired();
    }

    @Override
    public void complete() {
        if (isValid() && !event.isAcknowledged()) {
            event.deferEdit().queue();
        }
    }

}
