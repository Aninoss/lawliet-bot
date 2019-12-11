package Commands.Emotes;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "dance",
        emoji = "\uD83D\uDD7A",
        executable = true,
        aliases = {"party"}
)
public class DanceCommand extends EmoteCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public DanceCommand() {
        super("https://media1.tenor.com/images/9841990160f71767843af6cf08b5502d/tenor.gif?itemid=9251559",
                "https://media1.tenor.com/images/81df5e907f81dad1721f398ed7408deb/tenor.gif?itemid=7560548",
                "https://media1.tenor.com/images/0775ee877645d4a35cf219d83854fb9c/tenor.gif?itemid=7880641",
                "https://media1.tenor.com/images/b99e68bc2216552709e0e82d938f1348/tenor.gif?itemid=10260187",
                "https://media1.tenor.com/images/56350dfdcd3a5fa4fd66e9e87f9574bb/tenor.gif?itemid=4718162",
                "https://media1.tenor.com/images/766599022416cc0b7b7b1bd2040eb2db/tenor.gif?itemid=12039886",
                "https://media1.tenor.com/images/d85d9011c6c866057ca2e6780c6fedd8/tenor.gif?itemid=13266349",
                "https://media1.tenor.com/images/b89b469e2975bcfa6c0f6cf0c34afa67/tenor.gif?itemid=5115426",
                "https://media1.tenor.com/images/788f9d274e39299eb93698ee802ed56b/tenor.gif?itemid=14040294",
                "https://media1.tenor.com/images/21e860a31f32d5e3e6bdf2963cadfebf/tenor.gif?itemid=5897404",
                "https://media1.tenor.com/images/4ea6b5a95320003a13c03f09351abca0/tenor.gif?itemid=7275499",
                "https://media1.tenor.com/images/94e0a12faac0821738185def2d96a808/tenor.gif?itemid=7797151",
                "https://media1.tenor.com/images/c6fa55f4773f9c77251dbca954e79764/tenor.gif?itemid=4704136",
                "https://media1.tenor.com/images/a65229f878fad4e4973c9e8caf12bca9/tenor.gif?itemid=4810806",
                "https://media1.tenor.com/images/46605585d5fd5071e61e084005320178/tenor.gif?itemid=9523000",
                "https://media1.tenor.com/images/e282b8c13fa3758517ac5eb9c8e3c4f1/tenor.gif?itemid=9060211",
                "https://media1.tenor.com/images/8b47eca66b93d1b29f7df2db3d7eba6b/tenor.gif?itemid=13414394",
                "https://media1.tenor.com/images/d250c06c34f6961087a83c6fd79d0711/tenor.gif?itemid=4718235",
                "https://media1.tenor.com/images/8f668350ed3dca15ad95fcd2ae2d93bd/tenor.gif?itemid=5769476"
        );
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onEmoteRecieved(event, picked);
    }
}
