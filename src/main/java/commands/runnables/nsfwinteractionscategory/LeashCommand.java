package commands.runnables.nsfwinteractionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "leash",
        emoji = "🐕",
        executableWithoutArgs = true,
        nsfw = true,
        requiresFullMemberCache = true
)
public class LeashCommand extends RolePlayAbstract {

    public LeashCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false);

        setFtfGifs(
                "https://cdn.discordapp.com/attachments/955835975609749534/955836190802718761/leash.gif",
                "https://cdn.discordapp.com/attachments/955835975609749534/955836254405132338/leash.gif"
        );
        setMtfGifs(
                "https://cdn.discordapp.com/attachments/955835975609749534/955836016932032512/leash.gif",
                "https://cdn.discordapp.com/attachments/955835975609749534/955836092060418118/leash.gif",
                "https://cdn.discordapp.com/attachments/955835975609749534/955836134854897734/leash.gif",
                "https://cdn.discordapp.com/attachments/955835975609749534/955836316136906752/leash.gif",
                "https://cdn.discordapp.com/attachments/955835975609749534/955836365025738823/leash.gif",
                "https://cdn.discordapp.com/attachments/955835975609749534/955836425654382672/leash.gif"
        );
        setAtfGifs(
                "https://cdn.discordapp.com/attachments/955835975609749534/955836504767344740/leash.gif",
                "https://cdn.discordapp.com/attachments/955835975609749534/955836549789007933/leash.gif",
                "https://cdn.discordapp.com/attachments/955835975609749534/955836603476099122/leash.gif",
                "https://cdn.discordapp.com/attachments/955835975609749534/955836699726979102/leash.gif",
                "https://cdn.discordapp.com/attachments/955835975609749534/992098894043414559/leash.gif",
                "https://cdn.discordapp.com/attachments/955835975609749534/992098924506644601/leash.gif"
        );
    }

}
