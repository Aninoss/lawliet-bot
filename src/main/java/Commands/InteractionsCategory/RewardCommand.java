package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import Commands.InteractionAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "reward",
        emoji = "\uD83C\uDF53",
        executable = false
)
public class RewardCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://cdn.discordapp.com/attachments/499629904380297226/499653477568348163/reward.gif"};
    }

}
