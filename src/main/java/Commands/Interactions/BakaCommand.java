package Commands.Interactions;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

public class BakaCommand extends InteractionCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public BakaCommand() {
        super();
        trigger = "baka";
        emoji = "\uD83D\uDCA2";
        nsfw = false;
        gifs = new String[]{
                "https://media1.tenor.com/images/a3108070c1fe6b40a99e5add546e3423/tenor.gif?itemid=9441674",
                "https://media1.tenor.com/images/3348f40d0b580479f06a9b08dff247ac/tenor.gif?itemid=12414850",
                "https://media1.tenor.com/images/06218cff28c7e691731bd5ac6284da18/tenor.gif?itemid=13265129",
                "https://media1.tenor.com/images/18774d8725297c4e7a69132a9b959afc/tenor.gif?itemid=13451344",
                "https://media1.tenor.com/images/e32a01457c5db0e9424dea4de4a46585/tenor.gif?itemid=14364932",
                "https://media1.tenor.com/images/88819a7e457c5dc3f124b9d6bb720edc/tenor.gif?itemid=5586968",
                "https://media1.tenor.com/images/d381f1de7b525c2bbd21e803cbd253f4/tenor.gif?itemid=12908346",
                "https://media1.tenor.com/images/56507a7295a5e8fd8ae6ee6f97702f5d/tenor.gif?itemid=13499500",
                "https://media1.tenor.com/images/406ef128433bb6b0d62ba092ed5ec67d/tenor.gif?itemid=14954653",
                "https://media1.tenor.com/images/477821d58203a6786abea01d8cf1030e/tenor.gif?itemid=7958720",
                "https://media1.tenor.com/images/53d180f129f51575a46b6d3f0f5eeeea/tenor.gif?itemid=5373994",
                "https://media1.tenor.com/images/81b64932f764e44c0c2316c68703a46a/tenor.gif?itemid=5897515"
        };
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
