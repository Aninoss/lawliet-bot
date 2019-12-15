package Commands.Interactions;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "yurikiss",
        emoji = "\uD83D\uDC69\u200D❤️\u200D\uD83D\uDC69",
        executable = false
)
public class YuriKissCommand extends InteractionCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public YuriKissCommand() {
        super("https://i.gifer.com/Djbt.gif",
                "https://i.gifer.com/KTGr.gif",
                "https://i.gifer.com/J1b0.gif",
                "https://www.wykop.pl/cdn/c3201142/comment_tfROJ3JwtatzGcJxpnnFRiunICfsZsb5.gif",
                "https://media1.tenor.com/images/279c4716a469ace39b15e34d7fa3e7c4/tenor.gif?itemid=11487318",
                "https://data.whicdn.com/images/95252800/original.gif",
                "https://cdn.weeb.sh/images/rJrCj6_w-.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/579494706870747147/0.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/579494710247292929/1.gif",
                "https://media1.tenor.com/images/1f06e53598fb516931da7baee4807e4d/tenor.gif?itemid=9118951",
                "https://media1.tenor.com/images/69ace17be06147ed8db45af81d1d5485/tenor.gif?itemid=15111552",
                "https://media1.tenor.com/images/83c1c6749b404d95c05df23a67c0ca41/tenor.gif?itemid=12003885"
        );
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
