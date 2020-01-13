package Commands.Management;

import CommandListeners.*;
import CommandSupporters.Command;
import Constants.LogStatus;
import Constants.Permission;
import Constants.Response;
import General.*;
import General.EmojiConnection.EmojiConnection;
import General.Mention.MentionFinder;
import com.vdurmont.emoji.EmojiParser;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "reactionroles",
        botPermissions = Permission.MANAGE_ROLES_ON_SERVER | Permission.READ_MESSAGE_HISTORY_OF_TEXT_CHANNEL,
        userPermissions = Permission.MANAGE_ROLES_ON_SERVER,
        emoji = "\u2611\uFE0F️",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/long-shadow-documents/128/document-tick-icon.png",
        executable = true,
        aliases = {"rmess", "reactionrole", "rroles"}
)
public class ReactionRolesCommand extends Command implements onNavigationListener, onReactionAddStatic, onReactionRemoveStatic {
    
    private String title, description;
    private ArrayList<EmojiConnection> emojiConnections = new ArrayList<>();
    private Emoji emojiTemp;
    private Role roleTemp;
    private ServerTextChannel channel;
    private boolean removeRole = true, editMode = false, multipleRoles = true;
    private Message editMessage;

    private static ArrayList<Pair<Long, Long>> queue = new ArrayList<>();

