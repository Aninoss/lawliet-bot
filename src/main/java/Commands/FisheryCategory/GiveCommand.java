package Commands.FisheryCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Permission;
import Constants.FisheryStatus;
import General.*;
import General.Mention.MentionFinder;
import General.Mention.MentionList;
import MySQL.DBServerOld;
import MySQL.DBUser;
import MySQL.Server.DBServer;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "give",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS_IN_TEXT_CHANNEL,
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat/128/gift-icon.png",
        emoji = "\uD83C\uDF81",
        executable = false,
        aliases = {"gift"}
)
public class GiveCommand extends Command implements onRecievedListener {

    public GiveCommand() {
        super();
    }

    @Override
    public boolean onReceived(MessageCreateEvent event, String followedString) throws Throwable {
        if (event.getMessage().getUserAuthor().get().isBot()) return false;

        FisheryStatus status = DBServer.getInstance().getServerBean(event.getServer().get().getId()).getFisheryStatus();
        if (status == FisheryStatus.ACTIVE) {
            Server server = event.getServer().get();
            Message message = event.getMessage();
            MentionList<User> userMarked = MentionFinder.getUsers(message,followedString);
            ArrayList<User> list = userMarked.getList();
            for(User user: new ArrayList<>(list)) {
                if (user.isBot() || user.equals(event.getMessage().getUserAuthor().get())) list.remove(user);
            }

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

            long coins = DBUser.getFishingProfile(server, user0).getCoins();
            long value = Tools.getAmountExt(followedString, coins);

            if (value != -1) {
                if (value >= 1) {
                    if (value <= coins) {
                        EmbedBuilder eb = DBUser.addFishingValues(getLocale(), server, user0, 0L, -value);
                        if (eb != null) event.getChannel().sendMessage(eb);

                        eb = DBUser.addFishingValues(getLocale(), server, user1, 0L, value);
                        if (eb != null) event.getChannel().sendMessage(eb);

                        event.getChannel().sendMessage(EmbedFactory.getCommandEmbedSuccess(this, getString("successful", Tools.numToString(getLocale(), value), user1.getMentionTag()))).get();
                        return true;
                    } else {
                        event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, getString("too_large", Tools.numToString(getLocale(), coins)))).get();
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
