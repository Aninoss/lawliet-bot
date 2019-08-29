package Commands.PowerPlant;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Permission;
import Constants.PowerPlantStatus;
import General.*;
import General.Mention.MentionFinder;
import General.Mention.MentionList;
import MySQL.DBServer;
import MySQL.DBUser;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

public class GiveCommand extends Command implements onRecievedListener {
    private Message message;

    public GiveCommand() {
        super();
        trigger = "give";
        privateUse = false;
        botPermissions = Permission.USE_EXTERNAL_EMOJIS_IN_TEXT_CHANNEL;
        userPermissions = 0;
        nsfw = false;
        withLoadingBar = false;
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat/128/gift-icon.png";
        emoji = "\uD83C\uDF81";
        executable = false;
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        if (event.getMessage().getUserAuthor().get().isBot()) return false;

        PowerPlantStatus status = DBServer.getPowerPlantStatusFromServer(event.getServer().get());
        if (status == PowerPlantStatus.ACTIVE) {
            Server server = event.getServer().get();
            Message message = event.getMessage();
            MentionList<User> userMarked = MentionFinder.getUsers(message,followedString);
            ArrayList<User> list = userMarked.getList();
            for(User user: (ArrayList<User>)list.clone()) {
                if (user.isBot() || user.equals(event.getMessage().getUserAuthor().get())) list.remove(user);
            }

            if (list.size() == 0) {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,getString( "no_mentions"))).get();
                return false;
            }

            followedString = userMarked.getResultMessageString();

            User user0 = event.getMessage().getUserAuthor().get();
            User user1 = list.get(0);

            long coins = DBUser.getFishingProfile(server, user0).getCoins();
            long value = Tools.filterNumberFromString(followedString);

            if (followedString.toLowerCase().contains("all")) {
                value = coins;
            }

            if (value != -1) {
                if (value >= 1) {
                    if (value <= coins) {
                        EmbedBuilder eb = DBUser.addFishingValues(locale, server, user0, 0L, -value);
                        if (eb != null) event.getChannel().sendMessage(eb);

                        eb = DBUser.addFishingValues(locale, server, user1, 0L, value);
                        if (eb != null) event.getChannel().sendMessage(eb).get();

                        event.getChannel().sendMessage(EmbedFactory.getCommandEmbedSuccess(this, getString("successful", Tools.numToString(locale, value), user1.getMentionTag()))).get();
                        return true;
                    } else {
                        event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, getString("too_large", Tools.numToString(locale, coins)))).get();
                    }
                } else {
                    event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(locale, TextManager.GENERAL, "too_small", "1"))).get();
                }
            } else {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(locale, TextManager.GENERAL, "no_digit"))).get();
            }

            return false;
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(locale, TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", prefix), TextManager.getString(locale, TextManager.GENERAL, "fishing_notactive_title")));
            return false;
        }
    }
}
