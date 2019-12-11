package Commands.Interactions;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "cuddle",
        emoji = "\uD83D\uDC50",
        executable = false,
        aliases = {"snuggle"}
)
public class CuddleCommand extends InteractionCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public CuddleCommand() {
        super("https://media1.tenor.com/images/d16a9affe8915e6413b0c1f1d380b2ee/tenor.gif?itemid=12669052",
                "https://media1.tenor.com/images/012cc6d6cb65c3c98bd5505ab2e1c42a/tenor.gif?itemid=13317505",
                "https://media1.tenor.com/images/aa04a0093e2ef93922d3d88e12b70561/tenor.gif?itemid=12887276",
                "https://media1.tenor.com/images/4a211d5c5d076ad8795d8a82f9f01c29/tenor.gif?itemid=13221038",
                "https://media1.tenor.com/images/cfbde89890b97a0ddbac52bfe3e46fbc/tenor.gif?itemid=7936816",
                "https://media1.tenor.com/images/ec938c17b78033bf368cacea844d03af/tenor.gif?itemid=7250422",
                "https://media1.tenor.com/images/0cc4ed655f373d5ae0c55f4ca279fa0f/tenor.gif?itemid=13240992",
                "https://media1.tenor.com/images/adeb030aaa5a2a3d16abdc58be4d1448/tenor.gif?itemid=11733535",
                "https://media1.tenor.com/images/6499ca6129bde06a8aa35c3b4e3ab0a1/tenor.gif?itemid=5683511",
                "https://media1.tenor.com/images/0aced4890b86b4f78206dd7fa16dc198/tenor.gif?itemid=5404693",
                "https://media1.tenor.com/images/8e76ba7908efee7489aa53f349bb5b0d/tenor.gif?itemid=12669027",
                "https://media1.tenor.com/images/d0c2e7382742f1faf8fcb44db268615f/tenor.gif?itemid=5853736",
                "https://media1.tenor.com/images/13be52a4a4a26b0c9e479df6644d6de5/tenor.gif?itemid=12668752",
                "https://media1.tenor.com/images/6e1f8079fe446e8bc245c25d7dae91a7/tenor.gif?itemid=12806537",
                "https://media1.tenor.com/images/4710c7b3463f914432ff43aad0e48bf5/tenor.gif?itemid=9124287",
                "https://media1.tenor.com/images/b498f9fbbb0d7f1523c2ec1684994304/tenor.gif?itemid=12669026",
                "https://media1.tenor.com/images/1a65319302b9e1c86a99a39e9a81084e/tenor.gif?itemid=3553099",
                "https://media1.tenor.com/images/3b205574d0352d4d61687f835276566d/tenor.gif?itemid=12669039",
                "https://media1.tenor.com/images/0f0637c4fabb1baff48a88f35bab4eee/tenor.gif?itemid=12252532",
                "https://media1.tenor.com/images/38ac528295b255d0a92ae883b86e8750/tenor.gif?itemid=9186238",
                "https://media1.tenor.com/images/50e1eb3f727a2cf0598eaaf3c1fc46f3/tenor.gif?itemid=12668887",
                "https://media1.tenor.com/images/8042046da3a26c4d3cedb5ce06cdeb14/tenor.gif?itemid=4901574",
                "https://media1.tenor.com/images/965914ebcc390748b4517a1aec7bf8b4/tenor.gif?itemid=5514637",
                "https://media1.tenor.com/images/37ac3414835b0cce1304b6a4b5fcaddd/tenor.gif?itemid=12669038",
                "https://media1.tenor.com/images/a3862661163e420d57538b1b08aa5972/tenor.gif?itemid=12669020",
                "https://media1.tenor.com/images/5dbb6d29ac9f63d7815a95997ecbae56/tenor.gif?itemid=13356108",
                "https://media1.tenor.com/images/2ef3e594cc380567b53ae2f670c54ef9/tenor.gif?itemid=7287693",
                "https://media1.tenor.com/images/58cd629d688d826cf3fb39e949637169/tenor.gif?itemid=7250489",
                "https://media1.tenor.com/images/cc805107341e281102a2280f08b582e0/tenor.gif?itemid=13925386",
                "https://media1.tenor.com/images/778d789b9c6e8e624c8650f1b988204f/tenor.gif?itemid=14839882"
        );
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
