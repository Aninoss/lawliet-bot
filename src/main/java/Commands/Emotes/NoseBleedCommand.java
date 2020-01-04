package Commands.Emotes;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
    trigger = "nosebleed",
    emoji = "\uD83E\uDE78",
    executable = true
)
public class NoseBleedCommand extends EmoteCommand implements onRecievedListener {

    private static ArrayList<Integer> picked = new ArrayList<>();

    public NoseBleedCommand() {
        super("https://media1.tenor.com/images/9dbd860f873bc8d55770195cd31124a6/tenor.gif?itemid=9411114",
                "https://media1.tenor.com/images/385c7b4211ce6b251ef67e6f1737050c/tenor.gif?itemid=8882037",
                "https://media1.tenor.com/images/74c9206e868c8200bad44a97bc91525a/tenor.gif?itemid=5551238",
                "https://media1.tenor.com/images/e9530214c9aa36ec7a478574206efce7/tenor.gif?itemid=12362785",
                "https://media1.tenor.com/images/0eca82142481add1ddc8d8b031b91d23/tenor.gif?itemid=5469034",
                "https://media1.tenor.com/images/0d72a7278618f03f91c3f0ff58862b5a/tenor.gif?itemid=13804817",
                "https://media1.tenor.com/images/c8c0e22a8285999033c9d8f67162782c/tenor.gif?itemid=5585187",
                "https://media1.tenor.com/images/466415bafcbe015a035417f8c42d1d4e/tenor.gif?itemid=7294363",
                "https://media1.tenor.com/images/905612dcbdc68c8197663bc5b34089ac/tenor.gif?itemid=9839349",
                "https://media1.tenor.com/images/9d4d11f65f9faa9ee18ad361aec03adb/tenor.gif?itemid=5939995",
                "https://media1.tenor.com/images/2be7ccec26bac409e52ecc7cc27aaa4f/tenor.gif?itemid=5013979",
                "https://media1.tenor.com/images/d7d2ec7f66bafef5641bd748303cf0b3/tenor.gif?itemid=11481280",
                "https://media1.tenor.com/images/eb250a81c6b4fc6c424de058b2428e5a/tenor.gif?itemid=14725876"
                );
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onEmoteRecieved(event, picked);
    }
}
