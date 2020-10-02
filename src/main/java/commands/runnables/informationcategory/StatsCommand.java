package commands.runnables.informationcategory;

import commands.listeners.CommandProperties;

import commands.Command;
import constants.ExternalLinks;
import constants.Permission;
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
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        emoji = "\uD83D\uDCCA",
        executable = true,
        aliases = {"info"}
)
public class StatsCommand extends Command {

    public StatsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        String dephordName = DiscordApiCollection.getInstance().getUserById(303085910784737281L).map(User::getDiscriminatedName).orElse("???");
        String neverCookFirstName = DiscordApiCollection.getInstance().getUserById(298153126223937538L).map(User::getDiscriminatedName).orElse("???");

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this,
                getString("template",
                DiscordApiCollection.getInstance().getOwner().getMentionTag(),
                ExternalLinks.BOT_INVITE_URL,
                BotUtil.getCurrentVersion(),
                TimeUtil.getInstantString(getLocale(), DBVersion.getInstance().getBean().getCurrentVersion().getDate(), true),
                StringUtil.numToString(getLocale(), DiscordApiCollection.getInstance().getServerTotalSize()),
                StringUtil.numToString(getLocale(), DBTracker.getInstance().getBean().getSlots().size()),
                        StringUtil.escapeMarkdown(DiscordApiCollection.getInstance().getOwner().getDiscriminatedName()),
                StringUtil.numToString(getLocale(), DBSurvey.getInstance().getCurrentSurvey().getFirstVoteNumber())
                ) +
                "\n\n" +
                getString("translator", dephordName, neverCookFirstName));

        event.getServerTextChannel().get().sendMessage(eb).get();
        return true;
    }

}
