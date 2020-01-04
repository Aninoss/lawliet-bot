package Commands.Emotes;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
    trigger = "sleep",
    emoji = "\uD83D\uDCA4",
    executable = true
)
public class SleepCommand extends EmoteCommand implements onRecievedListener {

    private static ArrayList<Integer> picked = new ArrayList<>();

    public SleepCommand() {
        super("https://media1.tenor.com/images/536666c6ed48d260e68ae067a5e7129c/tenor.gif?itemid=13576257",
                "https://media1.tenor.com/images/62299afcedd465b631f9baa9786bd83b/tenor.gif?itemid=6238156",
                "https://media1.tenor.com/images/bad0e118dc9e1f66f8baf9291112c989/tenor.gif?itemid=12048173",
                "https://media1.tenor.com/images/078c70e3ed147f6f004773b6640936d7/tenor.gif?itemid=5810138",
                "https://media1.tenor.com/images/1cdece239ec7d0fb33d2976d623f5e77/tenor.gif?itemid=4718185",
                "https://media1.tenor.com/images/81b39f20f9369290a0f3c8148427480e/tenor.gif?itemid=5469651",
                "https://media1.tenor.com/images/a7e8e8f9fd0a8784012d8f14b09da4a8/tenor.gif?itemid=12048209",
                "https://media1.tenor.com/images/bc308ef7ed3753ae73f1ff047e14c554/tenor.gif?itemid=9920974",
                "https://media1.tenor.com/images/e35738edcf70fa24976fa862d0a9e32a/tenor.gif?itemid=9096283",
                "https://media1.tenor.com/images/c08efe7356f36e19ee3e2489c10d31f3/tenor.gif?itemid=10173924",
                "https://media1.tenor.com/images/75fe39865ba1c713d740bbe65776c2c7/tenor.gif?itemid=4605306",
                "https://media1.tenor.com/images/ce1290d1994e2da8407d53903809d774/tenor.gif?itemid=5209581",
                "https://media1.tenor.com/images/3102d607024c60db8592dfe41af2004a/tenor.gif?itemid=5634618",
                "https://media1.tenor.com/images/5ec9e445aeb6fe23b5f6df7a4b837874/tenor.gif?itemid=9188482",
                "https://media1.tenor.com/images/83f8887cbc91300c97bc16a19e470230/tenor.gif?itemid=13878986",
                "https://media1.tenor.com/images/5a519ab7fbf494265b7ba09de84b05aa/tenor.gif?itemid=12069369",
                "https://media1.tenor.com/images/f56b79e789708b92fe52041e8696a512/tenor.gif?itemid=9412949",
                "https://media1.tenor.com/images/a570ecdd31426d45bd0f9f1b4e97fc82/tenor.gif?itemid=11399964",
                "https://media1.tenor.com/images/8656d05cbb568591b1423793ce4bc1f0/tenor.gif?itemid=5198795",
                "https://media1.tenor.com/images/17b6c2bfa7619822ad55e39efa1a30b1/tenor.gif?itemid=13436979",
                "https://media1.tenor.com/images/340108a4ac709fa76a93a148d3042f2b/tenor.gif?itemid=4671188",
                "https://media1.tenor.com/images/4c06772922fd591f0c4cd077b09de541/tenor.gif?itemid=5079238",
                "https://media1.tenor.com/images/ea686c5a0097992a1557792f5362c7e9/tenor.gif?itemid=5303823",
                "https://media1.tenor.com/images/f4728565a33ad7b459ea2ca43faffb45/tenor.gif?itemid=15779007",
                "https://media1.tenor.com/images/766a25de69e36c91d06726ba3113b234/tenor.gif?itemid=3468672",
                "https://media1.tenor.com/images/182e0fed20b37178fddc75471b4d944e/tenor.gif?itemid=15150330",
                "https://media1.tenor.com/images/563763c318916e8fe915eca62d164903/tenor.gif?itemid=9249213",
                "https://media1.tenor.com/images/d38b83768e108889e7e0dadfc92d4070/tenor.gif?itemid=15077633"
                );
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onEmoteRecieved(event, picked);
    }
}
