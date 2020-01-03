package Commands.Interactions;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "yurifuck",
        emoji = "\uD83D\uDC69\uD83D\uDECF\uD83D\uDC69️",
        executable = false,
        nsfw = true
)
public class YuriFuckCommand extends InteractionCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public YuriFuckCommand() {
        super("https://img2.gelbooru.com//images/2d/43/2d43444830e02e7e5f0c7e822516d952.gif",
                "https://img2.gelbooru.com/images/95/39/9539017a59cb9baa98e2fe941deddfab.gif",
                "https://img2.gelbooru.com//images/67/d5/67d55a9eabbc0a3cc2fd264ad8710de2.gif",
                "https://img2.gelbooru.com//images/53/7d/537d2f90de32be66f525a16ff32d213f.gif",
                "https://img2.gelbooru.com//images/7b/c1/7bc11ab89be09830a7838fe0da8619aa.gif",
                "https://img2.gelbooru.com//images/b6/9d/b69dbc34d3ee4006223a28e226065df9.gif",
                "https://img2.gelbooru.com//images/bf/af/bfafc32fc4593b0e085eca3c978ae67c.gif",
                "https://img2.gelbooru.com//images/ab/b7/abb787211c8a98e1f489d1edd5f6c20b.gif",
                "https://img2.gelbooru.com//images/cc/17/cc177210e4a689880b285f04982ae36c.gif",
                "https://img2.gelbooru.com//images/d4/34/d43453fbc93edbe4af06f39da0826828.gif",
                "https://img2.gelbooru.com//images/b6/79/b6796a171722dc73202d76b5d9bdc13e.gif",
                "https://img2.gelbooru.com//images/19/5f/195f2743f64b52d4d3fd04793d45ff1d.gif",
                "https://img2.gelbooru.com//images/08/36/083662c830ef43f1c7cb6ec5fa4085a2.gif",
                "https://img2.gelbooru.com//images/fb/36/fb36754ff14018f6543b2589d1741a92.gif",
                "https://img2.gelbooru.com//images/1f/3b/1f3be63a2f7fcd3345c59e9e667dce1e.gif",
                "https://img2.gelbooru.com//images/e7/88/e7880b46353ffefe6d7d856e0a535d56.gif",
                "https://img2.gelbooru.com//images/19/3d/193d7c0aea6862f1dacd9f9f8ef21f9b.gif",
                "https://img2.gelbooru.com//images/a6/d2/a6d217f4135b4e3d5ba6b736bc4732fa.gif",
                "https://img2.gelbooru.com//images/a3/24/a3242219e65d6b595b49a6eaa5cc6d85.gif",
                "https://img2.gelbooru.com//images/87/cf/87cf7975b8f7597c1c3f4007fdad7428.gif",
                "https://img2.gelbooru.com//images/fa/a2/faa22eab7a8de3ec59c7d99dc0f0ff55.gif",
                "https://img2.gelbooru.com//images/f7/f5/f7f5ae25bec9d2606c24cf0281e9dcb0.gif",
                "https://img2.gelbooru.com//images/21/f1/21f106a01b29d187dcfd622103296330.gif"
        );
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
