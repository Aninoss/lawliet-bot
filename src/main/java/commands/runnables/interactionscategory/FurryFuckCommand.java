package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.InteractionAbstract;

@CommandProperties(
        trigger = "furryfuck",
        emoji = "\uD83E\uDD8A",
        executableWithoutArgs = true,
        nsfw = true
)
public class FurryFuckCommand extends InteractionAbstract {

    public FurryFuckCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[] {
                "https://cdn.discordapp.com/attachments/736283579457208414/736283596993855548/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283604535214100/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283613531734127/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283618409840717/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283623678017606/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283636428570634/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283644867379220/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283650022178907/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283677222502400/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283685443338340/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283693911507024/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283774983340102/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283785569632296/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283792360341594/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283802317619250/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283808466206780/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283818163437698/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283824652156938/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283830163472479/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283835431518298/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283848052047913/furryfuck.gif",
                "https://cdn.discordapp.com/attachments/736283579457208414/736283993590202538/furryfuck.gif"
        };
    }

}
