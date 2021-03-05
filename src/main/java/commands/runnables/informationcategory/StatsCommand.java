package commands.runnables.informationcategory;

import commands.listeners.CommandProperties;

import commands.Command;
import constants.ExternalLinks;
import constants.PermissionDeprecated;
import core.*;
import core.utils.BotUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import mysql.modules.survey.DBSurvey;
import mysql.modules.tracker.DBTracker;
import mysql.modules.version.DBVersion;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Locale;

@CommandProperties(
        trigger = "stats",
        botPermissions = PermissionDeprecated.USE_EXTERNAL_EMOJIS,
        emoji = "\uD83D\uDCCA",
        executableWithoutArgs = true,
        onlyPublicVersion = true,
        aliases = { "info" }
)
public class StatsCommand extends Command {

    public StatsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        String dephordName = ShardManager.getInstance().fetchUserById(303085910784737281L).get().map(User::getDiscriminatedName).orElse("???");
        String neverCookFirstName = ShardManager.getInstance().fetchUserById(298153126223937538L).get().map(User::getDiscriminatedName).orElse("???");
        String owner = ShardManager.getInstance().fetchOwner().get().getDiscriminatedName();

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(
                this,
                getString(
                        "template",
                        owner,
                        ExternalLinks.BOT_INVITE_URL,
                        BotUtil.getCurrentVersion(),
                        TimeUtil.getInstantString(getLocale(), DBVersion.getInstance().getBean().getCurrentVersion().getDate(), true),
                        ShardManager.getInstance().getGlobalGuildSize().map(StringUtil::numToString).orElse("-"),
                        StringUtil.numToString(DBTracker.getInstance().getBean().getSlots().size()),
                        owner,
                        StringUtil.numToString(DBSurvey.getInstance().getCurrentSurvey().getFirstVoteNumber())
                ) +
                        "\n\n" +
                        getString("translator", dephordName, neverCookFirstName)
        );

        event.getServerTextChannel().get().sendMessage(eb).get();
        return true;
    }

}
