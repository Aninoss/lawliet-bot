package Commands.Interactions;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

public class EveryoneCommand extends InteractionCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public EveryoneCommand() {
        super();
        trigger = "everyone";
        emoji = "\uD83D\uDE21";
        nsfw = false;
        gifs = new String[]{
                "https://cdn.discordapp.com/attachments/499640076150636555/499654979452272670/5.jpg",
                "https://cdn.discordapp.com/attachments/499640076150636555/499654979871703051/6.jpg",
                "https://cdn.discordapp.com/attachments/499640076150636555/499654983696777216/7.jpg",
                "https://cdn.discordapp.com/attachments/499640076150636555/499654984292630530/8.jpg",
                "https://cdn.discordapp.com/attachments/499640076150636555/499654991552839690/0.jpg",
                "https://cdn.discordapp.com/attachments/499640076150636555/499654994719408183/10.gif",
                "https://cdn.discordapp.com/attachments/499640076150636555/499654996388741121/1.jpg",
                "https://cdn.discordapp.com/attachments/499640076150636555/499654997060091915/2.jpg",
                "https://cdn.discordapp.com/attachments/499640076150636555/499654993691803651/9.gif",
                "https://cdn.discordapp.com/attachments/499640076150636555/499655000402690049/3.jpg",
                "https://cdn.discordapp.com/attachments/499640076150636555/499655248395239475/4.jpg"
        };
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
