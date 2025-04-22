package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "nervous",
        emoji = "💦",
        executableWithoutArgs = true,
        aliases = { "nerves" }
)
public class NervousCommand extends RolePlayAbstract {

    public NervousCommand(Locale locale, String prefix) {
        super(locale, prefix, false);

        setFtaGifs(
                "https://cdn.discordapp.com/attachments/1298595419143802981/1298595485577252974/nervous.gif",
                "https://cdn.discordapp.com/attachments/1298595419143802981/1298595631455273031/nervous.gif",
                "https://cdn.discordapp.com/attachments/1298595419143802981/1298595835977924661/nervous.gif",
                "https://cdn.discordapp.com/attachments/1298595419143802981/1298596533184499743/nervous.gif"
        );
        setMtaGifs(
                "https://cdn.discordapp.com/attachments/1298595419143802981/1298595718977818644/nervous.gif",
                "https://cdn.discordapp.com/attachments/1298595419143802981/1298595781384863814/nervous.gif",
                "https://cdn.discordapp.com/attachments/1298595419143802981/1298595889354637323/nervous.gif",
                "https://cdn.discordapp.com/attachments/1298595419143802981/1298596188496592939/nervous.gif",
                "https://cdn.discordapp.com/attachments/1298595419143802981/1298596250857508936/nervous.gif",
                "https://cdn.discordapp.com/attachments/1298595419143802981/1298596476351414333/nervous.gif"
        );
    }

}
