package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;
import Commands.InteractionAbstract;

@CommandProperties(
        trigger = "steal",
        emoji = "❔",
        executable = true
)
public class StealCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736271960065048577/736271967300223079/steal.gif",
                "https://media.discordapp.net/attachments/736271960065048577/736271972966858772/steal.gif",
                "https://media.discordapp.net/attachments/736271960065048577/736271981372112958/steal.gif",
                "https://media.discordapp.net/attachments/736271960065048577/736271984941596682/steal.gif",
                "https://media.discordapp.net/attachments/736271960065048577/736271990607970424/steal.gif",
                "https://media.discordapp.net/attachments/736271960065048577/736271997977362572/steal.gif",
                "https://media.discordapp.net/attachments/736271960065048577/736272008761049199/steal.gif",
                "https://media.discordapp.net/attachments/736271960065048577/736272017590059038/steal.gif",
                "https://media.discordapp.net/attachments/736271960065048577/736272026041450557/steal.gif"
        };
    }

}
