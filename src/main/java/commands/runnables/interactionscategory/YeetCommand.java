package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "yeet",
        emoji = "\uD83D\uDCA8",
        executableWithoutArgs = true,
        requiresFullMemberCache = true
)
public class YeetCommand extends RolePlayAbstract {

    public YeetCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false);

        setFtfGifs(
                "https://cdn.discordapp.com/attachments/910944832845905980/910945162706968596/yeet.gif",
                "https://cdn.discordapp.com/attachments/910944832845905980/910945354596364308/yeet.gif",
                "https://cdn.discordapp.com/attachments/910944832845905980/910945462650040430/yeet.gif",
                "https://cdn.discordapp.com/attachments/910944832845905980/910945543176454214/yeet.gif"
        );
        setFtmGifs(
                "https://cdn.discordapp.com/attachments/910944832845905980/910945707337322567/yeet.gif",
                "https://cdn.discordapp.com/attachments/910944832845905980/910945848337236008/yeet.gif",
                "https://cdn.discordapp.com/attachments/910944832845905980/910946009486614528/yeet.gif"
        );
        setMtfGifs(
                "https://cdn.discordapp.com/attachments/910944832845905980/910945933691338772/yeet.gif"
        );
        setMtmGifs(
                "https://cdn.discordapp.com/attachments/910944832845905980/910945255141023755/yeet.gif"
        );
        setAtfGifs(
                "https://cdn.discordapp.com/attachments/910944832845905980/910945626911555644/yeet.gif"
        );
    }

}
