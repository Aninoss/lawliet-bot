package commands.runnables.interactionscategory;
import commands.listeners.CommandProperties;
import commands.runnables.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "love",
        emoji = "\u2764\uFE0F",
        executable = true
)
public class LoveCommand extends InteractionAbstract {

    public LoveCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736277076868595794/736277083407384626/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277085747675206/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277089451507712/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277095386447883/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277101396885615/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277104978690199/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277110284615742/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277122091450418/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277128286437466/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277134557052998/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277138906284173/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277142374973541/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277148159049840/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277151577538681/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277156715430008/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277162566483978/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277170841845873/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277174926966795/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277181893836861/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277184808747018/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277193751265290/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277204891336754/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277211975516261/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277216400507000/love.gif",
                "https://media.discordapp.net/attachments/736277076868595794/736277224491319417/love.gif"
        };
    }

}
