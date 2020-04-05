package Commands.ManagementCategory;

import CommandListeners.*;
import CommandSupporters.Command;
import Constants.LogStatus;
import Constants.Permission;
import Constants.Response;
import Constants.Settings;
import General.*;
import General.EmojiConnection.EmojiConnection;
import General.Mention.MentionTools;
import General.Mention.MentionList;
import General.Tools.StringTools;
import com.vdurmont.emoji.EmojiParser;
import javafx.util.Pair;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.emoji.KnownCustomEmoji;
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
        botPermissions = Permission.MANAGE_ROLES | Permission.READ_MESSAGE_HISTORY,
        userPermissions = Permission.MANAGE_ROLES,
        emoji = "\u2611\uFE0F️",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/long-shadow-documents/128/document-tick-icon.png",
        executable = true,
        aliases = {"rmess", "reactionrole", "rroles"}
)
public class ReactionRolesCommand extends Command implements onNavigationListener, onReactionAddStaticListener, onReactionRemoveStaticListener {

    private static final int MAX_LINKS = 18;

    private String title, description;
    private ArrayList<EmojiConnection> emojiConnections = new ArrayList<>();
    private Emoji emojiTemp;
    private Role roleTemp;
    private ServerTextChannel channel;
    private boolean removeRole = true, editMode = false, multipleRoles = true;
    private Message editMessage;

