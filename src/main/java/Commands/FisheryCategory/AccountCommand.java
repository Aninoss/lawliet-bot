package Commands.FisheryCategory;

import CommandListeners.CommandProperties;
import CommandSupporters.Command;
import Constants.Permission;
import Constants.FisheryStatus;
import Core.*;
import Core.Mention.MentionTools;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.Server.DBServer;
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
        trigger = "acc",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/flat-finance/128/person-icon.png",
        emoji = "\uD83D\uDE4B",
        executable = true,
        aliases = {"profile", "profil", "account"}
)
public class AccountCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws SQLException, IOException, ExecutionException, InterruptedException {
        FisheryStatus status = DBServer.getInstance().getBean(event.getServer().get().getId()).getFisheryStatus();
        if (status == FisheryStatus.ACTIVE) {
            Server server = event.getServer().get();
            Message message = event.getMessage();
            ArrayList<User> list = MentionTools.getUsers(message,followedString).getList();
            if (list.size() > 5) {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                        TextManager.getString(getLocale(),TextManager.GENERAL,"too_many_users"))).get();
                return false;
            }
            boolean userMentioned = true;
            boolean userBefore = list.size() > 0;
            list.removeIf(User::isBot);
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
                EmbedBuilder eb = DBFishery.getInstance().getBean(event.getServer().get().getId()).getUserBean(user.getId()).getAccountEmbed();
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
