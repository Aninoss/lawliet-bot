package core.interactionresponse;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.requests.RestAction;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class InteractionResponse {

    public RestAction<Message> editMessageEmbeds(List<MessageEmbed> embeds) {
        return editMessageEmbeds(embeds, Collections.emptyList());
    }

    public abstract RestAction<Message> editMessageEmbeds(List<MessageEmbed> embeds, Collection<ActionRow> actionRows);

    public RestAction<Message> replyEmbeds(List<MessageEmbed> embeds, boolean ephemeral) {
        return replyEmbeds(embeds, Collections.emptyList(), ephemeral);
    }

    public abstract RestAction<Message> replyEmbeds(List<MessageEmbed> embeds, Collection<ActionRow> actionRows, boolean ephemeral);

    public abstract RestAction<Message> editMessageComponents(MessageComponentTree componentTree);

    public abstract RestAction<Message> replyComponents(MessageComponentTree componentTree, boolean ephemeral);

    public abstract boolean isValid();

    public abstract void complete();

}
