package Commands.Interactions;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "yiff",
        emoji = "\uD83E\uDD8A",
        executable = false
)
public class YiffCommand extends InteractionCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public YiffCommand() {
        super("https://static1.e621.net/data/24/e1/24e19783d4e24ea5d426ade6fe554ef9.gif",
                "https://static1.e621.net/data/e5/70/e570224a4a09c0d25e06666a64d9d393.gif",
                "https://static1.e621.net/data/68/a2/68a2a4bbfaceabb8e8e3334735aa214b.gif",
                "https://static1.e621.net/data/ff/cc/ffcc2358cb9b770bbb9f9d81a1b682e0.gif",
                "https://static1.e621.net/data/fd/f9/fdf9d2ce1f1e7a2f7e1785e16dce76f4.gif",
                "https://static1.e621.net/data/40/09/400982726dab87310824c0b63be4083f.gif",
                "https://static1.e621.net/data/37/95/3795b45c2f7f060244cf40fc69145954.gif",
                "https://static1.e621.net/data/4a/85/4a85763106add06dda471bf457d6cf27.gif",
                "https://static1.e621.net/data/bb/48/bb484a0e73fcff9373f1cb1484ff75fe.gif",
                "https://static1.e621.net/data/9f/c1/9fc1640ade5c9bb8260281c841fc90a0.gif",
                "https://static1.e621.net/data/27/c6/27c61d68d85ea266439fea6fe99b058a.gif",
                "https://static1.e621.net/data/23/3e/233ec04ae5a62b0ac3bdc2d599ddc198.gif",
                "https://static1.e621.net/data/6b/f3/6bf3f2fc68f4875759b5cbcc376a1603.gif",
                "https://static1.e621.net/data/0a/4c/0a4caa268eee37b14e12ed69b40acc36.gif",
                "https://static1.e621.net/data/c2/6c/c26c1f836ef2f66a5bb8a23d9838c82b.gif",
                "https://static1.e621.net/data/af/f0/aff0e9194acbc64cf225ab4ac0a2692a.gif",
                "https://static1.e621.net/data/06/b8/06b8c71a06d75ef53335b96ad8e59ac8.gif",
                "https://static1.e621.net/data/73/71/737169dbede48be095a14229c3686d31.gif",
                "https://static1.e621.net/data/6b/44/6b44fba8bed61ef6b943e368b1cf0162.gif",
                "https://static1.e621.net/data/e7/8b/e78b1b80314665d22dfad68e390e84ee.gif",
                "https://static1.e621.net/data/da/05/da05e0c4687980126c8d3811d1af8ddc.gif",
                "https://static1.e621.net/data/f8/03/f80364e3e447f3a9de6fca9faadcb0d6.gif",
                "https://static1.e621.net/data/34/58/3458bf0cd510c786f4195ee1e4295201.gif",
                "https://static1.e621.net/data/24/e9/24e9ac9a1ef4b3e8ec4c11d15c1bbaa4.gif",
                "https://static1.e621.net/data/70/84/7084a1b87e31388a5cef3a59c46955b6.gif",
                "https://static1.e621.net/data/e6/a4/e6a4ec25e8c2c57c2b2a70cd8e2b301e.gif",
                "https://static1.e621.net/data/47/7e/477e04522f76a011a05031d81cd4fdb4.gif"
        );
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
