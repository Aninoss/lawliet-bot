package core.interactionresponse;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;

import java.util.Collection;
import java.util.List;

public class SlashCommandResponse extends InteractionResponse {

    private final InteractionHook interactionHook;

    public SlashCommandResponse(InteractionHook interactionHook) {
        this.interactionHook = interactionHook;
    }

    @Override
    public RestAction<Message> editMessageEmbeds(List<MessageEmbed> embeds, Collection<ActionRow> actionRows) {
        return interactionHook.editOriginalEmbeds(embeds)
                .setComponents(actionRows);
    }

    @Override
    public RestAction<Message> replyEmbeds(List<MessageEmbed> embeds, Collection<ActionRow> actionRows, boolean ephemeral) {
        return interactionHook.sendMessageEmbeds(embeds)
                .setComponents(actionRows)
                .setEphemeral(ephemeral);
    }

    @Override
    public RestAction<Message> editMessageComponents(MessageComponentTree componentTree) {
        return interactionHook.editOriginalComponents(componentTree)
                .useComponentsV2();
    }

    @Override
    public RestAction<Message> replyComponents(MessageComponentTree componentTree, boolean ephemeral) {
        return interactionHook.sendMessageComponents(componentTree)
                .useComponentsV2(true)
                .setEphemeral(ephemeral);
    }

    @Override
    public boolean isValid() {
        return !interactionHook.isExpired();
    }

    @Override
    public void complete() {
    }

}
