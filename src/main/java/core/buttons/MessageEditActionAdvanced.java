package core.buttons;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.internal.requests.Route;

public class MessageEditActionAdvanced extends MessageActionAdvanced {

    public MessageEditActionAdvanced(MessageChannel channel, long messageId) {
        super(
                channel,
                Route.Messages.EDIT_MESSAGE.compile(channel.getId(), String.valueOf(messageId))
        );
    }

}
