package Commands.Emotes;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
    trigger = "awkward",
    emoji = "\uD83D\uDE05",
    executable = true
)
public class AwkwardCommand extends EmoteCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public AwkwardCommand() {
        super("https://media1.tenor.com/images/a88493abc7f9f8e296c6fd3dd8d1fbe9/tenor.gif?itemid=10517455",
                "https://media1.tenor.com/images/8271fe0d4da44d3f01266e32cb22dc95/tenor.gif?itemid=10880412",
                "https://media1.tenor.com/images/d59b4de25cd8cf2833bfa55f90137c7e/tenor.gif?itemid=9474338",
                "https://media1.tenor.com/images/11758e6b97e1e1d21420646f91efb07f/tenor.gif?itemid=11624562",
                "https://media1.tenor.com/images/6537bbbf7cc5d6b33295abedb9191977/tenor.gif?itemid=14859821",
                "https://media1.tenor.com/images/6c4b324c0708f259c1fee5f1e40cc492/tenor.gif?itemid=12546889",
                "https://media1.tenor.com/images/f64193576fa4d02d893f923ad547fde0/tenor.gif?itemid=14740117",
                "https://media1.tenor.com/images/74ade8c3248788749f0de44c3edfe319/tenor.gif?itemid=14751743",
                "https://media1.tenor.com/images/de267d5f34e3f9d05e3c15a64a1bd162/tenor.gif?itemid=14080530",
                "https://media1.tenor.com/images/130827fc81cd9688a8d7a25a6577c9fa/tenor.gif?itemid=10268864",
                "https://media1.tenor.com/images/96378f911d4057b56a947db06b7eec2d/tenor.gif?itemid=9685848",
                "https://media1.tenor.com/images/c1461ba849fd9cff5b346f2433f8e8c1/tenor.gif?itemid=12668868",
                "https://media1.tenor.com/images/c33e7d0f9f639c6faef9e902c81a8d5a/tenor.gif?itemid=10067913",
                "https://media1.tenor.com/images/f27e79bd11647307420417ce70b552f3/tenor.gif?itemid=14865625",
                "https://media1.tenor.com/images/cb427167533b1ad95616ad7020c098af/tenor.gif?itemid=5716283",
                "https://media1.tenor.com/images/acf3eee4dcf9b8bd0b061db070f9dfe9/tenor.gif?itemid=14207288",
                "https://media1.tenor.com/images/8edc623e599c969359e4a5b5fb4c30f0/tenor.gif?itemid=12360867"
        );
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onEmoteRecieved(event, picked);
    }
}
