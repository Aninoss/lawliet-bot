package Commands.Management;

import CommandListeners.CommandProperties;
import CommandListeners.onNavigationListener;
import CommandListeners.onTrackerRequestListener;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import Constants.*;
import General.*;
import General.EmojiConnection.BackEmojiConnection;
import General.EmojiConnection.EmojiConnection;
import General.Tracker.TrackerData;
import General.Tracker.TrackerManager;
import MySQL.DBBot;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.time.Instant;
import java.util.ArrayList;

@CommandProperties(
    trigger = "tracker",
    botPermissions = Permission.USE_EXTERNAL_EMOJIS_IN_TEXT_CHANNEL,
    userPermissions = Permission.MANAGE_SERVER,
    thumbnail = "http://icons.iconarchive.com/icons/graphicloads/colorful-long-shadow/128/Favourite-2-icon.png",
    emoji = "\uD83D\uDD16",
    executable = true
)
public class TrackerCommand extends Command implements onNavigationListener {

    private ArrayList<EmojiConnection> emojiConnections;
    private Server server;
    private ServerTextChannel channel;
    private ArrayList<TrackerData> trackers;
    private onTrackerRequestListener command;
    private String commandTrigger;
    private boolean override;

    public TrackerCommand() {
        super();
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state, boolean firstTime) throws Throwable {
        if (firstTime) {
            server = event.getServer().get();
            channel = event.getServerTextChannel().get();
        }

        if (firstTime || state == 3) {
            controll(inputString, state, firstTime);
            return Response.TRUE;
        }
        return null;
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
        for (EmojiConnection emojiConnection: emojiConnections) {
            if (emojiConnection.isEmoji(event.getEmoji()) || (i == -1 && emojiConnection instanceof BackEmojiConnection)) {
                if (emojiConnection.getConnection().equalsIgnoreCase("back")) {
                    switch (state) {
                        case 0:
                            deleteNavigationMessage();
                            return true;

                        case 1:
                            setState(0);
                            return true;

                        case 2:
                            setState(0);
                            return true;

                        case 3:
                            setState(1);
                            return true;
                    }
                }
                controll(emojiConnection.getConnection(), state,false);
                return true;
            }
        }

        return false;
    }

    private void controll(String searchTerm, int state, boolean first) throws Throwable {
        while(true) {
            if (searchTerm.replace(" ", "").length() == 0) return;
            String arg = searchTerm.split(" ")[0].toLowerCase();

            switch (state) {
                case 0:
                    if (arg.equalsIgnoreCase("add")) {
                        updateTrackerList();
                        if (trackers.size() < 6) {
                            state = 1;
                            setState(1);
                        } else {
                            setLog(LogStatus.FAILURE, getString("state0_toomanytracker", "6"));
                            return;
                        }
                    }
                    else if (arg.equalsIgnoreCase("remove")) {
                        updateTrackerList();
                        if (trackers.size() > 0) {
                            state = 2;
                            setState(2);
                        } else {
                            setLog(LogStatus.FAILURE, getString("state0_notracker"));
                            return;
                        }
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "invalid", arg));
                        return;
                    }
                    break;

                case 1:
                    boolean found = false;
                    for (onTrackerRequestListener command : CommandContainer.getInstance().getTrackerCommands()) {
                        String trigger = ((Command) command).getTrigger();

                        if (trigger.equalsIgnoreCase(arg)) {
                            updateTrackerList();
                            TrackerData trackerRemove = getTracker(arg);
                            override = trackerRemove != null;
                            if (override) {
                                if (!command.trackerUsesKey()) {
                                    setLog(LogStatus.FAILURE, getString("state1_alreadytracking"));
                                    return;
                                }
                                TrackerManager.stopTracker(trackerRemove, true);
                            }
                            this.command = command;
                            this.commandTrigger = trigger;
                            if (!command.trackerUsesKey()) {
                                addTracker(null);
                                if (first) endNavigation();
                            } else {
                                updateTrackerList();
                                state = 3;
                                setState(3);
                            }
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "invalid", arg));
                        return;
                    }
                    break;

