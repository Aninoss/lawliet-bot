package Commands.FisheryCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Constants.Permission;
import Constants.FisheryStatus;
import Core.*;
import Core.Mention.MentionTools;
import Core.Mention.MentionList;
import Core.Tools.StringTools;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryUserBean;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "give",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat/128/gift-icon.png",
        emoji = "\uD83C\uDF81",
        executable = false,
        aliases = {"gift"}
)
public class GiveCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        if (event.getMessage().getUserAuthor().get().isBot()) return false;

        FisheryStatus status = DBServer.getInstance().getBean(event.getServer().get().getId()).getFisheryStatus();
        if (status == FisheryStatus.ACTIVE) {
            Server server = event.getServer().get();
            Message message = event.getMessage();
            MentionList<User> userMarked = MentionTools.getUsers(message,followedString);
            ArrayList<User> list = userMarked.getList();
            list.removeIf(user -> user.isBot() || user.equals(event.getMessage().getUserAuthor().get()));

            if (list.size() == 0) {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,getString( "no_mentions"))).get();
                return false;
            }

            followedString = userMarked.getResultMessageString();

            User user0 = event.getMessage().getUserAuthor().get();
            User user1 = list.get(0);

            if (server.getId() == 418223406698332173L) {
                Role role = server.getRoleById(660459523676438528L).get();
                if (!role.getUsers().contains(user0)) return false;
            }

            FisheryUserBean fisheryUser0 = DBFishery.getInstance().getBean(event.getServer().get().getId()).getUser(user0.getId());
            FisheryUserBean fisheryUser1 = DBFishery.getInstance().getBean(event.getServer().get().getId()).getUser(user1.getId());
            long value = MentionTools.getAmountExt(followedString, fisheryUser0.getCoins());

            if (value != -1) {
                if (value >= 1) {
                    if (value <= fisheryUser0.getCoins()) {
                        EmbedBuilder eb = fisheryUser0.changeValues(0, -value);
                        event.getChannel().sendMessage(eb);

                        eb = fisheryUser1.changeValues(0, value);
                        event.getChannel().sendMessage(eb);

                        event.getChannel().sendMessage(EmbedFactory.getCommandEmbedSuccess(this, getString("successful", StringTools.numToString(getLocale(), value), user1.getMentionTag()))).get();
                        return true;
                    } else {
                        event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, getString("too_large", StringTools.numToString(getLocale(), fisheryUser0.getCoins())))).get();
                    }
                } else {
                    event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1"))).get();
                }
            } else {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"))).get();
            }

            return false;
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title")));
            return false;
        }
    }
}
