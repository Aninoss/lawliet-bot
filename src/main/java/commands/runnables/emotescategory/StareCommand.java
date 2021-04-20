package commands.runnables.emotescategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.EmoteAbstract;

@CommandProperties(
        trigger = "stare",
        emoji = "\uD83D\uDC40",
        executableWithoutArgs = true,
        aliases = { "see" }
)
public class StareCommand extends EmoteAbstract {

    public StareCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[] {
                "https://cdn.discordapp.com/attachments/736260687633842216/736260702947508244/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260707611312288/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260716276875354/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260725416263691/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260731065991278/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260742839271424/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260754600362054/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260765467672756/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260774288162957/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260786942509076/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260792239915088/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260805212766288/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260809755328573/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260815098871828/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260822317137940/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260832987578398/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260838255755294/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260849764794508/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260861676486726/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260870103105617/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260884573323394/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260888947851304/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260891879931964/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260901484625950/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260905527934986/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260910791917690/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260917704261812/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260921600508005/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260930098298931/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260936528166963/stare.gif"
        };
    }

}
