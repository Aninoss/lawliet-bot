package Commands.Interactions;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

public class RewardCommand extends InteractionCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public RewardCommand() {
        super();
        trigger = "reward";
        emoji = "\uD83C\uDF53";
        nsfw = false;
        gifs = new String[]{
                "https://cdn.discordapp.com/attachments/499629904380297226/499653477568348163/reward.gif"
        };
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
