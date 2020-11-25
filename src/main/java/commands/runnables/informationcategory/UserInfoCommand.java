package commands.runnables.informationcategory;

import commands.listeners.CommandProperties;
import commands.runnables.UserAccountAbstract;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.util.Locale;

@CommandProperties(
        trigger = "userinfo",
        emoji = "\uD83D\uDC81",
        executableWithoutArgs = true,
        aliases = {"userinfos", "userstat", "userstats", "accountinfo", "whois"}
)
public class UserInfoCommand extends UserAccountAbstract {

    public UserInfoCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder generateUserEmbed(Server server, User user, boolean userIsAuthor, String followedString) throws Throwable {
        String[] type = getString("type").split("\n");
        int typeN = 0;
        if (!user.isBot()) {
            typeN = 1;
            if (server.getOwnerId() == user.getId()) typeN = 2;
            if (user.isBotOwner()) typeN = 3;
        }

        String[] args = {
                type[typeN],
                StringUtil.escapeMarkdown(user.getName()),
                user.getNickname(server).isPresent() ? StringUtil.escapeMarkdown(user.getNickname(server).get()) : "-",
                user.getDiscriminator(),
                user.getIdAsString(),
                user.getAvatar().getUrl().toString() + "?size=2048",
                user.getJoinedAtTimestamp(server).isPresent() ? TimeUtil.getInstantString(getLocale(), user.getJoinedAtTimestamp(server).get(), true) : "-",
                TimeUtil.getInstantString(getLocale(), user.getCreationTimestamp(), true),
                TextManager.getString(getLocale(), TextManager.GENERAL, "status_" + user.getStatus().getStatusString())
        };

        return EmbedFactory.getEmbedDefault(this, getString("template", args)).
                setThumbnail(user.getAvatar().getUrl().toString());
    }

}
