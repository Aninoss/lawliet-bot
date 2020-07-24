package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;
import Commands.InteractionAbstract;

@CommandProperties(
        trigger = "yaoihug",
        emoji = "\uD83D\uDC68\uD83D\uDC50\uD83D\uDC68",
        executable = true
)
public class YaoiHugCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736278340733894710/736278344986918954/yaoihug.gif",
                "https://media.discordapp.net/attachments/736278340733894710/736278350959607949/yaoihug.gif",
                "https://media.discordapp.net/attachments/736278340733894710/736278355015630868/yaoihug.gif",
                "https://media.discordapp.net/attachments/736278340733894710/736278358869934101/yaoihug.gif",
                "https://media.discordapp.net/attachments/736278340733894710/736278361889964072/yaoihug.gif",
                "https://media.discordapp.net/attachments/736278340733894710/736278366818271302/yaoihug.gif",
                "https://media.discordapp.net/attachments/736278340733894710/736278374993100840/yaoihug.gif",
                "https://media.discordapp.net/attachments/736278340733894710/736278379849973780/yaoihug.gif",
                "https://media.discordapp.net/attachments/736278340733894710/736278390432071800/yaoihug.gif",
                "https://media.discordapp.net/attachments/736278340733894710/736278395180154961/yaoihug.gif",
                "https://media.discordapp.net/attachments/736278340733894710/736278403434676224/yaoihug.gif",
                "https://media.discordapp.net/attachments/736278340733894710/736278410568925315/yaoihug.gif",
                "https://media.discordapp.net/attachments/736278340733894710/736278429510533176/yaoihug.gif",
                "https://media.discordapp.net/attachments/736278340733894710/736278434866790532/yaoihug.gif"
        };
    }

}
