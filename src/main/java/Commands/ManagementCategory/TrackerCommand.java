package Commands.ManagementCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnNavigationListener;
import CommandListeners.OnTrackerRequestListener;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import CommandSupporters.CommandManager;
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
        emoji = "\uD83D\uDD16",
        executable = true,
        aliases = {"track", "tracking"}
)
public class TrackerCommand extends Command implements OnNavigationListener {

    private ArrayList<EmojiConnection> emojiConnections;
    private long serverId, channelId;
    private ArrayList<TrackerBeanSlot> trackerSlots;
    private String commandTrigger, commandCategory;
    private boolean override;

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        serverId = event.getServer().get().getId();
        channelId = event.getServerTextChannel().get().getId();
        controll(followedString, 0);
        return true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable {
        if (state == 3) {
            controll(inputString, state);
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
                            removeNavigationWithMessage();
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
                controll(emojiConnection.getConnection(), state);
                return true;
            }
        }

        return false;
    }

    private void controll(String searchTerm, int state) throws Throwable {
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
                    for (Class<? extends OnTrackerRequestListener> clazz : CommandContainer.getInstance().getTrackerCommands()) {
                        OnTrackerRequestListener command = (OnTrackerRequestListener) CommandManager.createCommandByClass((Class<? extends Command>)clazz);
                        String trigger = ((Command) command).getTrigger();
                        String category = ((Command) command).getCategory();

                        if (trigger.equalsIgnoreCase(arg)) {
                            updateTrackerList();
                            TrackerBeanSlot slot = getTracker(arg);
                            override = slot != null;
                            if (override) {
                                if (!command.trackerUsesKey()) {
                                    setLog(LogStatus.FAILURE, getString("state1_alreadytracking"));
                                    return;
                                }
                            }
                            this.commandTrigger = trigger;
                            this.commandCategory = category;
                            if (!command.trackerUsesKey()) {
                                addTracker(null);
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
                        updateTrackerList();
                        setLog(LogStatus.SUCCESS, getString("state2_removed", arg));
                        if (trackerSlots.size() == 0) {
                            setState(0);
                        }
                        return;
                    }
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "invalid", arg));
                    return;

                case 3:
                    addTracker(searchTerm);
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
                    Class<? extends OnTrackerRequestListener> clazz = CommandContainer.getInstance().getTrackerCommands().get(i);
                    Command command = CommandManager.createCommandByClass((Class<? extends Command>) clazz);
                    String trigger = command.getTrigger();
                    opt[i] = trigger + " - " + TextManager.getString(getLocale(), command.getCategory(), trigger + "_description");
                    emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[i], trigger));
                }
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description"), getString("state1_title"));

            case 2:
                setOptions(new String[trackerSlots.size()]);
                emojiConnections = new ArrayList<>();
                emojiConnections.add(new BackEmojiConnection(channel, "back"));
                for (int i=0; i < getOptions().length; i++) {
                    Command command = CommandManager.createCommandByTrigger(trackerSlots.get(i).getCommandTrigger(), getLocale(), getPrefix());
                    String trigger = command.getTrigger();
                    getOptions()[i] = getString("slot", trackerSlots.get(i).getCommandKey().isPresent(),
                            trigger,
                            TextManager.getString(getLocale(), command.getCategory(), trigger + "_description"),
                            trackerSlots.get(i).getCommandKey().orElse("")
                            );
                    emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[i], trigger));
                }
                return EmbedFactory.getCommandEmbedStandard(this, getString("state2_description"), getString("state2_title"));

            case 3:
                emojiConnections = new ArrayList<>();
                emojiConnections.add(new BackEmojiConnection(channel, "back"));
                EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, TextManager.getString(getLocale(), commandCategory,  commandTrigger + "_trackerkey"), getString("state3_title"));
                if (override) EmbedFactory.addLog(eb, null, getString("state3_override"));

                return eb;

            case 4:
                return EmbedFactory.getCommandEmbedStandard(this, getString("state4_description"));
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
