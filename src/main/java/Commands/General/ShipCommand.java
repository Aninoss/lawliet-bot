package Commands.General;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Permission;
import General.EmbedFactory;
import General.Mention.MentionFinder;
import General.Tools;
import General.ImageCreator;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Random;

@CommandProperties(
    trigger = "ship",
    botPermissions = Permission.ATTACH_FILES_TO_TEXT_CHANNEL,
    withLoadingBar = true,
    emoji = "\uD83D\uDC6B",
    executable = false
)
public class ShipCommand extends Command implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public ShipCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        Server server = event.getServer().get();
        Message message = event.getMessage();
        ArrayList<User> list = MentionFinder.getUsers(message,followedString).getList();
        if (list.size() != 2) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    getString( "not_2"))).get();
            return false;
        }
        String idString = String.valueOf(list.get(0).getId() + list.get(1).getId());
        int randomNum = String.valueOf(idString.hashCode()).hashCode();
        int percentage = new Random(randomNum).nextInt(101);

        int u = 0;
        if (list.contains(event.getApi().getUserById(272037078919938058L).get())) u++;
        if (list.contains(event.getApi().getUserById(193889897118040064L).get())) u++;
        if (list.contains(event.getApi().getUserById(303085910784737281L).get()) || list.contains(event.getApi().getUserById(556898762811899925L).get())) {
            if (list.contains(event.getApi().getUserById(574276009625518091L).get())) {
                //percentage = 4;
            } else u++;
        }
        if (u >= 2) percentage = 98;


        int n = Tools.pickFullRandom(picked,7);

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this)
                .setImage(ImageCreator.createImageShip(getLocale(),list.get(0),list.get(1),n,percentage));
        event.getChannel().sendMessage(eb).get();

        return true;
    }
}
