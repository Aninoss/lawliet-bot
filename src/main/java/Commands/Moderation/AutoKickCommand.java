package Commands.Moderation;

import CommandListeners.*;
import CommandSupporters.Command;
import Constants.Permission;
import General.EmbedFactory;
import General.ExceptionHandler;
import General.Tools;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

@CommandProperties(
    trigger = "autokick",
    botPermissions = Permission.KICK_USER | Permission.READ_MESSAGE_HISTORY_OF_TEXT_CHANNEL,
    userPermissions = Permission.KICK_USER,
    emoji = "\uD83D\uDEAA",
    thumbnail = "http://icons.iconarchive.com/icons/elegantthemes/beautiful-flat/128/door-icon.png",
    executable = false
)
public class AutoKickCommand extends Command implements onRecievedListener, onReactionAddListener {
    
    private int stage = 0;
    private Message message;
    private ArrayList<User> banList;
    private static ArrayList<Server> serverBlockList = new ArrayList<>();

    public AutoKickCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        if (!serverBlockList.contains(event.getServer().get())) {
            if (followedString.length() > 0 && Tools.stringIsNumeric(followedString) && Integer.parseInt(followedString) >= 1) {
                serverBlockList.add(event.getServer().get());
                int n = Integer.parseInt(followedString);

                message = event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("deleting", Tools.getLoadingReaction(event.getServerTextChannel().get())))).get();
                message.addReaction(Tools.getEmojiForBoolean(false)).get();
                stage = 0;

                final AutoKickCommand thisInstance = this;
                Thread t= new Thread(() -> {
                    try {
                        Server server = event.getServer().get();
                        HashMap<User, Instant> map = new HashMap<>();
                        for (User user : server.getMembers()) {
                            map.put(user, user.getJoinedAtTimestamp(server).get());
                        }

                        for (ServerTextChannel channel : server.getTextChannels()) {
                            if (channel.canYouReadMessageHistory()) {
                                MessageSet messagesStart = channel.getMessages(1).get();
                                if (messagesStart.getNewestMessage().isPresent()) {
                                    Message startMessage = messagesStart.getNewestMessage().get();
                                    manageMessage(message, map, n);
                                    MessageSet messageSet;
                                    do {
                                        if (startMessage.getCreationTimestamp() != null && Instant.now().minusSeconds(60 * 60 * 24 * n).isAfter(startMessage.getCreationTimestamp()))
                                            break;
                                        messageSet = channel.getMessagesBefore(500, startMessage).get();
                                        for (Message message : messageSet) {
                                            if (message != null) {
                                                manageMessage(message, map, n);
                                            }
                                        }
                                        if (messageSet.size() > 0)
                                            startMessage = messageSet.getOldestMessage().get();
                                    } while (messageSet.size() >= 500 && stage == 0);
                                }
                                if (stage != 0) {
                                    return;
                                }
                            }
                        }

                        banList = new ArrayList<>();
                        for (User user : server.getMembers()) {
                            Instant instant = map.get(user);
                            if (Instant.now().minusSeconds(60 * 60 * 24 * n).isAfter(instant) && !user.isBot() && Tools.canYouKickUser(server, user)) {
                                banList.add(user);
                            }
                        }

                        if (banList.size() > 0) {
                            stage = 1;
                            message.edit(EmbedFactory.getCommandEmbedSuccess(thisInstance, getString("finished", banList.size() != 1, Tools.numToString(getLocale(), banList.size())))).get();
                            message.addReaction(Tools.getEmojiForBoolean(true)).get();
                            addReactionListener(message);
                        } else {
                            stage = -1;
                            message.edit(EmbedFactory.getCommandEmbedSuccess(thisInstance, getString("finished_noresults"))).get();
                            serverBlockList.remove(event.getServer().get());
                            removeReactionListener();
                        }
                    } catch (Throwable e) {
                        ExceptionHandler.handleException(e, getLocale(), event.getServerTextChannel().get());
                    }
                });
                t.setName("autokick_processor");
                t.start();
                return true;
            } else {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                        getString("wrong_args"))).get();
                removeReactionListener();
                return false;
            }
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, getString("used"), getString("used_title"))).get();
            removeReactionListener();
            return false;
        }
    }

    private static void manageMessage(Message message, HashMap<User, Instant> map, int n) {
        if (!message.getUserAuthor().isPresent()) return;
        User user = message.getUserAuthor().get();
        if (message.getServer().get().getMembers().contains(user)) {
            Instant instant = map.get(user);
            Instant messageInstant = message.getCreationTimestamp();
            if (messageInstant != null && messageInstant.isAfter(instant)) {
                map.put(user, message.getCreationTimestamp());
            }
        }
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        //event.removeReaction();

        if (event.getEmoji().isUnicodeEmoji()) {
            if (stage == 0) {
                if (event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(Tools.getEmojiForBoolean(false))) {
                    stage = -1;
                    removeReactionListener();
                    message.edit(EmbedFactory.getCommandEmbedError(this, getString("abort"), getString("abort_title"))).get();
                    serverBlockList.remove(event.getServer().get());
                }
            }

            else if (stage == 1) {
                if (event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(Tools.getEmojiForBoolean(false))) {
                    stage = -1;
                    removeReactionListener();
                    message.edit(EmbedFactory.getCommandEmbedError(this, getString("abort"), getString("abort_title"))).get();
                    serverBlockList.remove(event.getServer().get());
                } else if (event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(Tools.getEmojiForBoolean(true))) {
                    stage = -1;
                    removeReactionListener();

                    int kicked = 0;
                    for(User user: banList) {
                        try {
                            user.sendMessage(getString("usermessage", event.getServer().get().getName())).get();
                        } catch (InterruptedException | ExecutionException e) {
                            //Ignore
                        }

                        try {
                            message.getServer().get().kickUser(user, getString("auditlog")).get();
                            kicked++;
                        } catch (InterruptedException | ExecutionException e) {
                            //Ignore
                        }
                    }

                    message.edit(EmbedFactory.getCommandEmbedSuccess(this, getString("finished2", kicked != 1, Tools.numToString(getLocale(), kicked), Tools.numToString(getLocale(), banList.size())))).get();
                    serverBlockList.remove(event.getServer().get());
                }
            }
        }
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable {}
}
