package core.buttons;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.internal.requests.Route;

public class MessageEditActionAdvanced extends MessageActionAdvanced {

    public MessageEditActionAdvanced(Message message) {
        super(
                message.getChannel(),
                Route.Messages.EDIT_MESSAGE.compile(message.getChannel().getId(), message.getId())
        );
    }

    public MessageEditActionAdvanced(MessageChannel channel, long messageId) {
        super(
                channel,
                Route.Messages.EDIT_MESSAGE.compile(channel.getId(), String.valueOf(messageId))
        );
    }

}
