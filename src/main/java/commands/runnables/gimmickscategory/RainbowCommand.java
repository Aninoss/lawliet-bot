package commands.runnables.gimmickscategory;

import commands.listeners.CommandProperties;
import commands.runnables.UserAccountAbstract;
import constants.Permission;
import core.EmbedFactory;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import modules.graphics.RainbowGraphics;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;

import java.util.Locale;

@CommandProperties(
        trigger = "rainbow",
        botPermissions = Permission.ATTACH_FILES,
        withLoadingBar = true,
        emoji = "\uD83C\uDF08",
        executableWithoutArgs = true,
        aliases = {"lgbt", "pride"}
)
public class RainbowCommand extends UserAccountAbstract {

    private long opacity;

    public RainbowCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected void init(MessageCreateEvent event, String followedString) throws Throwable {
        opacity = StringUtil.filterLongFromString(followedString);
        if (opacity == -1) opacity = 50;
        if (opacity < 0) opacity = 0;
        if (opacity > 100) opacity = 100;
    }

    @Override
    protected EmbedBuilder generateUserEmbed(Server server, User user, boolean userIsAuthor, String followedString) throws Throwable {
        return EmbedFactory.getEmbedDefault(this,getString("template",user.getDisplayName(server)))
                .setImage(RainbowGraphics.createImageRainbow(user, opacity));
    }

    @Override
    protected void afterMessageSend(Message message, User user, boolean userIsAuthor) throws Throwable {
        if (message != null) {
            message.getEmbeds().get(0).getImage().ifPresent(image -> {
                String urlString = image.getUrl().toString();
                EmbedBuilder eb = EmbedFactory.getEmbedDefault().setDescription(getString("template2", urlString));
                EmbedUtil.setFooter(eb, this);
                message.getServerTextChannel().get().sendMessage(eb).exceptionally(ExceptionLogger.get());
            });
        }
    }

}
