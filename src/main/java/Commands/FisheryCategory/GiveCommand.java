package Commands.FisheryCategory;

import CommandListeners.CommandProperties;
import Commands.FisheryAbstract;
import Constants.Permission;
import Core.EmbedFactory;
import Core.Mention.MentionList;
import Core.Mention.MentionUtil;
import Core.TextManager;
import Core.Utils.StringUtil;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryUserBean;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Locale;

@CommandProperties(
        trigger = "give",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        emoji = "\uD83C\uDF81",
        executable = false,
        aliases = {"gift", "pay"}
)
public class GiveCommand extends FisheryAbstract {

    public GiveCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceivedSuccessful(MessageCreateEvent event, String followedString) throws Throwable {
        Server server = event.getServer().get();
        Message message = event.getMessage();
        MentionList<User> userMarked = MentionUtil.getUsers(message, followedString);
        ArrayList<User> list = userMarked.getList();
        list.removeIf(user -> user.isBot() || user.equals(event.getMessage().getUserAuthor().get()));

        if (list.size() == 0) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, getString("no_mentions"))).get();
            return false;
        }

        followedString = userMarked.getResultMessageString();

        User user0 = event.getMessage().getUserAuthor().get();
        User user1 = list.get(0);

        if (server.getId() == 418223406698332173L) {
            Role role = server.getRoleById(660459523676438528L).get();
            if (!role.hasUser(user0)) return false;
        }

        FisheryUserBean fisheryUser0 = DBFishery.getInstance().getBean(event.getServer().get().getId()).getUserBean(user0.getId());
        FisheryUserBean fisheryUser1 = DBFishery.getInstance().getBean(event.getServer().get().getId()).getUserBean(user1.getId());
        long value = Math.min(MentionUtil.getAmountExt(followedString, fisheryUser0.getCoins()), fisheryUser0.getCoins());

        if (value != -1) {
            if (value >= 1) {
                long coins0Pre = fisheryUser0.getCoins();
                long coins1Pre = fisheryUser1.getCoins();

                fisheryUser0.addCoins(-value);
                fisheryUser1.addCoins(value);

                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("successful",
                        StringUtil.numToString(getLocale(), value),
                        user1.getMentionTag(),
                        user0.getMentionTag(),
                        StringUtil.numToString(getLocale(), coins0Pre),
                        StringUtil.numToString(getLocale(), coins0Pre - value),
                        StringUtil.numToString(getLocale(), coins1Pre),
                        StringUtil.numToString(getLocale(), coins1Pre + value)
                ))).get();
                return true;
            } else {
                if (fisheryUser0.getCoins() <= 0)
                    event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, getString("nocoins"))).get();
                else
                    event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1"))).get();
            }
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"))).get();
        }

        return false;
    }
}