                case 2:
                    TrackerData trackerRemove = getTracker(arg);
                    if (trackerRemove != null) {
                        DBBot.removeTracker(trackerRemove);
                        TrackerManager.stopTracker(trackerRemove, true);
                        updateTrackerList();
                        setLog(LogStatus.SUCCESS, getString("state2_removed", arg));
                        if (trackers.size() == 0) {
                            setState(0);
                        }
                        if (first) endNavigation();
                        return;
                    }
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "invalid", arg));
                    return;

                case 3:
                    addTracker(searchTerm);
                    if (first) endNavigation();
                    return;
            }

            searchTerm = Tools.cutSpaces(searchTerm.substring(arg.length()));
        }
    }

    private void addTracker(String key) throws Throwable {
        if (!Bot.isDebug()) {
            TrackerData trackerData = new TrackerData(server, channel, 0, commandTrigger, key, Instant.now(), null);
            TrackerManager.startTracker(trackerData);
            setState(1);
            setLog(LogStatus.SUCCESS, getString("state3_added", override, commandTrigger));
        }
    }

    private void endNavigation() {
        removeNavigation();
        setState(4);
    }

    private void updateTrackerList() throws Throwable {
        ArrayList<TrackerData> newTrackers = new ArrayList<>();
        for(TrackerData trackerData: DBBot.getTracker(server.getApi())) {
            if (trackerData.getServerId() == server.getId() && trackerData.getChannelId() == channel.getId()) {
                newTrackers.add(trackerData);
            }
        }
        trackers = newTrackers;
    }

    private TrackerData getTracker(String trigger) {
        for(TrackerData trackerData: trackers) {
            if (trackerData.getCommand().equalsIgnoreCase(trigger)) {
                return trackerData;
            }
        }
        return null;
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        ServerTextChannel channel = getStarterMessage().getServerTextChannel().get();
        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));
                emojiConnections = new ArrayList<>();
                emojiConnections.add(new BackEmojiConnection(channel, "back"));
                emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[0], "add"));
                emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[1], "remove"));
                return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description"));

            case 1:
                setOptions(new String[CommandContainer.getInstance().getTrackerCommands().size()]);
                emojiConnections = new ArrayList<>();
                emojiConnections.add(new BackEmojiConnection(channel, "back"));
                for (int i=0; i < getOptions().length; i++) {
                    String trigger = ((Command) CommandContainer.getInstance().getTrackerCommands().get(i)).getTrigger();
                    getOptions()[i] = trigger + " - " + TextManager.getString(getLocale(), TextManager.COMMANDS, trigger + "_description");
                    emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[i], trigger));
                }
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description"), getString("state1_title"));

            case 2:
                setOptions(new String[trackers.size()]);
                emojiConnections = new ArrayList<>();
                emojiConnections.add(new BackEmojiConnection(channel, "back"));
                for (int i=0; i < getOptions().length; i++) {
                    String trigger = trackers.get(i).getCommand();
                    getOptions()[i] = trigger + " - " + TextManager.getString(getLocale(), TextManager.COMMANDS, trigger + "_description");;
                    emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[i], trigger));
                }
                return EmbedFactory.getCommandEmbedStandard(this, getString("state2_description"), getString("state2_title"));

            case 3:
                emojiConnections = new ArrayList<>();
                emojiConnections.add(new BackEmojiConnection(channel, "back"));
                EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, TextManager.getString(getLocale(), TextManager.COMMANDS,  commandTrigger + "_trackerkey"), getString("state3_title"));
                if (override) EmbedFactory.addLog(eb, null, getString("state3_override"));

                return eb;

            case 4:
                return EmbedFactory.getCommandEmbedSuccess(this, getString("state4_description"));
        }

        return null;
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return CommandContainer.getInstance().getTrackerCommands().size();
    }
}
