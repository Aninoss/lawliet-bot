package Commands.Emotes;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "no",
        emoji = "\uD83D\uDC4E",
        executable = true,
        aliases = {"nope"}
)
public class NoCommand extends EmoteCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public NoCommand() {
        super("https://media1.tenor.com/images/90ea198565528e21b8ec47cdae286395/tenor.gif?itemid=5552213",
                "https://media1.tenor.com/images/2cf373aef8fedfa21cc1f5587a6f9e2b/tenor.gif?itemid=8620719",
                "https://media1.tenor.com/images/294f0756c916983835e855df0cc4a6e6/tenor.gif?itemid=5799534",
                "https://media1.tenor.com/images/001d74c97a8c36cbaa203540b71e8745/tenor.gif?itemid=12199347",
                "https://media1.tenor.com/images/593fff0d8dff90a5f55d7c35f0c603e8/tenor.gif?itemid=4955442",
                "https://media1.tenor.com/images/925e2399f90f9414c8d2912fa19fc3a6/tenor.gif?itemid=10069655",
                "https://media1.tenor.com/images/548485d57335896ec0250bbcb2912a59/tenor.gif?itemid=13379237",
                "https://media1.tenor.com/images/8d2d5bd6b802ea81e87cc8deb3745a4c/tenor.gif?itemid=5102670",
                "https://media1.tenor.com/images/f2aa55cff42f1d5f79a9fb9b3ec11d9b/tenor.gif?itemid=8991384",
                "https://media1.tenor.com/images/0b25ec49614ec04b3d51a030a09b7dc7/tenor.gif?itemid=5703082",
                "https://media1.tenor.com/images/514aeb99aa9e9fb8fafd91df1376cb37/tenor.gif?itemid=4696636",
                "https://media1.tenor.com/images/1d24cb38f6fd5d1880d3a9ff7c5ee925/tenor.gif?itemid=9852449",
                "https://media1.tenor.com/images/3f10d4ba54de4302c1ea37ce0b272d40/tenor.gif?itemid=8620723",
                "https://media1.tenor.com/images/782aa1a037423aa86729353fe0296ca1/tenor.gif?itemid=9514682",
                "https://media1.tenor.com/images/f5dd17542f68803656f52dfce6cab8c1/tenor.gif?itemid=14132798",
                "https://media1.tenor.com/images/b9c243d81cb40b09b38634727b5fb7a5/tenor.gif?itemid=14119537",
                "https://media1.tenor.com/images/5768f9f99be830649a85dacd2a5809a3/tenor.gif?itemid=9423903",
                "https://media1.tenor.com/images/01798bf7711b3eaa726fc7708f76ddd1/tenor.gif?itemid=9320140",
                "https://media1.tenor.com/images/b13fb0dbb71ba21d0c1c8bea1d449340/tenor.gif?itemid=12252534"
        );
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onEmoteRecieved(event, picked);
    }
}