    private static ArrayList<Pair<Long, Long>> queue = new ArrayList<>();

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        return true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable {
        switch (state) {
            //Reaction Message hinzufügen
            case 1:
                ArrayList<ServerTextChannel> serverTextChannel = MentionTools.getTextChannels(event.getMessage(), inputString).getList();
                if (serverTextChannel.size() > 0) {
                    if (checkWriteInChannelWithLog(serverTextChannel.get(0))) {
                        channel = serverTextChannel.get(0);
                        setLog(LogStatus.SUCCESS, getString("channelset"));
                        return Response.TRUE;
                    } else {
                        return Response.FALSE;
                    }
                }
                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                return Response.FALSE;

            //Reaction Message bearbeiten
            case 2:
                addLoadingReaction();
                ArrayList<Message> messageArrayList = MentionTools.getMessagesAll(event.getMessage(), inputString).getList();
                if (messageArrayList.size() > 0) {
                    for (Message message : messageArrayList) {
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
                }
            {
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
                }
            {
                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", "1024"));
                return Response.FALSE;
            }

            //Verknüpfung hinzufügen
            case 6:
                if (inputString.length() > 0) {
                    boolean updateRole = false, updateEmoji = false;
                    String inputString2 = null;

                    List<KnownCustomEmoji> customEmojis = MentionTools.getCustomEmojiByTag(inputString);
                    if (customEmojis.size() > 0) {
                        updateEmoji = calculateEmoji(customEmojis.get(0));
                        inputString2 = inputString.replaceFirst(customEmojis.get(0).getMentionTag(), "");
                    } else {
                        try {
                            boolean remove = true;
                            for (Reaction reaction : getNavigationMessage().getReactions()) {
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
                                    if (remove) Thread.sleep(500);
                                    for (Reaction reaction : getNavigationMessage().getLatestInstance().get().getReactions()) {
                                        if (emojis.get(0).equals(reaction.getEmoji().getMentionTag())) {
                                            if (remove) reaction.remove().get();
                                            inputString2 = StringTools.trimString(inputString.replaceFirst(emojis.get(0), ""));
                                            updateEmoji = calculateEmoji(reaction.getEmoji());
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }

                    MentionList<Role> mentionedRoles = MentionTools.getRoles(event.getMessage(), inputString);
                    if (inputString2 != null && mentionedRoles.getList().size() == 0) mentionedRoles = MentionTools.getRoles(event.getMessage(), inputString2);
                    ArrayList<Role> list = mentionedRoles.getList();
                    if (list.size() > 0) {
                        Role roleTest = list.get(0);

                        if (!checkRoleWithLog(roleTest)) return Response.FALSE;

                        roleTemp = roleTest;
                        updateRole = true;

                        inputString = mentionedRoles.getResultMessageString();
                        //setLog(LogStatus.SUCCESS, getString("roleset"));
                        //return Response.TRUE;
                    }

                    if (updateEmoji || updateRole) {
                        return Response.TRUE;
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
                        if (emojiConnections.size() < MAX_LINKS) setState(6);
                        else {
                            setLog(LogStatus.FAILURE, getString("toomanyshortcuts", String.valueOf(MAX_LINKS)));
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
                        if (emojiConnections.size() > 0) {
                            setState(8);
                            return true;
                        } break;

                    case 7:
                        if (emojiConnections.size() > 0) {
                            Message m;
                            if (!editMode) {
                                m = channel.sendMessage(getMessageEmbed(false)).get();
                                for(EmojiConnection emojiConnection: new ArrayList<>(emojiConnections)) {
                                    emojiConnection.addReaction(m);
                                }
                            }
                            else {
                                editMessage.edit(getMessageEmbed(false));
                                m = editMessage;
                                for(EmojiConnection emojiConnection: new ArrayList<>(emojiConnections)) {
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
                                    for(EmojiConnection emojiConnection: new ArrayList<>(emojiConnections)) {
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
                    if (emojiConnections.size() == 0) setState(3);
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

        for(EmojiConnection emojiConnection: new ArrayList<>(emojiConnections)) {
            if(emojiConnection.getEmojiTag().equalsIgnoreCase(emoji.getMentionTag())) {
                setLog(LogStatus.FAILURE, getString("emojialreadyexists"));
                return true;
            }
        }

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
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description", Optional.ofNullable(channel).map(Mentionable::getMentionTag).orElse(notSet)), getString("state1_title"));

            case 2:
                if (editMessage != null) setOptions(new String[]{TextManager.getString(getLocale(),TextManager.GENERAL,"continue")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state2_description", Optional.ofNullable(editMessage).map(DiscordEntity::getIdAsString).orElse(notSet)), getString("state2_title"));

            case 3:
                setOptions(getString("state3_options").split("\n"));

                if (emojiConnections.size() == 0) {
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
                        .addField(getString("state3_mtitle"), Optional.ofNullable(title).orElse(notSet), true)
                        .addField(getString("state3_mdescription"), Optional.ofNullable(description).orElse(notSet), true)
                        .addField(getString("state3_mshortcuts"), Optional.ofNullable(getLinkString()).orElse(notSet), false)
                        .addField(getString("state3_mproperties"), getString("state3_mproperties_desc", StringTools.getOnOffForBoolean(getLocale(), removeRole), StringTools.getOnOffForBoolean(getLocale(), multipleRoles)), false);


            case 4:
                return EmbedFactory.getCommandEmbedStandard(this, getString("state4_description"), getString("state4_title"));

            case 5:
                return EmbedFactory.getCommandEmbedStandard(this, getString("state5_description"), getString("state5_title"));

            case 6:
                if (roleTemp != null && emojiTemp != null) setOptions(new String[]{getString("state6_options")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state6_description", Optional.ofNullable(emojiTemp).map(Mentionable::getMentionTag).orElse(notSet), Optional.ofNullable(roleTemp).map(Role::getMentionTag).orElse(notSet)), getString("state6_title"));

            case 7:
                ArrayList<String> optionsDelete = new ArrayList<>();
                for(EmojiConnection emojiConnection: new ArrayList<>(emojiConnections)) {
                    optionsDelete.add(emojiConnection.getEmojiTag() + " " + emojiConnection.getConnection());
                }
                setOptions(optionsDelete.toArray(new String[0]));

                return EmbedFactory.getCommandEmbedStandard(this, getString("state7_description"), getString("state7_title"));

            case 8:
                return getMessageEmbed(true);

            case 9:
                return EmbedFactory.getCommandEmbedStandard(this, getString("state9_description"), getString("state9_title"));
        }

        return null;
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 12;
    }

    private EmbedBuilder getMessageEmbed(boolean test) throws IOException {
        String titleAdd = "";
        String identity = "";
        if (!test) identity = Settings.EMPTY_EMOJI;
        if (!removeRole && !test) titleAdd = Settings.EMPTY_EMOJI;
        if (!multipleRoles && !test) titleAdd += Settings.EMPTY_EMOJI + Settings.EMPTY_EMOJI;
        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setTitle(getEmoji() + " " + (title != null ? title : getString("title")) + identity + titleAdd)
                .setDescription(description)
                .addField(TextManager.getString(getLocale(), TextManager.GENERAL, "options"), getLinkString());
        if (test) eb.setFooter(getString("previewfooter"));

        return eb;
    }

    private String getLinkString() {
        StringBuilder link = new StringBuilder();
        for(EmojiConnection emojiConnection: new ArrayList<>(emojiConnections)) {
            link.append(emojiConnection.getEmojiTag());
            link.append(" → ");
            link.append(emojiConnection.getConnection());
            link.append("\n");
        }
        if (link.length() == 0) return null;
        return link.toString();
    }

    private void updateValuesFromMessage(Message editMessage) {
        Embed embed = editMessage.getEmbeds().get(0);
        String title = embed.getTitle().get();

        int hiddenNumber = -1;
        while(title.endsWith(Settings.EMPTY_EMOJI)) {
            title = title.substring(0, title.length() - 1);
            hiddenNumber++;
        }
        removeRole = (hiddenNumber & 0x1) <= 0;
        multipleRoles = (hiddenNumber & 0x2) <= 0;
        this.title = StringTools.trimString(title.substring(3));
        if (embed.getDescription().isPresent()) this.description = StringTools.trimString(embed.getDescription().get());

        emojiConnections = new ArrayList<>();
        checkRolesWithLog(MentionTools.getRoles(editMessage, embed.getFields().get(0).getValue()).getList(), null);
        for(String line: embed.getFields().get(0).getValue().split("\n")) {
            String[] parts = line.split(" → ");
            if (parts[0].startsWith("<")) {
                MentionTools.getCustomEmojiByTag(parts[0]).stream().limit(1).forEach(customEmoji -> emojiConnections.add(new EmojiConnection(customEmoji, parts[1])));
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
                return title.startsWith(getEmoji()) && title.endsWith(Settings.EMPTY_EMOJI);
            }
        }
        return false;
    }

    @Override
    public void onReactionAddStatic(Message message, ReactionAddEvent event) throws Throwable {
        User user = event.getUser();

        if (event.getEmoji().isUnicodeEmoji() &&
                event.getEmoji().asUnicodeEmoji().get().equals("⭐") &&
                PermissionCheck.getMissingPermissionListForUser(event.getServer().get(), event.getServerTextChannel().get(), event.getUser(), getUserPermissions()).isEmpty()
        ) {
            event.getUser().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("messageid", message.getIdAsString())));
        }

        if (queueFind(message.getId(), user.getId()) == null) {

            try {
                updateValuesFromMessage(message);

                if (!multipleRoles) {
                    queueAdd(message.getId(), user.getId());

                    for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
                        Optional<Role> rOpt = MentionTools.getRoleByTag(event.getServer().get(), emojiConnection.getConnection());
                        if (rOpt.isPresent()) {
                            Role r = rOpt.get();
                            if (r.getUsers().contains(event.getUser()) && PermissionCheckRuntime.getInstance().botCanManageRoles(getLocale(), getClass(), r))
                                r.removeUser(event.getUser());
                        }
                    }
                }

                for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
                    if (emojiConnection.getEmojiTag().equalsIgnoreCase(event.getEmoji().getMentionTag())) {
                        Optional<Role> rOpt = MentionTools.getRoleByTag(event.getServer().get(), emojiConnection.getConnection());
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

                        if (PermissionCheckRuntime.getInstance().botCanManageRoles(getLocale(), getClass(), r)) event.getUser().addRole(r).get();

                        queueRemove(message.getId(), user.getId());
                        return;
                    }
                }

                if (message.getServerTextChannel().get().canYouRemoveReactionsOfOthers()) event.removeReaction();
                queueRemove(message.getId(), user.getId());
            } catch (Throwable e) {
                if (message.getServerTextChannel().get().canYouRemoveReactionsOfOthers()) event.removeReaction();
                queueRemove(message.getId(), user.getId());
                throw e;
            }
        }
    }

    @Override
    public void onReactionRemoveStatic(Message message, ReactionRemoveEvent event) {
        updateValuesFromMessage(message);
        if (removeRole) {
            for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
                if (emojiConnection.getEmojiTag().equalsIgnoreCase(event.getEmoji().getMentionTag())) {
                    Optional<Role> rOpt = MentionTools.getRoleByTag(event.getServer().get(), emojiConnection.getConnection());
                    if (!rOpt.isPresent()) return;
                    Role r = rOpt.get();
                    try {
                        User user = event.getUser();
                        if (event.getServer().get().getMembers().contains(user) && PermissionCheckRuntime.getInstance().botCanManageRoles(getLocale(), getClass(), r)) user.removeRole(r).get();
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
    public String getTitleStartIndicator() {
        return getEmoji();
    }

}
