package core;

import java.util.Collection;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.requests.RestAction;

public class InteractionResponse {

    private ButtonClickEvent event;
    private InteractionHook hook = null;

    public InteractionResponse(ButtonClickEvent buttonClickEvent) {
        this.event = buttonClickEvent;
    }

    public void deferEdit() {
        if (event != null) {
            event.deferEdit().queue();
            hook = event.getHook();
            event = null;
        }
    }

    public RestAction<Message> editMessageEmbeds(MessageEmbed embed, Collection<ActionRow> actionRows) {
        if (event != null) {
            hook = event.getHook();
            Message message = event.getMessage();
            RestAction<Message> action = event.editMessageEmbeds(embed)
                    .setActionRows(actionRows)
                    .map(h -> message);
            event = null;
            return action;
        } else {
            return hook.editOriginalEmbeds(embed)
                    .setActionRows(actionRows);
        }
    }

    public RestAction<Message> replyEmbeds(MessageEmbed embed, Collection<ActionRow> actionRows, boolean ephemeral) {
        if (event != null) {
            hook = event.getHook();
            Message message = event.getMessage();
            RestAction<Message> action = event.replyEmbeds(embed)
                    .addActionRows(actionRows)
                    .setEphemeral(ephemeral)
                    .map(h -> message);
            event = null;
            return action;
        } else {
            return hook.sendMessageEmbeds(embed)
                    .addActionRows(actionRows)
                    .setEphemeral(ephemeral);
        }
    }

    public void complete() {
        if (event != null) {
            event.deferEdit().queue();
        }
    }

}
