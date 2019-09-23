package Commands.PowerPlant;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Permission;
import Constants.PowerPlantStatus;
import General.*;
import General.Mention.MentionFinder;
import MySQL.DBServer;
import MySQL.DBUser;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "account",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS_IN_TEXT_CHANNEL,
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/flat-finance/128/person-icon.png",
        emoji = "\uD83D\uDE4B",
        executable = true,
        aliases = {"profile", "profil"}
)
public class AccountCommand extends Command implements onRecievedListener {

    public AccountCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws SQLException, IOException, ExecutionException, InterruptedException {
        PowerPlantStatus status = DBServer.getPowerPlantStatusFromServer(event.getServer().get());
        if (status == PowerPlantStatus.ACTIVE) {
            Server server = event.getServer().get();
            Message message = event.getMessage();
            ArrayList<User> list = MentionFinder.getUsers(message,followedString).getList();
            if (list.size() > 5) {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                        TextManager.getString(getLocale(),TextManager.GENERAL,"too_many_users"))).get();
                return false;
            }
            boolean userMentioned = true;
            boolean userBefore = list.size() > 0;
            for(User user: (ArrayList<User>)list.clone()) {
                if (user.isBot()) list.remove(user);
            }
            if (list.size() == 0) {
                if (userBefore) {
                    event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, getString("nobot"))).get();
                    return false;
                } else {
                    list.add(message.getUserAuthor().get());
                    userMentioned = false;
                }
            }
            for(User user: list) {
                EmbedBuilder eb = DBUser.addFishingValues(getLocale(), server, user, 0L, 0L);
                if (eb != null) {
                    eb.setAuthor(getString("author", user.getDisplayName(server)), "", user.getAvatar());
                    if (!userMentioned)
                        eb.setFooter(TextManager.getString(getLocale(), TextManager.GENERAL, "mention_optional"));
                    event.getChannel().sendMessage(eb).get();
                }
            }
            return true;
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title")));
            return false;
        }
    }
}
