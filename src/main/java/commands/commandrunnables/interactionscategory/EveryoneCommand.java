package commands.commandrunnables.interactionscategory;
import commands.commandlisteners.CommandProperties;
import commands.commandrunnables.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "everyone",
        emoji = "\uD83D\uDE21",
        executable = true
)
public class EveryoneCommand extends InteractionAbstract {

    public EveryoneCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736272698870857738/736272703253643314/everyone.jpg",
                "https://media.discordapp.net/attachments/736272698870857738/736272708106715236/everyone.jpg",
                "https://media.discordapp.net/attachments/736272698870857738/736272711499776050/everyone.jpg",
                "https://media.discordapp.net/attachments/736272698870857738/736272716058853497/everyone.jpg",
                "https://media.discordapp.net/attachments/736272698870857738/736272718864842852/everyone.jpg",
                "https://media.discordapp.net/attachments/736272698870857738/736272726146416650/everyone.gif",
                "https://media.discordapp.net/attachments/736272698870857738/736272729887473684/everyone.jpg",
                "https://media.discordapp.net/attachments/736272698870857738/736272736107888650/everyone.jpg",
                "https://media.discordapp.net/attachments/736272698870857738/736272744731115580/everyone.gif",
                "https://media.discordapp.net/attachments/736272698870857738/736272748481085570/everyone.jpg",
                "https://media.discordapp.net/attachments/736272698870857738/736272752889036890/everyone.jpg"
        };
    }

}
