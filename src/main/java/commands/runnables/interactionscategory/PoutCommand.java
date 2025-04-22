package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "pout",
        emoji = "\uD83D\uDE21",
        executableWithoutArgs = true
)
public class PoutCommand extends RolePlayAbstract {

    public PoutCommand(Locale locale, String prefix) {
        super(locale, prefix, false);

        setFtaGifs(
                "https://cdn.discordapp.com/attachments/834836041906126869/834836172979044352/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836185833799710/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836209292541972/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836223226282004/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836248584912937/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836260072849478/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836272522592356/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836285843177532/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836298757177404/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836312900370522/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836326863208498/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836343208149042/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836356278255687/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836380915728455/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836392378761316/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836406076571698/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836419305799760/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836446283694120/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836470514188338/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836484988338176/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836496762535946/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836508934012979/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836520434794548/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836534191980594/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836546305130516/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836560884006962/pout.gif"
        );
        setMtaGifs(
                "https://cdn.discordapp.com/attachments/834836041906126869/834836198090211388/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836235809062962/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836368043409459/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836432140894298/pout.gif",
                "https://cdn.discordapp.com/attachments/834836041906126869/834836459311333447/pout.gif"
        );
    }

}
