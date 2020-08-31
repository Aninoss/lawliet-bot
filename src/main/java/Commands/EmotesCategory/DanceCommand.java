package Commands.EmotesCategory;
import CommandListeners.CommandProperties;

import Commands.EmoteAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "dance",
        emoji = "\uD83D\uDD7A",
        executable = true,
        aliases = {"party"}
)
public class DanceCommand extends EmoteAbstract {

    public DanceCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736255695296528484/736255699134316634/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255702888218645/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255712967131146/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255716825890936/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255721024520243/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255733183676506/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255736828395571/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255741056253993/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255748463525958/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255753236643890/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255762107596810/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255767702798447/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255770919829575/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255778809446510/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255781896192040/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255785176399972/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255794114461881/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255798123954337/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255805346676797/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255811684401274/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255818269458452/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255825542119504/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255833431867472/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736256109261881344/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/736255845733498900/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/745278813138583702/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/745279078990348308/dance.gif",
                "https://media.discordapp.net/attachments/736255695296528484/750018053256904806/dance.gif"
        };
    }

}
