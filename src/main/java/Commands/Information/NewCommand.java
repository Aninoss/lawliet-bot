package Commands.Information;

import CommandListeners.*;
import CommandSupporters.Command;
import General.*;
import General.Tracker.TrackerData;
import General.Tracker.TrackerManager;
import MySQL.DBBot;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
    trigger = "new",
    emoji = "\uD83C\uDD95",
    thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat/128/new-icon.png",
    executable = true
)
public class NewCommand extends Command implements onRecievedListener, onTrackerRequestListener {

    public NewCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        //Ohne Argumente
        if (followedString.length() == 0) {
            ArrayList<String> versions = DBBot.getCurrentVersions(3);
            event.getChannel().sendMessage(getVersionsEmbed(versions)).get();
            return true;
        } else {
            //Anzahl
            if (Tools.stringIsNumeric(followedString)) {
                int i = Integer.parseInt(followedString);
                if (i >= 1) {
                    if (i <= 10) {
                        ArrayList<String> versions = DBBot.getCurrentVersions(i);
                        event.getChannel().sendMessage(getVersionsEmbed(versions)).get();
                        return true;
                    } else {
                        event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                                TextManager.getString(getLocale(), TextManager.GENERAL,"too_large", "10"))).get();
                        return false;
                    }
                } else {
                    event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                            TextManager.getString(getLocale(), TextManager.GENERAL,"too_small", "1"))).get();
                    return false;
                }
            } else {
                ArrayList<String> versions = DBBot.getCurrentVersions(followedString.split(" "));

                if (versions.size() > 0) {
                    event.getChannel().sendMessage(getVersionsEmbed(versions)).get();
                    return true;
                } else {
                    event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", followedString))).get();
                    return false;
                }
            }
        }
    }

    private EmbedBuilder getVersionsEmbed(ArrayList<String> versions) throws Throwable {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this).setFooter(getString("footer"));
        for(String str: versions) {
            eb.addField(str, ("• "+TextManager.getString(getLocale(), TextManager.VERSIONS, str)).replace("\n", "\n• ").replace("%PREFIX", getPrefix()));
        }
        return eb;
    }

    @Override
    public TrackerData onTrackerRequest(TrackerData trackerData) throws Throwable {
        if (trackerData.getMessageDelete() == null || trackerData.getMessageDelete().getCreationTimestamp().isBefore(DBBot.getCurrentVersionDate())) {
            ArrayList<String> versions = DBBot.getCurrentVersions(1);
            Message message = trackerData.getChannel().sendMessage(getVersionsEmbed(versions)).get();
            trackerData.setMessageDelete(message);
        }

        TrackerManager.interruptTracker(trackerData);
        return trackerData;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

    @Override
    public boolean needsPrefix() {
        return true;
    }
}
