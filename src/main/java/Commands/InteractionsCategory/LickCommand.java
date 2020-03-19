package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import Commands.InteractionAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "lick",
        emoji = "\uD83D\uDE0B",
        executable = false
)
public class LickCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/fc0ef2ba03d82af0cbd6c5815c3c83d5/tenor.gif?itemid=12141725",
                "https://media1.tenor.com/images/ec2ca0bf12d7b1a30fea702b59e5a7fa/tenor.gif?itemid=13417195",
                "https://media1.tenor.com/images/1a2d051f28155db0e4cf175d987cdac2/tenor.gif?itemid=12141721",
                "https://media1.tenor.com/images/42a2aa3a3bee0c5dfe53e51cd8e4fe0d/tenor.gif?itemid=10575259",
                "https://media1.tenor.com/images/5f73f2a7b302a3800b3613095f8a5c40/tenor.gif?itemid=10005495",
                "https://media1.tenor.com/images/df6d1b4922c131f1d191f022a3dbaf67/tenor.gif?itemid=11357830",
                "https://media1.tenor.com/images/c4f68fbbec3c96193386e5fcc5429b89/tenor.gif?itemid=13451325",
                "https://media1.tenor.com/images/359d9a5038eb688e9d5b25eead83ad3e/tenor.gif?itemid=4854805",
                "https://media1.tenor.com/images/f46762ad38fbfed9e4e46bf7b89497c2/tenor.gif?itemid=12141724",
                "https://media1.tenor.com/images/2c15d00633af18a31a1d45aeb6e7ae0d/tenor.gif?itemid=9152683",
                "https://media1.tenor.com/images/f0a7f04a7bc32029cc1273d06b93237f/tenor.gif?itemid=13451464",
                "https://media1.tenor.com/images/7132e6f39a0e4ada4e33d71056bcde67/tenor.gif?itemid=12858455",
                "https://media1.tenor.com/images/6b701503b0e5ea725b0b3fdf6824d390/tenor.gif?itemid=12141727",
                "https://media1.tenor.com/images/244251eb768432357c47f03972b955ad/tenor.gif?itemid=12174884",
                "https://media1.tenor.com/images/1063e876a461f4be347b496a9ecd271c/tenor.gif?itemid=9340107",
                "https://media1.tenor.com/images/f3e4ddfca159dbca52c37ff48b7dd95a/tenor.gif?itemid=8162118",
                "https://media1.tenor.com/images/fc0ef2ba03d82af0cbd6c5815c3c83d5/tenor.gif?itemid=12141725",
                "https://media1.tenor.com/images/0ce34500facf2ada86307bb740a03dfd/tenor.gif?itemid=5567738",
                "https://media1.tenor.com/images/c5f0ef91102c12203dd351841cfd2d73/tenor.gif?itemid=13451452",
                "https://media1.tenor.com/images/d702fa41028207c6523b831ec2db9467/tenor.gif?itemid=5990650",
                "https://media1.tenor.com/images/5785566574fe6293e3be673e85d4894b/tenor.gif?itemid=5649365",
                "https://media1.tenor.com/images/fb5e394d76ec3b91f8482177fc4203ad/tenor.gif?itemid=9803097",
                "https://media1.tenor.com/images/feeef4685f9307b76c78a22ba0a69f48/tenor.gif?itemid=8413059",
                "https://media1.tenor.com/images/efd46743771a78e493e66b5d26cd2af1/tenor.gif?itemid=14002773",
                "https://media1.tenor.com/images/0d5c3b71635cad214699cab7d6250644/tenor.gif?itemid=7272611",
                "https://media1.tenor.com/images/a90264f34cc1d91775fb96cbca280062/tenor.gif?itemid=5070299",
                "https://cdn.discordapp.com/attachments/499629904380297226/613075980243697808/lick_yarichin.gif"
        };
    }

}
