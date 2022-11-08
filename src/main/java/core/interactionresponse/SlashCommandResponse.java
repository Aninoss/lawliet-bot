package core.interactionresponse;

import java.util.Collection;
import java.util.List;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.requests.RestAction;

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
    public boolean isValid() {
        return !interactionHook.isExpired();
    }

    @Override
    public void complete() {
    }

}
