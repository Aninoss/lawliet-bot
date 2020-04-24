package Commands.InformationCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Constants.Permission;
import Constants.Settings;
import Core.*;
import Core.Utils.StringUtil;
import Core.Utils.TimeUtil;
import MySQL.Modules.Survey.DBSurvey;
import MySQL.Modules.Tracker.DBTracker;
import MySQL.Modules.Version.DBVersion;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

@CommandProperties(
        trigger = "stats",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        emoji = "\uD83D\uDCCA",
        thumbnail = "http://icons.iconarchive.com/icons/webalys/kameleon.pics/128/Graph-Magnifier-icon.png",
        executable = true,
        aliases = {"info"}
)
public class StatsCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        User dephord = DiscordApiCollection.getInstance().getUserById(303085910784737281L).get();

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this,
                getString("template",
                DiscordApiCollection.getInstance().getOwner().getMentionTag(),
                Settings.BOT_INVITE_URL,
                StringUtil.getCurrentVersion(),
                TimeUtil.getInstantString(getLocale(), DBVersion.getInstance().getBean().getCurrentVersion().getDate(), true),
                StringUtil.numToString(getLocale(), DiscordApiCollection.getInstance().getServerTotalSize()),
                StringUtil.numToString(getLocale(), DBTracker.getInstance().getBean().getMap().size()),
                DiscordApiCollection.getInstance().getOwner().getDiscriminatedName(),
                StringUtil.numToString(getLocale(), DBSurvey.getInstance().getCurrentSurvey().getFirstVoteNumber())
                ) +
                "\n\n" +
                getString("translator", dephord.getMentionTag(), dephord.getDiscriminatedName()));

        event.getServerTextChannel().get().sendMessage(eb).get();
        return true;
    }

}
