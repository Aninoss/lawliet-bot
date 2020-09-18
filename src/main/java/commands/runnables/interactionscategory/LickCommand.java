package commands.runnables.interactionscategory;
import commands.listeners.CommandProperties;
import commands.runnables.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "lick",
        emoji = "\uD83D\uDE0B",
        executable = true
)
public class LickCommand extends InteractionAbstract {

    public LickCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736276719161311264/736276727541661706/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276731530444870/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276735301255208/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276747737235487/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276750547288244/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276751990259762/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276758483042414/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276770109653022/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276778502586378/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276786043682868/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276797376823316/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276800291733584/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276810979082270/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276819862356096/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276823985356842/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276837382226020/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276845078511727/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276848769499206/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276854931062904/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276860379594812/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276868709220352/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276874107289681/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276878918156340/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276884370882578/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276895477530684/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276904692416642/lick.gif",
                "https://media.discordapp.net/attachments/736276719161311264/736276914175475712/lick.gif"
        };
    }

}
