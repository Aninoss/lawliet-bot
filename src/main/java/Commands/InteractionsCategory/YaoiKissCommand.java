package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;
import Commands.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "yaoikiss",
        emoji = "\uD83D\uDC68\u200D❤️\u200D\uD83D\uDC68",
        executable = true
)
public class YaoiKissCommand extends InteractionAbstract {

    public YaoiKissCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736280745601007737/736280751187689482/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280753842683994/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280757068234823/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280762189348965/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280765754769488/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280775162593280/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280778094411776/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280780505874502/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280783437955083/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280786956976168/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280798512152617/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280803624878210/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280806497976360/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280812973981786/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280816711106710/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280822331473930/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280826047889448/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280832011927622/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280839658143805/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280845991542824/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280852501233674/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280860105506816/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280871895695480/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280880162537472/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280893903077466/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280897053130844/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280901410881653/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280907043831869/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280913641734216/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280917433253928/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280925477928960/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736280930292989952/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736284287359254599/yaoikiss.gif",
                "https://media.discordapp.net/attachments/736280745601007737/736284502275522670/yaoikiss.gif"
        };
    }

}
