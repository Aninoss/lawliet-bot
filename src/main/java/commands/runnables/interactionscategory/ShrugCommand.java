package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "shrug",
        emoji = "🤷",
        executableWithoutArgs = true
)
public class ShrugCommand extends RolePlayAbstract {

    public ShrugCommand(Locale locale, String prefix) {
        super(locale, prefix, false);

        setFtaGifs(
                "https://cdn.discordapp.com/attachments/743231747415867483/743231795050315787/shrug.gif",
                "https://cdn.discordapp.com/attachments/743231747415867483/743231970557034597/shrug.gif",
                "https://cdn.discordapp.com/attachments/743231747415867483/743232043651039283/shrug.gif",
                "https://cdn.discordapp.com/attachments/743231747415867483/743232193207205958/shrug.gif",
                "https://cdn.discordapp.com/attachments/743231747415867483/743232253194141776/shrug.gif"
        );
        setMtaGifs(
                "https://cdn.discordapp.com/attachments/743231747415867483/743232018091081769/shrug.gif",
                "https://cdn.discordapp.com/attachments/743231747415867483/743232278288924752/shrug.gif",
                "https://cdn.discordapp.com/attachments/743231747415867483/743232310358573146/shrug.gif",
                "https://cdn.discordapp.com/attachments/743231747415867483/821077491603079198/shrug.gif"
        );
        setAtaGifs(
                "https://cdn.discordapp.com/attachments/743231747415867483/743232226140880907/shrug.gif"
        );
    }

}
