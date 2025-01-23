package commands.runnables.nsfwinteractionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "cum",
        emoji = "💦",
        executableWithoutArgs = true,
        nsfw = true,
        aliases = "semen",
        requiresFullMemberCache = true
)
public class CumCommand extends RolePlayAbstract {

    public CumCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/834500528426844160/834500917847916605/cum.gif",
                "https://cdn.discordapp.com/attachments/834500528426844160/834500928869761044/cum.gif",
                "https://cdn.discordapp.com/attachments/834500528426844160/834500940002230332/cum.gif",
                "https://cdn.discordapp.com/attachments/834500528426844160/834500951191584809/cum.gif",
                "https://cdn.discordapp.com/attachments/834500528426844160/834500964269686784/cum.gif",
                "https://cdn.discordapp.com/attachments/834500528426844160/834500991411945482/cum.gif",
                "https://cdn.discordapp.com/attachments/834500528426844160/834501054635704420/cum.gif",
                "https://cdn.discordapp.com/attachments/834500528426844160/834501065961111622/cum.gif",
                "https://cdn.discordapp.com/attachments/834500528426844160/834501078753476639/cum.gif",
                "https://cdn.discordapp.com/attachments/834500528426844160/834501092069998622/cum.gif",
                "https://cdn.discordapp.com/attachments/834500528426844160/834501103655190548/cum.gif",
                "https://cdn.discordapp.com/attachments/834500528426844160/834501115449180200/cum.gif",
                "https://cdn.discordapp.com/attachments/834500528426844160/834501127272923217/cum.gif",
                "https://cdn.discordapp.com/attachments/834500528426844160/834501140014956624/cum.gif",
                "https://cdn.discordapp.com/attachments/834500528426844160/839471906024849408/cum.gif",
                "https://cdn.discordapp.com/attachments/834500528426844160/881904994863951873/cum.gif",
                "https://cdn.discordapp.com/attachments/834500528426844160/881905088447250443/cum.gif",
                "https://cdn.discordapp.com/attachments/834500528426844160/1151397084436627466/cum.gif"
        );
    }

}
