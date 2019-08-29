package Commands.ServerManagement;

import CommandListeners.*;
import CommandSupporters.Command;
import Constants.LogStatus;
import Constants.Permission;
import Constants.Response;
import General.*;
import General.EmojiConnection.EmojiConnection;
import General.Mention.MentionFinder;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.server.Server;
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

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ReactionRolesCommand extends Command implements onNavigationListener, onReactionAddStatic, onReactionRemoveStatic {
    private String title, description;
    private ArrayList<EmojiConnection> emojiConnections = new ArrayList<>();
    private Emoji emojiTemp;
    private Role roleTemp;
    private ServerTextChannel channel;
    private boolean removeRole = true, editMode = false, multipleRoles = true;
    private Message editMessage;

    private static ArrayList<User> queue = new ArrayList<>();

    public ReactionRolesCommand() {
        super();
        trigger = "reactionroles";
        privateUse = false;
        botPermissions = Permission.MANAGE_MASSAGES_IN_TEXT_CHANNEL | Permission.REMOVE_REACTIONS_OF_OTHERS_IN_TEXT_CHANNEL |Permission.MANAGE_ROLES_ON_SERVER;
        userPermissions = Permission.MANAGE_MASSAGES_IN_TEXT_CHANNEL |Permission.MANAGE_ROLES_ON_SERVER;
        nsfw = false;
        withLoadingBar = false;
        emoji = "\u2611\uFE0F️";
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/long-shadow-documents/128/document-tick-icon.png";
        executable = true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, boolean firstTime) throws Throwable {
        if (firstTime) return Response.TRUE;

        switch (state) {
            //Reaction Message hinzufügen
            case 1:
                ArrayList<ServerTextChannel> serverTextChannel = MentionFinder.getTextChannels(event.getMessage(), inputString).getList();
                if (serverTextChannel.size() > 0) {
                    if (serverTextChannel.get(0).canYouWrite() && serverTextChannel.get(0).canYouEmbedLinks() && serverTextChannel.get(0).canYouManageMessages()) {
                        channel = serverTextChannel.get(0);
                        setLog(LogStatus.SUCCESS, getString("channelset"));
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE,TextManager.getString(locale, TextManager.GENERAL, "missing_permission_channel"));
                        return Response.FALSE;
                    }
                }
                setLog(LogStatus.FAILURE,TextManager.getString(locale, TextManager.GENERAL, "no_results_description", inputString));
                return Response.FALSE;

            //Reaction Message bearbeiten
            case 2:
                addLoadingReaction(event.getMessage());
                ArrayList<Message> messageArrayList = MentionFinder.getMessagesAll(event.getMessage(), inputString).getList();
                if (messageArrayList.size() > 0) {
                    for(Message message: messageArrayList) {
                        if (messageIsReactionMessage(message)) {
                            ServerTextChannel messageChannel = message.getServerTextChannel().get();
                            if (messageChannel.canYouEmbedLinks() && messageChannel.canYouManageMessages()) {
                                editMessage = message;
                                setLog(LogStatus.SUCCESS, getString("messageset"));
                                return Response.TRUE;
                            } else {
                                setLog(LogStatus.FAILURE,TextManager.getString(locale, TextManager.GENERAL, "missing_permission_channel"));
                                return Response.FALSE;
                            }
                        }
                    }
                }
                setLog(LogStatus.FAILURE, TextManager.getString(locale, TextManager.GENERAL, "no_results_description", inputString));
                return Response.FALSE;

            //Titel anpassen
            case 4:
                if (inputString.length() > 0) {
                    title = inputString;
                    setLog(LogStatus.SUCCESS, getString("titleset", inputString));
                    state = 3;
                    return Response.TRUE;
                } return Response.FALSE;

            //Beschreibung anpassen
            case 5:
                if (inputString.length() > 0) {
                    description = inputString;
                    setLog(LogStatus.SUCCESS, getString("descriptionset", inputString));
                    state = 3;
                    return Response.TRUE;
                } return Response.FALSE;

            //Verknüpfung hinzufügen
            case 6:
                if (inputString.length() > 0) {
                    ArrayList<Role> list = MentionFinder.getRoles(event.getMessage(), inputString).getList();
                    if (list.size() > 0) {
                        Role roleTest = list.get(0);

                        if (!Tools.canManageRole(roleTest)) {
                            setLog(LogStatus.FAILURE, TextManager.getString(locale, TextManager.GENERAL, "missing_permission", false, inputString));
                            return Response.FALSE;
                        }

                        roleTemp = roleTest;
                        setLog(LogStatus.SUCCESS, getString("roleset"));
                        return Response.TRUE;
                    } else {
                        if (inputString.startsWith("<")) {
                            CustomEmoji customEmoji = null;
                            for(Server server: event.getMessageAuthor().asUser().get().getMutualServers()) {
                                customEmoji = Tools.getCustomEmojiByTag(server, inputString);
                                if (customEmoji != null) break;
                            }

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

                                getNavigationMessage().addReaction(inputString).get();
                                if (remove) Thread.sleep(1000);
                                for(Reaction reaction: getNavigationMessage().getLatestInstance().get().getReactions()) {
                                    if (reaction.getEmoji().getMentionTag().equals(inputString)) {
                                        if (remove) reaction.remove();
                                        if (calculateEmoji(reaction.getEmoji())) return Response.TRUE;
                                        break;
                                    }
                                }
                            } catch (Throwable e) {
                                //Ignore
                            }
                        }
                    }
                }
                
                setLog(LogStatus.FAILURE, TextManager.getString(locale, TextManager.GENERAL, "no_results_description", inputString));
                return Response.FALSE;
        }

        return null;
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i) throws Throwable {
        switch (state) {
            //Hauptmenü
            case 0:
                switch (i) {
                    case -1:
                        deleteNavigationMessage();
                        return false;

                    case 0:
                        state = 1;
                        editMode = false;
                        return true;

                    case 1:
                        state = 2;
                        editMode = true;
                        return true;
                }
                return false;

            //Reaction Message hinzufügen
            case 1:
                switch (i) {
                    case -1:
                        state = 0;
                        return true;

                    case 0:
                        if (channel != null) {
                            state = 3;
                            return true;
                        }
                }
                return false;

            //Reaction Message bearbeiten
            case 2:
                switch (i) {
                    case -1:
                        state = 0;
                        return true;

                    case 0:
                        if (editMessage != null) {
                            updateValuesFromMessage(editMessage);
                            state = 3;
                            return true;
                        }
                }
                return false;

            //Message konfigurieren
            case 3:
                switch (i) {
                    case -1:
                        if (!editMode) state = 1;
                        else state = 2;
                        return true;

                    case 0:
                        state = 4;
                        return true;

                    case 1:
                        state = 5;
                        return true;

                    case 2:
                        if (emojiConnections.size() < getMaxReactionNumber()) state = 6;
                        else {
                            setLog(LogStatus.FAILURE, getString("toomanyshortcuts", String.valueOf(getMaxReactionNumber())));
                        }
                        roleTemp = null;
                        emojiTemp = null;
                        return true;

                    case 3:
                        if (emojiConnections.size() > 0) state = 7;
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
                            state = 8;
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
                            state = 9;
                            navigationFinish();
                            return true;
                        } break;
                }
                return false;

            //Verknüpfung hinzufügen
            case 6:
                if (i == 0 && roleTemp != null && emojiTemp != null) {
                    emojiConnections.add(new EmojiConnection(emojiTemp, roleTemp.getMentionTag()));
                    state = 3;
                    setLog(LogStatus.SUCCESS, getString("linkadded"));
                    return true;
                }

                switch (i) {
                    case -1:
                        state = 3;
                        return true;

                    default:
                        event.getMessage().get().removeReactionByEmoji(event.getUser(), event.getEmoji());
                        return calculateEmoji(event.getEmoji());
                }

            //Verknüpfung entfernen
            case 7:
                if (i == -1) {
                    state = 3;
                    return true;
                }
                if (i < emojiConnections.size() && i != -2) {
                    setLog(LogStatus.SUCCESS, getString("linkremoved"));
                    emojiConnections.remove(i);
                    state = 3;
                    return true;
                }
                return false;

            //Abgesendet
            case 9:
                return false;

            //Der Rest
            default:
                if (i == -1) {
                    state = 3;
                    return true;
                }
                return false;
        }
    }

    private boolean calculateEmoji(Emoji emoji) throws Throwable {
        if (emoji == null || (emoji.isCustomEmoji() && !emoji.isKnownCustomEmoji())) {
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
    public EmbedBuilder draw(DiscordApi api) throws Throwable {
        String notSet = TextManager.getString(locale, TextManager.GENERAL, "notset");
        switch (state) {
            case 0:
                options = getString("state0_options").split("\n");
                return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description"));

            case 1:
                if (channel != null) options = new String[]{TextManager.getString(locale,TextManager.GENERAL,"continue")};
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description", Tools.getStringIfNotNull(channel, notSet)),getString("state1_title"));

            case 2:
                if (editMessage != null) options = new String[]{TextManager.getString(locale,TextManager.GENERAL,"continue")};
                return EmbedFactory.getCommandEmbedStandard(this, getString("state2_description", Tools.getStringIfNotNull(editMessage, notSet)),getString("state2_title"));

            case 3:
                options = getString("state3_options").split("\n");

                if (title == null || description == null || emojiConnections.size() == 0) {
                    String[] optionsNew = new String[options.length-2];
                    for(int i=0; i < optionsNew.length; i++) {
                        optionsNew[i] = options[i];
                    }
                    options = optionsNew;
                }

                String add;
                if (editMode) add = "edit";
                else add = "new";

                return EmbedFactory.getCommandEmbedStandard(this, getString("state3_description"), getString("state3_title_"+add))
                        .addField(getString("state3_mtitle"), Tools.getStringIfNotNull(title, notSet), false)
                        .addField(getString("state3_mdescription"), Tools.getStringIfNotNull(description, notSet), false)
                        .addField(getString("state3_mshortcuts"), Tools.getStringIfNotNull(getLinkString(), notSet), false)
                        .addField(getString("state3_mproperties"), getString("state3_mproperties_desc", Tools.getOnOffForBoolean(locale, removeRole), Tools.getOnOffForBoolean(locale, multipleRoles)), false);


            case 4:
                return EmbedFactory.getCommandEmbedStandard(this, getString("state4_description"), getString("state4_title"));

            case 5:
                return EmbedFactory.getCommandEmbedStandard(this, getString("state5_description"), getString("state5_title"));

            case 6:
                if (roleTemp != null && emojiTemp != null) options = new String[]{getString("state6_options")};
                return EmbedFactory.getCommandEmbedStandard(this, getString("state6_description", Tools.getStringIfNotNull(emojiTemp, notSet), Tools.getStringIfNotNull(roleTemp, notSet)), getString("state6_title"));

            case 7:
                ArrayList<String> optionsDelete = new ArrayList<>();
                for(EmojiConnection emojiConnection: emojiConnections) {
                    optionsDelete.add(emojiConnection.getEmojiTag() + " " + emojiConnection.getConnection());
                }
                String[] strings = new String[0];
                options = optionsDelete.toArray(strings);

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
        return 12;
    }

    private EmbedBuilder getMessageEmbed(boolean test) throws Throwable {
        String titleAdd = "";
        String identity = "";
        if (!test) identity = Tools.getEmptyCharacter();
        if (!removeRole && !test) titleAdd = Tools.getEmptyCharacter();
        if (!multipleRoles && !test) titleAdd += Tools.getEmptyCharacter() + Tools.getEmptyCharacter();
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(emoji + " " + title + identity + titleAdd)
                .setDescription(description)
                .setColor(Color.WHITE)
                .addField(TextManager.getString(locale, TextManager.GENERAL, "options"), getLinkString());
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
        for(String line: embed.getFields().get(0).getValue().split("\n")) {
            String[] parts = line.split(" → ");
            if (parts[0].startsWith("<")) {
                CustomEmoji customEmoji = Tools.getCustomEmojiByTag(editMessage.getServer().get(), parts[0]);
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
                return title.startsWith(emoji) && title.endsWith(Tools.getEmptyCharacter());
            }
        }
        return false;
    }

    @Override
    public void onReactionAddStatic(Message message, ReactionAddEvent event) throws Throwable {
        User user = event.getUser();
        if (!queue.contains(user)) {

            try {
                updateValuesFromMessage(message);

                if (!multipleRoles) {
                    queue.add(user);

                    for (EmojiConnection emojiConnection : emojiConnections) {
                        Role r = Tools.getRoleByTag(event.getServer().get(), emojiConnection.getConnection());
                        if (r.getUsers().contains(event.getUser()) && event.getServer().get().canYouManageRoles()) r.removeUser(event.getUser());
                    }
                }

                for (EmojiConnection emojiConnection : emojiConnections) {
                    if (emojiConnection.getEmojiTag().equalsIgnoreCase(event.getEmoji().getMentionTag())) {
                        Role r = Tools.getRoleByTag(event.getServer().get(), emojiConnection.getConnection());
                        for (Reaction reaction : message.getReactions()) {
                            List<User> userList = reaction.getUsers().get();

                            for (User userCheck : userList) {
                                if (!message.getServer().get().getMembers().contains(userCheck)) {
                                    if (userCheck != user) reaction.removeUser(userCheck).get();
                                }
                            }
                        }

                        try {
                            if (Tools.canManageRole(r)) event.getUser().addRole(r).get();
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }

                        queue.remove(user);
                        return;
                    }
                }

                queue.remove(user);
                event.removeReaction();
            } catch (Throwable e) {
                queue.remove(user);
                event.removeReaction();
                throw e;
            }
        }
    }

    @Override
    public void onReactionRemoveStatic(Message message, ReactionRemoveEvent event) throws Throwable {
        updateValuesFromMessage(message);
        if (removeRole) {
            for (EmojiConnection emojiConnection : emojiConnections) {
                if (emojiConnection.getEmojiTag().equalsIgnoreCase(event.getEmoji().getMentionTag())) {
                    Role r = Tools.getRoleByTag(event.getServer().get(), emojiConnection.getConnection());
                    try {
                        User user = event.getUser();
                        if (event.getServer().get().getMembers().contains(user) && Tools.canManageRole(r)) user.removeRole(r).get();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
    }

    @Override
    public boolean requiresLocale() {
        return false;
    }

    @Override
    public String getTitleStartIndicator() {
        return emoji;
    }
}
