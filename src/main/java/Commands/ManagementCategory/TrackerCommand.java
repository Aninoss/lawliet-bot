package Commands.ManagementCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnNavigationListener;
import CommandListeners.OnTrackerRequestListener;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import Constants.*;
import Core.*;
import Core.EmojiConnection.BackEmojiConnection;
import Core.EmojiConnection.EmojiConnection;
import Core.Utils.StringUtil;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Tracker.DBTracker;
import MySQL.Modules.Tracker.TrackerBeanSlot;
import javafx.util.Pair;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.stream.Collectors;

@CommandProperties(
    trigger = "tracker",
    botPermissions = Permission.USE_EXTERNAL_EMOJIS,
    userPermissions = Permission.MANAGE_SERVER,
    thumbnail = "http://icons.iconarchive.com/icons/graphicloads/colorful-long-shadow/128/Favourite-2-icon.png",
    emoji = "\uD83D\uDD16",
    executable = true
)
public class TrackerCommand extends Command implements OnNavigationListener {

    private ArrayList<EmojiConnection> emojiConnections;
    private long serverId, channelId;
    private ArrayList<TrackerBeanSlot> trackerSlots;
    private String commandTrigger;
    private boolean override;

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        serverId = event.getServer().get().getId();
        channelId = event.getServerTextChannel().get().getId();
        controll(followedString, 0, true);
        return true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable {
        if (state == 3) {
            controll(inputString, state, false);
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
                            return false;

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
                        if (trackerSlots.size() < 6) {
                            state = 1;
                            setState(1);
                        } else {
                            setLog(LogStatus.FAILURE, getString("state0_toomanytracker", "6"));
                            return;
                        }
                    }
                    else if (arg.equalsIgnoreCase("remove")) {
                        updateTrackerList();
                        if (trackerSlots.size() > 0) {
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
                    for (OnTrackerRequestListener command : CommandContainer.getInstance().getTrackerCommands()) {
                        String trigger = ((Command) command).getTrigger();

                        if (trigger.equalsIgnoreCase(arg)) {
                            updateTrackerList();
                            TrackerBeanSlot slot = getTracker(arg);
                            override = slot != null;
                            if (override) {
                                if (!command.trackerUsesKey()) {
                                    setLog(LogStatus.FAILURE, getString("state1_alreadytracking"));
                                    return;
                                }
                                slot.stop();
                            }
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
                    TrackerBeanSlot slotRemove = getTracker(arg);
                    if (slotRemove != null) {
                        slotRemove.delete();
                        slotRemove.stop();
                        updateTrackerList();
                        setLog(LogStatus.SUCCESS, getString("state2_removed", arg));
                        if (trackerSlots.size() == 0) {
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

            searchTerm = StringUtil.trimString(searchTerm.substring(arg.length()));
        }
    }

    private void addTracker(String key) throws Throwable {
        TrackerBeanSlot slot = new TrackerBeanSlot(
                DBServer.getInstance().getBean(serverId),
                channelId,
                commandTrigger,
                null,
                key,
                Instant.now(),
                null
        );
        DBTracker.getInstance().getBean().getMap().put(new Pair<>(channelId, commandTrigger), slot);
        setState(1);
        setLog(LogStatus.SUCCESS, getString("state3_added", override, commandTrigger));
    }

    private void endNavigation() {
        removeNavigation();
        setState(4);
    }

    private void updateTrackerList() throws Throwable {
        trackerSlots = DBTracker.getInstance().getBean().getMap().values().stream()
                .filter(slot -> slot.getChannelId() == channelId)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private TrackerBeanSlot getTracker(String trigger) {
        for(TrackerBeanSlot slot: trackerSlots) {
            if (slot.getCommandTrigger().equalsIgnoreCase(trigger)) {
                return slot;
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
                String[] opt = new String[CommandContainer.getInstance().getTrackerCommands().size()];
                setOptions(opt);
                emojiConnections = new ArrayList<>();
                emojiConnections.add(new BackEmojiConnection(channel, "back"));
                for (int i = 0; i < opt.length; i++) {
                    String trigger = ((Command) CommandContainer.getInstance().getTrackerCommands().get(i)).getTrigger();
                    opt[i] = trigger + " - " + TextManager.getString(getLocale(), TextManager.COMMANDS, trigger + "_description");
                    emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[i], trigger));
                }
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description"), getString("state1_title"));

            case 2:
                setOptions(new String[trackerSlots.size()]);
                emojiConnections = new ArrayList<>();
                emojiConnections.add(new BackEmojiConnection(channel, "back"));
                for (int i=0; i < getOptions().length; i++) {
                    String trigger = trackerSlots.get(i).getCommandTrigger();
                    getOptions()[i] = getString("slot", trackerSlots.get(i).getCommandKey().isPresent(),
                            trigger,
                            TextManager.getString(getLocale(), TextManager.COMMANDS, trigger + "_description"),
                            trackerSlots.get(i).getCommandKey().orElse("")
                            );
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
