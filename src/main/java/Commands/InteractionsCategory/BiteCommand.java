package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;
import Commands.InteractionAbstract;

@CommandProperties(
    trigger = "bite",
    emoji = "\uD83E\uDE78",
    executable = true
)
public class BiteCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736272811789779006/736272817162551347/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272821134819428/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272835869278359/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272843343659120/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272848963764224/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272854709960784/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272859315306656/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272863157551204/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272877245956156/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272884091060434/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272893218127922/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272902462373888/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272915338756136/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272926579359794/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272935270219842/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272942534492190/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272948289077308/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272953817432184/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272959609765918/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272970309304390/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272976814538812/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272985458999357/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272988722167808/bite.gif",
                "https://media.discordapp.net/attachments/736272811789779006/736272993193427035/bite.gif"
        };
    }

}
