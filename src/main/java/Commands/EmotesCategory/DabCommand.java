package Commands.EmotesCategory;
import CommandListeners.CommandProperties;
import Commands.EmoteAbstract;

@CommandProperties(
        trigger = "dab",
        emoji = "\uD83C\uDD92",
        executable = true,
        aliases = {"dap"}
)
public class DabCommand extends EmoteAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/d13c16a8853e3b309db0ec7e573c4c94/tenor.gif?itemid=10617637",
                "https://media1.tenor.com/images/033a2e811f625be20008eed617734d40/tenor.gif?itemid=13628207",
                "https://media1.tenor.com/images/425cf503dedae77ca0ffcfebd0ddc5d7/tenor.gif?itemid=12692598"
        };
    }

}
