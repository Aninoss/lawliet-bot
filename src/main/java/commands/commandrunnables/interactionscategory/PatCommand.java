package commands.commandrunnables.interactionscategory;
import commands.commandlisteners.CommandProperties;
import commands.commandrunnables.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "pat",
        emoji = "\uD83E\uDD1A",
        executable = true,
        aliases = {"praise"}
)
public class PatCommand extends InteractionAbstract {

    public PatCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736275536317382717/736275542193733702/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275544949260368/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275548061302886/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275551500763186/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275553941848176/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275564264161350/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275566885601391/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275577140543569/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275582425366620/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275590251937872/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275593615769640/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275599550578778/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275602939576380/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275607645847572/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275612804579328/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275622304677908/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275627279122502/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275630219591690/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275637765144716/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275648313688124/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275656794701904/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275665401413632/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275670447030292/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275674821689404/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275680597377064/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275692689555456/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275700214136892/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275702793371654/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275705926778895/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275710263558225/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275713761476668/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275763661242478/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/736275767389978676/pat.gif",
                "https://media.discordapp.net/attachments/736275536317382717/741227933955522570/pat.gif"
        };
    }

}
