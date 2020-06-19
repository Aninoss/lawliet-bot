package Commands.InteractionsCategory;

import CommandListeners.CommandProperties;
import Commands.InteractionAbstract;

@CommandProperties(
        trigger = "wave",
        emoji = "\uD83D\uDC4B",
        executable = true,
        aliases = {"greet", "bye", "hi", "cya"}
)
public class WaveCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/72c9b849aa10b222371ebb99a6b1896a/tenor.gif?itemid=8807701",
                "https://media1.tenor.com/images/056c584d9335fcabf080ca43e583e3c4/tenor.gif?itemid=8994845",
                "https://media1.tenor.com/images/c2e21a9d8e17c1d335166dbcbe0bd1bf/tenor.gif?itemid=5459102",
                "https://media1.tenor.com/images/97dd3b98910af5ac12559df23454f9ac/tenor.gif?itemid=12046347",
                "https://media1.tenor.com/images/79f33c2f524cbfed4ef6896b39e67663/tenor.gif?itemid=9416181",
                "https://media1.tenor.com/images/9ea72ef078139ced289852e8a4ea0c5c/tenor.gif?itemid=7537923",
                "https://media1.tenor.com/images/f5cd33863e8319ea72990eefc8e697a8/tenor.gif?itemid=5417197",
                "https://media1.tenor.com/images/b82e6a78b221f7dc2e41605b6aa2cbcc/tenor.gif?itemid=11503720",
                "https://media1.tenor.com/images/a251caa1a2f4ca8db9da1ec9dfd95c2b/tenor.gif?itemid=13358680",
                "https://media1.tenor.com/images/d6a2910107681d5d2deabf0b4d872906/tenor.gif?itemid=10548215",
                "https://media1.tenor.com/images/d2a4bcd7648c32d1a10c36b918b45c6b/tenor.gif?itemid=14518602",
                "https://media1.tenor.com/images/74c478ebe44025b5ce0b3235cc5dc9e5/tenor.gif?itemid=12005603",
                "https://media1.tenor.com/images/972424767943ed34a19f6ff2a9cbe976/tenor.gif?itemid=14192312",
                "https://media1.tenor.com/images/d10c3d213be6893235d97ae768db8c07/tenor.gif?itemid=4608178",
                "https://media1.tenor.com/images/900e502f7534a3756106655170ff6397/tenor.gif?itemid=12421971",
                "https://media1.tenor.com/images/dcc97b4b37a36c17009c1fa73eb32c9b/tenor.gif?itemid=14970331",
                "https://media1.tenor.com/images/ba6274e75aa5db794344160f52f93cab/tenor.gif?itemid=13907870",
                "https://media1.tenor.com/images/a2a85146f3ea210a8e5f8e4042d96f16/tenor.gif?itemid=5142331",
                "https://media1.tenor.com/images/55e095422090eddf82474cd2d52d68ef/tenor.gif?itemid=12003948",
                "https://media1.tenor.com/images/e16def45f8b0e39cfc93440517695fbd/tenor.gif?itemid=12217237",
                "https://media1.tenor.com/images/303fa578c015329fec602c34240ef58e/tenor.gif?itemid=10904607",
                "https://media1.tenor.com/images/59df72b5fc429a01e745684bd3f8b66a/tenor.gif?itemid=12363611",
                "https://media1.tenor.com/images/5ec19a5bfed4ac58ceb52e2fc265a3e3/tenor.gif?itemid=13701943",
                "https://media1.tenor.com/images/2585170d55cf54d2e890e3028b3b0b9f/tenor.gif?itemid=12395965",
                "https://media1.tenor.com/images/3c2054130fb03de2d8549a98a3e2685a/tenor.gif?itemid=6195457",
                "https://media1.tenor.com/images/bf598d1de4b18315f117333c54e5f6c4/tenor.gif?itemid=12484739",
                "https://media1.tenor.com/images/5ba9f79a3e164b1f6dba42965fee7377/tenor.gif?itemid=13783883",
                "https://media1.tenor.com/images/37aa92ebab4f4657a8f1b913b4ea7da4/tenor.gif?itemid=5970516",
                "https://media1.tenor.com/images/261ef8fda67d96057a3f5b912e3a7280/tenor.gif?itemid=12912666",
                "https://media1.tenor.com/images/0a70c873c428032bcbf1354da765e278/tenor.gif?itemid=10965696",
                "https://cdn.discordapp.com/attachments/499629904380297226/709440870788628500/awkward.gif"
        };
    }

}
