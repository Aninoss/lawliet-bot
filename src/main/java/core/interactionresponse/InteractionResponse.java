package core.interactionresponse;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.requests.RestAction;

public abstract class InteractionResponse {

    public RestAction<Message> editMessageEmbeds(List<MessageEmbed> embeds) {
        return editMessageEmbeds(embeds, Collections.emptyList());
    }

    public abstract RestAction<Message> editMessageEmbeds(List<MessageEmbed> embeds, Collection<ActionRow> actionRows);

    public RestAction<Message> replyEmbeds(List<MessageEmbed> embeds, boolean ephemeral) {
        return replyEmbeds(embeds, Collections.emptyList(), ephemeral);
    }

    public abstract RestAction<Message> replyEmbeds(List<MessageEmbed> embeds, Collection<ActionRow> actionRows, boolean ephemeral);

    public abstract boolean isValid();

    public abstract void complete();

}
