package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;
import Commands.InteractionAbstract;

@CommandProperties(
        trigger = "merkel",
        emoji = "\uD83C\uDDE9\uD83C\uDDEA",
        executable = true
)
public class MerkelCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736269653948760146/736269660143747082/merkel.png"
        };
    }

}
