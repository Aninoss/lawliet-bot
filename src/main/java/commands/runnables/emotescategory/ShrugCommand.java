package commands.runnables.emotescategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.EmoteAbstract;

@CommandProperties(
        trigger = "shrug",
        emoji = "🤷",
        executableWithoutArgs = true
)
public class ShrugCommand extends EmoteAbstract {

    public ShrugCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[] {
                "https://media.discordapp.net/attachments/743231747415867483/743231795050315787/shrug.gif",
                "https://media.discordapp.net/attachments/743231747415867483/743231970557034597/shrug.gif",
                "https://media.discordapp.net/attachments/743231747415867483/743232018091081769/shrug.gif",
                "https://media.discordapp.net/attachments/743231747415867483/743232043651039283/shrug.gif",
                "https://media.discordapp.net/attachments/743231747415867483/743232193207205958/shrug.gif",
                "https://media.discordapp.net/attachments/743231747415867483/743232226140880907/shrug.gif",
                "https://media.discordapp.net/attachments/743231747415867483/743232253194141776/shrug.gif",
                "https://media.discordapp.net/attachments/743231747415867483/743232278288924752/shrug.gif",
                "https://media.discordapp.net/attachments/743231747415867483/743232310358573146/shrug.gif",
                "https://media.discordapp.net/attachments/743231747415867483/743232541858725898/shrug.gif",
                "https://media.discordapp.net/attachments/743231747415867483/821077491603079198/shrug.gif"
        };
    }

}
