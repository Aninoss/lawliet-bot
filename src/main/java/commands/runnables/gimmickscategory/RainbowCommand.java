package commands.runnables.gimmickscategory;

import commands.listeners.CommandProperties;

import commands.Command;
import constants.Permission;
import core.*;
import core.mention.MentionList;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.ImageCreator;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Locale;

@CommandProperties(
        trigger = "rainbow",
        botPermissions = Permission.ATTACH_FILES,
        withLoadingBar = true,
        emoji = "\uD83C\uDF08",
        executable = true,
        aliases = {"lgbt", "pride"}
)
public class RainbowCommand extends Command {

    public RainbowCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Server server = event.getServer().get();
        Message message = event.getMessage();
        MentionList<User> userMention = MentionUtil.getUsers(message,followedString);
        ArrayList<User> list = userMention.getList();
        if (list.size() > 5) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    TextManager.getString(getLocale(),TextManager.GENERAL,"too_many_users"))).get();
            return false;
        }

        long opacity = StringUtil.filterLongFromString(userMention.getResultMessageString());
        if (opacity == -1) opacity = 50;
        if (opacity < 0) opacity = 0;
        if (opacity > 100) opacity = 100;

        boolean userMentioned = true;
        if (list.size() == 0) {
            list.add(message.getUserAuthor().get());
            userMentioned = false;
        }

        for (User user: list) {
            EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this,getString("template",user.getDisplayName(server)))
                    .setImage(ImageCreator.createImageRainbow(user, opacity));

            if (!userMentioned) {
                eb.setFooter(TextManager.getString(getLocale(), TextManager.GENERAL, "mention_optional"));
                if (StringUtil.filterLettersFromString(followedString).length() > 0)
                    EmbedFactory.addNoResultsLog(eb, getLocale(), followedString);
            }

            Message message1 = event.getChannel().sendMessage(eb).get();

            if (message1 != null) {
                String url = message1.getEmbeds().get(0).getImage().get().getUrl().toString();
                eb = EmbedFactory.getEmbed().setDescription(getString("template2", url));
                event.getChannel().sendMessage(eb).get();
            }
        }
        return true;
    }
}
