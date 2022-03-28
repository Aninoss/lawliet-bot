package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "finger",
        emoji = "👇",
        executableWithoutArgs = true,
        nsfw = true,
        requiresFullMemberCache = true
)
public class FingerCommand extends RolePlayAbstract {

    public FingerCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true,
                "https://cdn.discordapp.com/attachments/958005774603386881/958007005254140005/finger.gif",
                "https://cdn.discordapp.com/attachments/958005774603386881/958007071817760838/finger.gif",
                "https://cdn.discordapp.com/attachments/958005774603386881/958007294594015252/finger.gif",
                "https://cdn.discordapp.com/attachments/958005774603386881/958007513561853983/finger.gif",
                "https://cdn.discordapp.com/attachments/958005774603386881/958007592087613440/finger.gif",
                "https://cdn.discordapp.com/attachments/958005774603386881/958007654524002344/finger.gif",
                "https://cdn.discordapp.com/attachments/958005774603386881/958007701223391292/finger.gif",
                "https://cdn.discordapp.com/attachments/958005774603386881/958007761629757470/finger.gif",
                "https://cdn.discordapp.com/attachments/958005774603386881/958007818340937748/finger.gif",
                "https://cdn.discordapp.com/attachments/958005774603386881/958007878634053703/finger.gif"
        );
    }

}
