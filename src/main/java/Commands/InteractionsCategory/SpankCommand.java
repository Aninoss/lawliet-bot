package Commands.InteractionsCategory;

import CommandListeners.CommandProperties;
import Commands.InteractionAbstract;

@CommandProperties(
    trigger = "spank",
    emoji = "\uD83C\uDF51",
    executable = false
)
public class SpankCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736274084488282113/736274092105269308/spank.gif",
                "https://media.discordapp.net/attachments/736274084488282113/736274104361025606/spank.gif",
                "https://media.discordapp.net/attachments/736274084488282113/736274108337225818/spank.gif",
                "https://media.discordapp.net/attachments/736274084488282113/736274115195043931/spank.gif",
                "https://media.discordapp.net/attachments/736274084488282113/736274118776979483/spank.gif",
                "https://media.discordapp.net/attachments/736274084488282113/736274121486237736/spank.gif",
                "https://media.discordapp.net/attachments/736274084488282113/736274125311443056/spank.gif",
                "https://media.discordapp.net/attachments/736274084488282113/736274130747523072/spank.gif",
                "https://media.discordapp.net/attachments/736274084488282113/736274138632552478/spank.gif",
                "https://media.discordapp.net/attachments/736274084488282113/736274143187828757/spank.gif",
                "https://media.discordapp.net/attachments/736274084488282113/736274153820127362/spank.gif",
                "https://media.discordapp.net/attachments/736274084488282113/736274160266903674/spank.gif"
        };
    }

}
