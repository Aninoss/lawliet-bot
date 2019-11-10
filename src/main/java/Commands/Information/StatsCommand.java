package Commands.Information;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Permission;
import Constants.Settings;
import General.*;
import General.Fishing.FishingRecords;
import General.Tracker.TrackerManager;
import MySQL.DBBot;
import MySQL.DBSurvey;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

@CommandProperties(
        trigger = "stats",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS_IN_TEXT_CHANNEL,
        emoji = "\uD83D\uDCCA",
        thumbnail = "http://icons.iconarchive.com/icons/webalys/kameleon.pics/128/Graph-Magnifier-icon.png",
        executable = true,
        aliases = {"botstats"}
)
public class StatsCommand extends Command implements onRecievedListener {

    public StatsCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        User dephord = DiscordApiCollection.getInstance().getUserById(303085910784737281L).get();

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this,
                getString("template",
                DiscordApiCollection.getInstance().getOwner().getMentionTag(),
                Settings.BOT_INVITE_URL,
                Tools.getCurrentVersion(),
                Tools.getInstantString(getLocale(), DBBot.getCurrentVersionDate(), true),
                Tools.numToString(getLocale(), DiscordApiCollection.getInstance().getServerTotalSize()),
                Tools.numToString(getLocale(), TrackerManager.getSize()),
                DiscordApiCollection.getInstance().getOwner().getDiscriminatedName(),
                Tools.numToString(getLocale(), DBSurvey.getCurrentVotesNumber())
                ) +
                "\n\n" +
                getString("translator", dephord.getMentionTag(), dephord.getDiscriminatedName()));


        FishingRecords fishingRecords = DBBot.getFishingRecords();
        String args[] = new String[12];

        for(int i=0; i<4; i++) {
            for(int j=0; j<3; j++) {
                int k = i * 3 + j;
                if (j == 0) args[k] = fishingRecords.getUser(i).getDiscriminatedName();
                if (j == 1) args[k] = fishingRecords.getServer(i).getName();
                if (j == 2) args[k] = Tools.numToString(getLocale(), fishingRecords.getValue(i));
                args[k] = args[k].replace("_", "").replace("*", "").replace("~", "-").replace("\\`", "'").replace("||", "II");
                if (args[k].length() == 0) args[k] = " ";
            }
        }

        eb.addField(getString("fishingrecords_title"),getString("fishingrecords_template", args));
        event.getServerTextChannel().get().sendMessage(eb).get();
        return true;
    }
}