    public ReactionRolesCommand() {
        super();
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state, boolean firstTime) throws Throwable {
        if (firstTime) return Response.TRUE;

        switch (state) {
            //Reaction Message hinzufügen
            case 1:
                ArrayList<ServerTextChannel> serverTextChannel = MentionFinder.getTextChannels(event.getMessage(), inputString).getList();
                if (serverTextChannel.size() > 0) {
                    if (checkWriteInChannelWithLog(serverTextChannel.get(0))) {
                        channel = serverTextChannel.get(0);
                        setLog(LogStatus.SUCCESS, getString("channelset"));
                        return Response.TRUE;
                    } else {
                        return Response.FALSE;
                    }
                }
                setLog(LogStatus.FAILURE,TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                return Response.FALSE;

            //Reaction Message bearbeiten
            case 2:
                addLoadingReaction();
                ArrayList<Message> messageArrayList = MentionFinder.getMessagesAll(event.getMessage(), inputString).getList();
                if (messageArrayList.size() > 0) {
                    for(Message message: messageArrayList) {
                        if (messageIsReactionMessage(message)) {
                            ServerTextChannel messageChannel = message.getServerTextChannel().get();
                            if (checkWriteInChannelWithLog(messageChannel)) {
                                editMessage = message;
                                setLog(LogStatus.SUCCESS, getString("messageset"));
                                return Response.TRUE;
                            } else {
                                return Response.FALSE;
                            }
                        }
                    }
                }
                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                return Response.FALSE;

            //Titel anpassen
            case 4:
                if (inputString.length() > 0 && inputString.length() <= 256) {
                    title = inputString;
                    setLog(LogStatus.SUCCESS, getString("titleset", inputString));
                    setState(3);
                    return Response.TRUE;
                } {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", "256"));
                    return Response.FALSE;
                }

            //Beschreibung anpassen
            case 5:
                if (inputString.length() > 0 && inputString.length() <= 1024) {
                    description = inputString;
                    setLog(LogStatus.SUCCESS, getString("descriptionset", inputString));
                    setState(3);
                    return Response.TRUE;
                } {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", "1024"));
                    return Response.FALSE;
                }

            //Verknüpfung hinzufügen
            case 6:
                if (inputString.length() > 0) {
                    ArrayList<Role> list = MentionFinder.getRoles(event.getMessage(), inputString).getList();
                    if (list.size() > 0) {
                        Role roleTest = list.get(0);

                        if (!checkRoleWithLog(roleTest)) return Response.FALSE;

                        roleTemp = roleTest;
                        setLog(LogStatus.SUCCESS, getString("roleset"));
                        return Response.TRUE;
                    } else {
                        if (inputString.startsWith("<")) {
                            CustomEmoji customEmoji = Tools.getCustomEmojiByTag(inputString);

                            if (calculateEmoji(customEmoji)) return Response.TRUE;
                        } else {
                            try {
                                boolean remove = true;
                                for(Reaction reaction: getNavigationMessage().getReactions()) {
                                    if (reaction.getEmoji().getMentionTag().equals(inputString)) {
                                        remove = false;
                                        break;
                                    }
                                }

                                List<String> emojis = EmojiParser.extractEmojis(inputString);

                                if (emojis.size() > 0) {
                                    boolean success = false;
                                    try {
                                        getNavigationMessage().addReaction(emojis.get(0)).get();
                                        success = true;
                                    } catch (ExecutionException e) {
                                        //Ignore
                                    }
                                    if (success) {
                                        if (remove) Thread.sleep(1000);
                                        for (Reaction reaction : getNavigationMessage().getLatestInstance().get().getReactions()) {
                                            if (reaction.getEmoji().getMentionTag().equals(inputString)) {
                                                if (remove) reaction.remove().get();
                                                if (calculateEmoji(reaction.getEmoji())) return Response.TRUE;
                                                break;
                                            }
                                        }
                                    }
                                }
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                
                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                return Response.FALSE;
        }

        return null;
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
        switch (state) {
            //Hauptmenü
            case 0:
                switch (i) {
                    case -1:
                        deleteNavigationMessage();
                        return false;

                    case 0:
                        setState(1);
                        editMode = false;
                        return true;

                    case 1:
                        setState(2);
                        editMode = true;
                        return true;
                }
                return false;

            //Reaction Message hinzufügen
            case 1:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        if (channel != null) {
                            setState(3);
                            return true;
                        }
                }
                return false;

            //Reaction Message bearbeiten
            case 2:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        if (editMessage != null) {
                            updateValuesFromMessage(editMessage);
                            setState(3);
                            return true;
                        }
                }
                return false;

            //Message konfigurieren
            case 3:
                switch (i) {
                    case -1:
                        if (!editMode) setState(1);
                        else setState(2);
                        return true;

                    case 0:
                        setState(4);
                        return true;

                    case 1:
                        setState(5);
                        return true;

                    case 2:
                        if (emojiConnections.size() < getMaxReactionNumber()) setState(6);
                        else {
                            setLog(LogStatus.FAILURE, getString("toomanyshortcuts", String.valueOf(getMaxReactionNumber())));
                        }
                        roleTemp = null;
                        emojiTemp = null;
                        return true;

                    case 3:
                        if (emojiConnections.size() > 0) setState(7);
                        else {
                            setLog(LogStatus.FAILURE, getString("noshortcuts"));
                        }
                        return true;

                    case 4:
                        removeRole = !removeRole;
                        setLog(LogStatus.SUCCESS, getString("roleremoveset"));
                        return true;

                    case 5:
                        multipleRoles = !multipleRoles;
                        setLog(LogStatus.SUCCESS, getString("multiplerolesset"));
                        return true;

                    case 6:
                        if (title != null && description != null && emojiConnections.size() > 0) {
                            setState(8);
                            return true;
                        } break;

                    case 7:
                        if (title != null && description != null && emojiConnections.size() > 0) {
                            Message m;
                            if (!editMode) {
                                m = channel.sendMessage(getMessageEmbed(false)).get();
                                for(EmojiConnection emojiConnection: emojiConnections) {
                                    emojiConnection.addReaction(m);
                                }
                            }
                            else {
                                editMessage.edit(getMessageEmbed(false));
                                m = editMessage;
                                for(EmojiConnection emojiConnection: emojiConnections) {
                                    boolean exist = false;
                                    for(Reaction reaction: m.getReactions()) {
                                        if (reaction.getEmoji().getMentionTag().equalsIgnoreCase(emojiConnection.getEmojiTag())) {
                                            exist = true;
                                            break;
                                        }
                                    }
                                    if (!exist) emojiConnection.addReaction(m);
                                }
                                for(Reaction reaction: m.getReactions()) {
                                    boolean exist = false;
                                    for(EmojiConnection emojiConnection: emojiConnections) {
                                        if (reaction.getEmoji().getMentionTag().equalsIgnoreCase(emojiConnection.getEmojiTag())) {
                                            exist = true;
                                            break;
                                        }
                                    }
                                    if (!exist) reaction.remove();
                                }
                            }
                            setState(9);
                            removeNavigation();
                            return true;
                        } break;
                }
                return false;

            //Verknüpfung hinzufügen
            case 6:
                if (i == 0 && roleTemp != null && emojiTemp != null) {
                    emojiConnections.add(new EmojiConnection(emojiTemp, roleTemp.getMentionTag()));
                    setState(3);
                    setLog(LogStatus.SUCCESS, getString("linkadded"));
                    return true;
                }

                switch (i) {
                    case -1:
                        setState(3);
                        return true;

                    default:
                        event.getMessage().get().removeReactionByEmoji(event.getUser(), event.getEmoji());
                        return calculateEmoji(event.getEmoji());
                }

            //Verknüpfung entfernen
            case 7:
                if (i == -1) {
                    setState(3);
                    return true;
                }
                if (i < emojiConnections.size() && i != -2) {
                    setLog(LogStatus.SUCCESS, getString("linkremoved"));
                    emojiConnections.remove(i);
                    setState(3);
                    return true;
                }
                return false;

            //Abgesendet
            case 9:
                return false;

            //Der Rest
            default:
                if (i == -1) {
                    setState(3);
                    return true;
                }
                return false;
        }
    }

    private boolean calculateEmoji(Emoji emoji) throws IOException {
        if (emoji == null || (emoji.isCustomEmoji() && !DiscordApiCollection.getInstance().customEmojiIsKnown(emoji.asCustomEmoji().get()))) {
            setLog(LogStatus.FAILURE, getString("emojiunknown"));
            return true;
        }

        for(EmojiConnection emojiConnection: emojiConnections) {
            if(emojiConnection.getEmojiTag().equalsIgnoreCase(emoji.getMentionTag())) {
                setLog(LogStatus.FAILURE, getString("emojialreadyexists"));
                return true;
            }
        }

        setLog(LogStatus.SUCCESS, getString("emojiset"));
        emojiTemp = emoji;
        return true;
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description"));

            case 1:
                if (channel != null) setOptions(new String[]{TextManager.getString(getLocale(),TextManager.GENERAL,"continue")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description", Tools.getStringIfNotNull(channel, notSet)),getString("state1_title"));

            case 2:
                if (editMessage != null) setOptions(new String[]{TextManager.getString(getLocale(),TextManager.GENERAL,"continue")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state2_description", Tools.getStringIfNotNull(editMessage, notSet)),getString("state2_title"));

            case 3:
                setOptions(getString("state3_options").split("\n"));

                if (title == null || description == null || emojiConnections.size() == 0) {
                    String[] optionsNew = new String[getOptions().length-2];
                    for(int i=0; i < optionsNew.length; i++) {
                        optionsNew[i] = getOptions()[i];
                    }
                    setOptions(optionsNew);
                }

                String add;
                if (editMode) add = "edit";
                else add = "new";

                return EmbedFactory.getCommandEmbedStandard(this, getString("state3_description"), getString("state3_title_"+add))
                        .addField(getString("state3_mtitle"), Tools.getStringIfNotNull(title, notSet), false)
                        .addField(getString("state3_mdescription"), Tools.getStringIfNotNull(description, notSet), false)
                        .addField(getString("state3_mshortcuts"), Tools.getStringIfNotNull(getLinkString(), notSet), false)
                        .addField(getString("state3_mproperties"), getString("state3_mproperties_desc", Tools.getOnOffForBoolean(getLocale(), removeRole), Tools.getOnOffForBoolean(getLocale(), multipleRoles)), false);


            case 4:
                return EmbedFactory.getCommandEmbedStandard(this, getString("state4_description"), getString("state4_title"));

            case 5:
                return EmbedFactory.getCommandEmbedStandard(this, getString("state5_description"), getString("state5_title"));

            case 6:
                if (roleTemp != null && emojiTemp != null) setOptions(new String[]{getString("state6_options")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state6_description", Tools.getStringIfNotNull(emojiTemp, notSet), Tools.getStringIfNotNull(roleTemp, notSet)), getString("state6_title"));

            case 7:
                ArrayList<String> optionsDelete = new ArrayList<>();
                for(EmojiConnection emojiConnection: emojiConnections) {
                    optionsDelete.add(emojiConnection.getEmojiTag() + " " + emojiConnection.getConnection());
                }
                String[] strings = new String[0];
                setOptions(optionsDelete.toArray(strings));

                return EmbedFactory.getCommandEmbedStandard(this, getString("state7_description"), getString("state7_title"));

            case 8:
                return getMessageEmbed(true);

            case 9:
                return EmbedFactory.getCommandEmbedStandard(this, getString("state9_description"), getString("state9_title"));
        }

        return null;
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {
    }

    @Override
    public int getMaxReactionNumber() {
        return 18;
    }

    private EmbedBuilder getMessageEmbed(boolean test) throws IOException {
        String titleAdd = "";
        String identity = "";
        if (!test) identity = Tools.getEmptyCharacter();
        if (!removeRole && !test) titleAdd = Tools.getEmptyCharacter();
        if (!multipleRoles && !test) titleAdd += Tools.getEmptyCharacter() + Tools.getEmptyCharacter();
        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setTitle(getEmoji() + " " + title + identity + titleAdd)
                .setDescription(description)
                .addField(TextManager.getString(getLocale(), TextManager.GENERAL, "options"), getLinkString());
        if (test) eb.setFooter(getString("previewfooter"));

        return eb;
    }

    private String getLinkString() {
        StringBuilder link = new StringBuilder();
        for(EmojiConnection emojiConnection: emojiConnections) {
            link.append(emojiConnection.getEmojiTag());
            link.append(" → ");
            link.append(emojiConnection.getConnection());
            link.append("\n");
        }
        return link.toString();
    }

    private void updateValuesFromMessage(Message editMessage) {
        Embed embed = editMessage.getEmbeds().get(0);
        String title = embed.getTitle().get();

        int hiddenNumber = -1;
        while(title.endsWith(Tools.getEmptyCharacter())) {
            title = title.substring(0, title.length() - 1);
            hiddenNumber++;
        }
        removeRole = (hiddenNumber & 0x1) <= 0;
        multipleRoles = (hiddenNumber & 0x2) <= 0;
        this.title = Tools.cutSpaces(title.substring(3));
        this.description = Tools.cutSpaces(embed.getDescription().get());

        emojiConnections = new ArrayList<>();
        checkRolesWithLog(MentionFinder.getRoles(editMessage, embed.getFields().get(0).getValue()).getList(), null);
        for(String line: embed.getFields().get(0).getValue().split("\n")) {
            String[] parts = line.split(" → ");
            if (parts[0].startsWith("<")) {
                CustomEmoji customEmoji = Tools.getCustomEmojiByTag(parts[0]);
                if (customEmoji != null)
                    emojiConnections.add(new EmojiConnection(customEmoji,parts[1]));
            } else {
                emojiConnections.add(new EmojiConnection(parts[0], parts[1]));
            }
        }
    }

    private boolean messageIsReactionMessage(Message message) {
        if (message.getAuthor().isYourself() && message.getEmbeds().size() > 0) {
            Embed embed = message.getEmbeds().get(0);
            if (embed.getTitle().isPresent() && !embed.getAuthor().isPresent()) {
                String title = embed.getTitle().get();
                return title.startsWith(getEmoji()) && title.endsWith(Tools.getEmptyCharacter());
            }
        }
        return false;
    }

    @Override
    public void onReactionAddStatic(Message message, ReactionAddEvent event) throws Throwable {
        User user = event.getUser();

        if (event.getEmoji().isUnicodeEmoji() &&
                event.getEmoji().asUnicodeEmoji().get().equals("⭐") &&
                PermissionCheck.userhasPermissions(getLocale(), event.getServer().get(), event.getServerTextChannel().get(), event.getUser(), getUserPermissions()) == null
        ) {
            if (Tools.canSendPrivateMessage(event.getUser())) event.getUser().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("messageid", message.getIdAsString())));
        }

        if (queueFind(message.getId(), user.getId()) == null) {

            try {
                updateValuesFromMessage(message);

                if (!multipleRoles) {
                    queueAdd(message.getId(), user.getId());

                    for (EmojiConnection emojiConnection : emojiConnections) {
                        Optional<Role> rOpt = Tools.getRoleByTag(event.getServer().get(), emojiConnection.getConnection());
                        if (rOpt.isPresent()) {
                            Role r = rOpt.get();
                            if (r.getUsers().contains(event.getUser()) && PermissionCheckRuntime.getInstance().botCanManageRoles(getLocale(), getTrigger(), r))
                                r.removeUser(event.getUser());
                        }
                    }
                }

                for (EmojiConnection emojiConnection : emojiConnections) {
                    if (emojiConnection.getEmojiTag().equalsIgnoreCase(event.getEmoji().getMentionTag())) {
                        Optional<Role> rOpt = Tools.getRoleByTag(event.getServer().get(), emojiConnection.getConnection());
                        if (!rOpt.isPresent()) return;
                        Role r = rOpt.get();
                        for (Reaction reaction : message.getReactions()) {
                            List<User> userList = reaction.getUsers().get();

                            for (User userCheck : userList) {
                                if (!message.getServer().get().getMemberById(userCheck.getId()).isPresent() &&
                                        userCheck.getId() != user.getId() && message.getServerTextChannel().get().canYouRemoveReactionsOfOthers()
                                ) {
                                    reaction.removeUser(userCheck).get();
                                }
                            }
                        }

                        if (PermissionCheckRuntime.getInstance().botCanManageRoles(getLocale(), getTrigger(), r)) event.getUser().addRole(r).get();

                        queueRemove(message.getId(), user.getId());
                        return;
                    }
                }

                if (message.getServerTextChannel().get().canYouRemoveReactionsOfOthers()) event.removeReaction().get();
                queueRemove(message.getId(), user.getId());
            } catch (Throwable e) {
                if (message.getServerTextChannel().get().canYouRemoveReactionsOfOthers()) event.removeReaction().get();
                queueRemove(message.getId(), user.getId());
                throw e;
            }
        }
    }

    @Override
    public void onReactionRemoveStatic(Message message, ReactionRemoveEvent event) {
        updateValuesFromMessage(message);
        if (removeRole) {
            for (EmojiConnection emojiConnection : emojiConnections) {
                if (emojiConnection.getEmojiTag().equalsIgnoreCase(event.getEmoji().getMentionTag())) {
                    Optional<Role> rOpt = Tools.getRoleByTag(event.getServer().get(), emojiConnection.getConnection());
                    if (!rOpt.isPresent()) return;
                    Role r = rOpt.get();
                    try {
                        User user = event.getUser();
                        if (event.getServer().get().getMembers().contains(user) && PermissionCheckRuntime.getInstance().botCanManageRoles(getLocale(), getTrigger(), r)) user.removeRole(r).get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    private static synchronized Pair<Long, Long> queueFind(long messageId, long userId) {
        for(Pair<Long, Long> pair: queue) {
            if (pair.getKey() == messageId && pair.getValue() == userId) return pair;
        }
        return null;
    }

    private static synchronized void queueAdd(long messageId, long userId) {
        if (queueFind(messageId, userId) == null) queue.add(new Pair<>(messageId, userId));
    }

    private static synchronized void queueRemove(long messageId, long userId) {
        Pair<Long, Long> pair = queueFind(messageId, userId);
        if (pair != null) queue.remove(pair);
    }

    @Override
    public boolean requiresLocale() {
        return true;
    }

    @Override
    public String getTitleStartIndicator() {
        return getEmoji();
    }
}
