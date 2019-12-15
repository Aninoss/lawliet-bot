package Commands.Emotes;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "run",
        emoji = "\uD83C\uDFC3",
        executable = true
)
public class RunCommand extends EmoteCommand implements onRecievedListener {

    private static ArrayList<Integer> picked = new ArrayList<>();

    public RunCommand() {
        super("https://media1.tenor.com/images/0a1aaa016c56cd398a28ba745b541ba8/tenor.gif?itemid=11734758",
                "https://media1.tenor.com/images/cdd97372f67962d3c6b39e31b3aa05b0/tenor.gif?itemid=5354482",
                "https://media1.tenor.com/images/087628e72f9ac4d5aa752032813bd0c3/tenor.gif?itemid=12961969",
                "https://media1.tenor.com/images/e99d0a0812b2a5481c3d98d51950814d/tenor.gif?itemid=6181817",
                "https://media1.tenor.com/images/b68a3eca610725b8eba6def973229955/tenor.gif?itemid=12374475",
                "https://media1.tenor.com/images/3df5b4ead18a6b5ecc0d4a7ae1476a74/tenor.gif?itemid=4718145",
                "https://media1.tenor.com/images/8f6323f71a398a806361d8c1e80bbcb3/tenor.gif?itemid=5501383",
                "https://media1.tenor.com/images/17edfb50ba26e75ffa27ba851034d627/tenor.gif?itemid=8737177",
                "https://media1.tenor.com/images/879fbfa179c7b510f21743e1a19f0de6/tenor.gif?itemid=11115606",
                "https://media1.tenor.com/images/c62a8b9e0d0643874d68d09bb2c1f859/tenor.gif?itemid=12546891",
                "https://media1.tenor.com/images/2f6464634efffc95e78a3a33cdb83c5b/tenor.gif?itemid=4574927",
                "https://media1.tenor.com/images/1439ebc108e631960a610a8f5cad4e8a/tenor.gif?itemid=5700816",
                "https://media1.tenor.com/images/db41d2a91102a4e24df9aa98fe7f97b6/tenor.gif?itemid=15082392",
                "https://media1.tenor.com/images/dfe36d74746f80fd222119b249c0c960/tenor.gif?itemid=9182649",
                "https://media1.tenor.com/images/c37bb56a83c9c2e57833b83c819c8caa/tenor.gif?itemid=8540975",
                "https://media1.tenor.com/images/dffcb724cd274e666c8589a7e57e2915/tenor.gif?itemid=15355808",
                "https://media1.tenor.com/images/b6d6602cec4f3f16870d815fcd320173/tenor.gif?itemid=9352866",
                "https://media1.tenor.com/images/cc2ba1cad3a9bbd8b3086c1a1542698e/tenor.gif?itemid=14546534",
                "https://media1.tenor.com/images/498e8acfbae9c79ac7a9180dff83d278/tenor.gif?itemid=15698673",
                "https://media1.tenor.com/images/ef5a57dd70214b4712b65d3aecc8b037/tenor.gif?itemid=11109847",
                "https://media1.tenor.com/images/65f82c6732e8b2edf27f424c1fd6cf00/tenor.gif?itemid=3411991",
                "https://media1.tenor.com/images/b9584605fa97c4d6455ec80a0477eb13/tenor.gif?itemid=14780939",
                "https://media1.tenor.com/images/44a8d29788a7e898b67ade8a0086f294/tenor.gif?itemid=8540976",
                "https://media1.tenor.com/images/e75e255cd1689c8b2914afb29b1b046b/tenor.gif?itemid=11437115",
                "https://media1.tenor.com/images/24b31bc04477b741cc3406fb2996a4af/tenor.gif?itemid=14444818",
                "https://media1.tenor.com/images/6d47909ce3371297efbaa887fcaf1318/tenor.gif?itemid=15569282",
                "https://media1.tenor.com/images/a688aa7040841b2344a49f521e1b4338/tenor.gif?itemid=15407085",
                "https://media1.tenor.com/images/92770a75b81c3da1dbf58bf8fa3eb510/tenor.gif?itemid=5393623",
                "https://media1.tenor.com/images/bd3fa91aac4ba421b644b7cf43eb559a/tenor.gif?itemid=7340890",
                "https://media1.tenor.com/images/be3426585ca88098592d18c167c6b684/tenor.gif?itemid=12251542",
                "https://media1.tenor.com/images/422b112e5cbd7fb813bbf061ab0db83b/tenor.gif?itemid=13714597",
                "https://media1.tenor.com/images/93867c6d7af3491eb9cdc9d16404593f/tenor.gif?itemid=7617614"
        );
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onEmoteRecieved(event, picked);
    }
}
