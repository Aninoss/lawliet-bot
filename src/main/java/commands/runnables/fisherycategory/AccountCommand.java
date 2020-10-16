package commands.runnables.fisherycategory;

import commands.listeners.CommandProperties;
import commands.runnables.FisheryAbstract;
import constants.Permission;
import core.EmbedFactory;
import core.utils.EmbedUtil;
import core.utils.MentionUtil;
import core.TextManager;
import mysql.modules.fisheryusers.DBFishery;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Locale;

@CommandProperties(
        trigger = "acc",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        emoji = "\uD83D\uDE4B",
        executableWithoutArgs = true,
        aliases = {"profile", "profil", "account", "balance", "fish", "bal"}
)
public class AccountCommand extends FisheryAbstract {

    public AccountCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceivedSuccessful(MessageCreateEvent event, String followedString) throws Throwable {
        Server server = event.getServer().get();
        Message message = event.getMessage();
        ArrayList<User> list = MentionUtil.getUsers(message,followedString).getList();

        if (list.size() > 5) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    TextManager.getString(getLocale(),TextManager.GENERAL,"too_many_users"))).get();
            return false;
        }
        boolean userMentioned = true;
        boolean userBefore = list.size() > 0;
        list.removeIf(User::isBot);
        if (list.size() == 0) {
            if (userBefore) {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "nobot"))).get();
                return false;
            } else {
                list.add(message.getUserAuthor().get());
                userMentioned = false;
            }
        }
        for(User user: list) {
            EmbedBuilder eb = DBFishery.getInstance().getBean(event.getServer().get().getId()).getUserBean(user.getId()).getAccountEmbed();
            if (eb != null) {
                if (!userMentioned) {
                    eb.setFooter(TextManager.getString(getLocale(), TextManager.GENERAL, "mention_optional"));
                    if (followedString.length() > 0)
                        EmbedUtil.addNoResultsLog(eb, getLocale(), followedString);
                }

                event.getChannel().sendMessage(eb).get();
            }
        }
        return true;
    }

}
