package core;

import java.util.Collection;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.requests.RestAction;

public class InteractionResponse {

    private final ButtonClickEvent event;

    public InteractionResponse(ButtonClickEvent buttonClickEvent) {
        this.event = buttonClickEvent;
    }

    public RestAction<Message> editMessageEmbeds(MessageEmbed embed, Collection<ActionRow> actionRows) {
        if (!event.isAcknowledged()) {
            Message message = event.getMessage();
            return event.editMessageEmbeds(embed)
                    .setActionRows(actionRows)
                    .map(h -> message);
        } else {
            return event.getHook().editOriginalEmbeds(embed)
                    .setActionRows(actionRows);
        }
    }

    public RestAction<Message> replyEmbeds(MessageEmbed embed, Collection<ActionRow> actionRows, boolean ephemeral) {
        if (!event.isAcknowledged()) {
            Message message = event.getMessage();
            return event.replyEmbeds(embed)
                    .addActionRows(actionRows)
                    .setEphemeral(ephemeral)
                    .map(h -> message);
        } else {
            return event.getHook().sendMessageEmbeds(embed)
                    .addActionRows(actionRows)
                    .setEphemeral(ephemeral);
        }
    }

    public void complete() {
        if (!event.isAcknowledged()) {
            event.deferEdit().queue();
        }
    }

}
