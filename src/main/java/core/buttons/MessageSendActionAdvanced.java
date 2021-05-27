package core.buttons;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.internal.requests.Route;

public class MessageSendActionAdvanced extends MessageActionAdvanced {

    public MessageSendActionAdvanced(MessageChannel channel) {
        super(
                channel,
                Route.Messages.SEND_MESSAGE.compile(channel.getId())
        );
    }

}
