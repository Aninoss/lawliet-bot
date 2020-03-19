package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import Commands.InteractionAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "notwork",
        emoji = "❌",
        executable = false
)
public class NotWorkCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://cdn.discordapp.com/attachments/499629904380297226/621051161952256050/notwork.png",
                "https://media1.tenor.com/images/471e6c2ca597921c3f26ea7713555feb/tenor.gif?itemid=11299432"
        };
    }

}
