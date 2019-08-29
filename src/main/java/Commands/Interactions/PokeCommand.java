package Commands.Interactions;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

public class PokeCommand extends InteractionCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public PokeCommand() {
        super();
        trigger = "poke";
        emoji = "\uD83D\uDC49";
        nsfw = false;
        gifs = new String[]{
                "https://media1.tenor.com/images/48086974f33a3e0114b2e0387f812ae4/tenor.gif?itemid=12360399",
                "https://media1.tenor.com/images/2b55eb1befce3e843dec7e8feebf274b/tenor.gif?itemid=10168199",
                "https://media1.tenor.com/images/ab936c887562756472f83850426bf6ef/tenor.gif?itemid=11956062",
                "https://media1.tenor.com/images/e8b25e7d069c203ea7f01989f2a0af59/tenor.gif?itemid=12011027",
                "https://media1.tenor.com/images/1e0ea8b241a7db2b9c03775133138733/tenor.gif?itemid=10064326",
                "https://media1.tenor.com/images/d2b08ce502740221b978d8e5e876b6e2/tenor.gif?itemid=12872012",
                "https://media1.tenor.com/images/8d82ec69b004ac909881420724d6daa7/tenor.gif?itemid=11823786",
                "https://media1.tenor.com/images/2b55eb1befce3e843dec7e8feebf274b/tenor.gif?itemid=10168199",
                "https://media1.tenor.com/images/514efe749cb611eb382713596e3427d8/tenor.gif?itemid=13054528",
                "https://media1.tenor.com/images/fd46d903c4a20a7e82519a78f15b2750/tenor.gif?itemid=8562185",
                "https://media1.tenor.com/images/1a64ac660387543c5b779ba1d7da2c9e/tenor.gif?itemid=12396068",
                "https://media1.tenor.com/images/faa16497a7cd01dc7c947225b27062f5/tenor.gif?itemid=12583170",
                "https://media1.tenor.com/images/1236e0d70c6ee3ea91d414bcaf9f3aa4/tenor.gif?itemid=5015314",
                "https://media1.tenor.com/images/573002c649f529f0141f07c740df54ea/tenor.gif?itemid=10271400",
                "https://media1.tenor.com/images/cbf38a2e97a348a621207c967a77628a/tenor.gif?itemid=6287077",
                "https://media1.tenor.com/images/4a19444c15196e240067e8498c5bf5d9/tenor.gif?itemid=7537947",
                "https://media1.tenor.com/images/8fe23ec8e2c5e44964e5c11983ff6f41/tenor.gif?itemid=5600215",
                "https://media1.tenor.com/images/4f886a9db21b5398f2ad91178887ed4d/tenor.gif?itemid=12583168",
                "https://media1.tenor.com/images/90f68d48795c51222c60afc7239c930c/tenor.gif?itemid=8701034",
                "https://media1.tenor.com/images/3b9cffb5b30236f678fdccf442006a43/tenor.gif?itemid=7739077",
                "https://media1.tenor.com/images/175cc4686c4c67809f48eef44965c845/tenor.gif?itemid=10217135",
                "https://media1.tenor.com/images/3b2bfd09965bd77f2a8cb9ba59cedbe4/tenor.gif?itemid=5607667",
                "https://media1.tenor.com/images/decc2c2f705b74556142d4b746c2dc97/tenor.gif?itemid=12016340",
                "https://media1.tenor.com/images/effab12abbd4bcddc04ee6a72007b1d1/tenor.gif?itemid=12286562",
                "https://media1.tenor.com/images/3b5eda57ba5cd315234af9367f4a4248/tenor.gif?itemid=12411431"
        };
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
