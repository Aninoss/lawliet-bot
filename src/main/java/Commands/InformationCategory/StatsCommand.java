package Commands.InformationCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Constants.Permission;
import Constants.Settings;
import Core.*;
import Core.Utils.BotUtil;
import Core.Utils.StringUtil;
import Core.Utils.TimeUtil;
import MySQL.Modules.Survey.DBSurvey;
import MySQL.Modules.Tracker.DBTracker;
import MySQL.Modules.Version.DBVersion;
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
                Settings.BOT_INVITE_URL,
                BotUtil.getCurrentVersion(),
                TimeUtil.getInstantString(getLocale(), DBVersion.getInstance().getBean().getCurrentVersion().getDate(), true),
                StringUtil.numToString(getLocale(), DiscordApiCollection.getInstance().getServerTotalSize()),
                StringUtil.numToString(getLocale(), DBTracker.getInstance().getBean().getMap().size()),
                        StringUtil.escapeMarkdown(DiscordApiCollection.getInstance().getOwner().getDiscriminatedName()),
                StringUtil.numToString(getLocale(), DBSurvey.getInstance().getCurrentSurvey().getFirstVoteNumber())
                ) +
                "\n\n" +
                getString("translator", dephordName, neverCookFirstName));

        event.getServerTextChannel().get().sendMessage(eb).get();
        return true;
    }

}
