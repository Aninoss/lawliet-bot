package commands.runnables.gimmickscategory;

import commands.listeners.CommandProperties;

import commands.Command;
import constants.Permission;
import core.*;
import core.utils.EmbedUtil;
import core.utils.MentionUtil;
import modules.graphics.TriggerGraphics;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Locale;

@CommandProperties(
        trigger = "trigger",
        botPermissions = Permission.ATTACH_FILES,
        withLoadingBar = true,
        emoji = "\uD83D\uDCA2",
        executableWithoutArgs = true,
        aliases = {"triggered"}
)
public class TriggerCommand extends Command {

    public TriggerCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Server server = event.getServer().get();
        Message message = event.getMessage();
        ArrayList<User> list = MentionUtil.getUsers(message,followedString).getList();
        if (list.size() > 5) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    TextManager.getString(getLocale(),TextManager.GENERAL,"too_many_users"))).get();
            return false;
        }
        boolean userMentioned = true;
        if (list.size() == 0) {
            list.add(message.getUserAuthor().get());
            userMentioned = false;
        }
        for (User user: list) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this,getString("template",user.getDisplayName(server)))
                    .setImage(TriggerGraphics.createImageTriggered(user), "gif");

            if (!userMentioned) {
                EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "mention_optional"));
                if (followedString.length() > 0)
                    EmbedUtil.addNoResultsLog(eb, getLocale(), followedString);
            }

            event.getChannel().sendMessage(eb).get();
        }
        return true;
    }
}
