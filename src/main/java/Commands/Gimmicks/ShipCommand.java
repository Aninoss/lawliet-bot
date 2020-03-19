package Commands.Gimmicks;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Permission;
import General.EmbedFactory;
import General.Mention.MentionFinder;
import General.Tools;
import General.ImageCreator;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
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
        Message message = event.getMessage();
        ArrayList<User> list = MentionFinder.getUsers(message,followedString).getList();
        if (list.size() == 1 && list.get(0).getId() != event.getMessage().getUserAuthor().get().getId()) {
            list.add(event.getMessage().getUserAuthor().get());
        }
        if (list.size() != 2) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    getString( "not_2"))).get();
            return false;
        }
        list.sort(Comparator.comparingLong(DiscordEntity::getId));
        String idString = String.valueOf(list.get(0).getId() + list.get(1).getId());
        int randomNum = String.valueOf(idString.hashCode()).hashCode();
        int percentage = new Random(randomNum).nextInt(101);

        int n = Tools.pickFullRandom(picked,7);
        if (event.getServer().get().getId() == 580048842020487180L) n = 7;

        InputStream is = ImageCreator.createImageShip(getLocale(),list.get(0),list.get(1), n, percentage);
        if (is == null) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, getString("noavatar"))).get();
            return false;
        }

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this)
                .setImage(is);
        event.getChannel().sendMessage(eb).get();

        return true;
    }
}
