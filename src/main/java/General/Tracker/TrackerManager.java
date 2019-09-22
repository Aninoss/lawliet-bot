package General.Tracker;

import CommandListeners.onTrackerRequestListener;
import CommandSupporters.Command;
import CommandSupporters.CommandManager;
import Constants.Permission;
import General.PermissionCheck;
import General.TextManager;
import MySQL.DBBot;
import MySQL.DBServer;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;

public class TrackerManager {
    private static ArrayList<TrackerConnection> trackerConnections = new ArrayList<>();

    public static void manageTracker(TrackerData trackerData) throws Throwable {
        Locale locale = DBServer.getServerLocale(trackerData.getServer());
        Command command = CommandManager.createCommandByTrigger(trackerData.getCommand(), locale);
        if (((onTrackerRequestListener) command).needsPrefix())
            command.setPrefix(DBServer.getPrefix(trackerData.getServer()));

        while (true) {
            try {
                Duration duration = Duration.between(Instant.now(), trackerData.getInstant());
                Thread.sleep(Math.max(1, duration.getSeconds() * 1000 + duration.getNano() / 1000000));

                if (!trackerData.getChannel().getCurrentCachedInstance().isPresent()) return;

                EmbedBuilder errEmbed;
                do {
                    errEmbed = PermissionCheck.bothasPermissions(command.getLocale(), trackerData.getServer(), trackerData.getChannel(), Permission.WRITE_IN_TEXT_CHANNEL | Permission.EMBED_LINKS_IN_TEXT_CHANNELS);

                    if (errEmbed != null) {
                        //System.out.println("Keine Rechte für den Tracker! Channel: " + trackerData.getVoiceChannel().getIdAsString());
                        User owner = trackerData.getServer().getOwner();
                        int RETRY_MINUTES = 30;
                        if (owner != null) {
                            owner.sendMessage(new EmbedBuilder()
                                    .setColor(Color.RED)
                                    .setTitle(TextManager.getString(locale, TextManager.GENERAL,"error"))
                                    .setDescription(TextManager.getString(locale, TextManager.GENERAL,"tracker_missing_permissions", command.getTrigger(), trackerData.getChannel().getIdAsString(), String.valueOf(RETRY_MINUTES)))).get();
                            owner.sendMessage(errEmbed).get();
                        }
                        Thread.sleep(1000 * 60 * RETRY_MINUTES);
                    }
                } while (errEmbed != null);

                TrackerData oldTrackerData = trackerData;
                trackerData = ((onTrackerRequestListener) command).onTrackerRequest(trackerData);
                if (trackerData != null) DBBot.saveTracker(trackerData);
                else {
                    trackerConnections.remove(getTrackerConnection(oldTrackerData));
                    return;
                }
            } catch (InterruptedException e) {
                //Ignore
                return;
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                Thread.sleep(5 * 60 * 1000);
            }
        }
    }

    public static void startTracker(TrackerData trackerData) throws Throwable {
        Thread thread = new Thread(() -> {
            try {
                TrackerManager.manageTracker(trackerData);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        });
        thread.start();
        trackerConnections.add(new TrackerConnection(trackerData, thread));
    }

    public static void stopTracker(TrackerData trackerData) throws Throwable {
        TrackerConnection trackerConnectionRemove = getTrackerConnection(trackerData);
        if (trackerConnectionRemove != null) {
            trackerConnectionRemove.getThread().interrupt();
            trackerConnections.remove(trackerConnectionRemove);
            DBBot.removeTracker(trackerData);
        }
    }

    public static void interruptTracker(TrackerData trackerData) throws Throwable {
        TrackerConnection trackerConnectionRemove = getTrackerConnection(trackerData);
        if (trackerConnectionRemove != null) {
            trackerConnectionRemove.getThread().interrupt();
        }
    }

    private static TrackerConnection getTrackerConnection(TrackerData trackerData) {
        if (trackerData == null) return null;

        for(TrackerConnection trackerConnection: trackerConnections) {
            TrackerData trackerData2 = trackerConnection.getTrackerData();
            if (trackerData.getServer().getId() == trackerData2.getServer().getId() && trackerData.getChannel().getId() == trackerData2.getChannel().getId() && trackerData.getCommand().equals(trackerData2.getCommand())) {
                return trackerConnection;
            }
        }
        return null;
    }

    public static int getSize() {
        return trackerConnections.size();
    }
}
