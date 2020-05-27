package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;

import Commands.InteractionAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
    trigger = "bite",
    emoji = "\uD83E\uDE78",
    executable = true
)
public class BiteCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/06f88667b86a701b1613bbf5fb9205e9/tenor.gif?itemid=13417199",
                "https://media1.tenor.com/images/3632813a0264ec1fc44525ff86cb1224/tenor.gif?itemid=9060303",
                "https://media1.tenor.com/images/3baeaa0c5ae3a1a4ae9ac2780b2d965d/tenor.gif?itemid=13342683",
                "https://media1.tenor.com/images/6ab39603ef0dd6dbfc78ba20885b991f/tenor.gif?itemid=8220087",
                "https://media1.tenor.com/images/432a41a6beb3c05953c769686e8c4ce9/tenor.gif?itemid=4704665",
                "https://media1.tenor.com/images/6dd67bd831780c4a754cb33697cddcb6/tenor.gif?itemid=10095819",
                "https://media1.tenor.com/images/f308e2fe3f1b3a41754727f8629e5b56/tenor.gif?itemid=12390216",
                "https://media1.tenor.com/images/a74770936aa6f1a766f9879b8bf1ec6b/tenor.gif?itemid=4676912",
                "https://media1.tenor.com/images/418a2765b0bf54eb57bab3fde5d83a05/tenor.gif?itemid=12151511",
                "https://media1.tenor.com/images/ebc0cf14de0e77473a3fc00e60a2a9d3/tenor.gif?itemid=11535890",
                "https://media1.tenor.com/images/2440ac6ca623910a258b8616704850f0/tenor.gif?itemid=7922565",
                "https://media1.tenor.com/images/7b9575ccf2a5b33f97d0eaa053e1892c/tenor.gif?itemid=12180198",
                "https://media1.tenor.com/images/f3f456723f2f8735d118b43823c837f5/tenor.gif?itemid=14659250",
                "https://media1.tenor.com/images/6b42070f19e228d7a4ed76d4b35672cd/tenor.gif?itemid=9051585",
                "https://media1.tenor.com/images/34a08d324868d33358e0a465040f210e/tenor.gif?itemid=11961581",
                "https://media1.tenor.com/images/2735c3a10b0b09871cd5d6bded794f0d/tenor.gif?itemid=14399284",
                "https://media1.tenor.com/images/774226b902dac2639f2162bc40e1ad83/tenor.gif?itemid=13122306",
                "https://media1.tenor.com/images/128c1cfb7f4e6ea4a4dce9b487648143/tenor.gif?itemid=12051598",
                "https://media1.tenor.com/images/cb5b6f8b267be7f9f0e1dd4ac52e6439/tenor.gif?itemid=4696679",
                "https://media1.tenor.com/images/c7f647ab1a07bc5cb2e2783169095329/tenor.gif?itemid=5594274",
                "https://media1.tenor.com/images/491225e5ca713454f9cc25318129976d/tenor.gif?itemid=11039959",
                "https://media1.tenor.com/images/c688d2cf5c50569c74ce8e8d87c40935/tenor.gif?itemid=13341413",
                "https://media1.tenor.com/images/49c23b25f05b791cf7149ba3cc0f2dde/tenor.gif?itemid=14987144",
                "https://media1.tenor.com/images/9f684a0ae87a1a62053038971c0ac001/tenor.gif?itemid=14499663"
        };
    }

}
