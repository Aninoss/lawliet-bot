package commands.runnables.utilitycategory;

import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnNavigationListener;
import commands.listeners.OnReactionAddStaticListener;
import commands.listeners.OnReactionRemoveStaticListener;
import constants.Emojis;
import constants.LogStatus;
import constants.Permission;
import constants.Response;
import core.DiscordApiCollection;
import core.EmbedFactory;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.emojiconnection.EmojiConnection;
import core.utils.MentionUtil;
import core.utils.PermissionUtil;
import core.utils.StringUtil;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.ServerTextChannel;
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
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "reactionroles",
        botPermissions = Permission.MANAGE_ROLES | Permission.READ_MESSAGE_HISTORY,
        userPermissions = Permission.MANAGE_ROLES,
        emoji = "☑️️",
        executableWithoutArgs = true,
        aliases = {"rmess", "reactionrole", "rroles", "selfrole", "selfroles", "sroles", "srole"}
)
public class ReactionRolesCommand extends Command implements OnNavigationListener, OnReactionAddStaticListener, OnReactionRemoveStaticListener {

    private static final int MAX_LINKS = 20;
    private final static int
            ADD_OR_EDIT = 0,
            ADD_MESSAGE = 1,
            EDIT_MESSAGE = 2,
            CONFIGURE_MESSAGE = 3,
            UPDATE_TITLE = 4,
            UPDATE_DESC = 5,
            ADD_SLOT = 6,
            REMOVE_SLOT = 7,
            EXAMPLE = 8,
            SENT = 9;

    private String title, description;
    private ArrayList<EmojiConnection> emojiConnections = new ArrayList<>();
    private Emoji emojiTemp;
    private Role roleTemp;
    private ServerTextChannel channel;
    private boolean removeRole = true, editMode = false, multipleRoles = true;
    private Message editMessage;

    private final static Logger LOGGER = LoggerFactory.getLogger(ReactionRolesCommand.class);
    private static final ArrayList<Long> block = new ArrayList<>();

    public ReactionRolesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        return true;
    }

    @ControllerMessage(state = ADD_MESSAGE)
    public Response onMessageAddMessage(MessageCreateEvent event, String inputString) {
        ArrayList<ServerTextChannel> serverTextChannel = MentionUtil.getTextChannels(event.getMessage(), inputString).getList();
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
    }

    @ControllerMessage(state = EDIT_MESSAGE)
    public Response onMessageEditMessage(MessageCreateEvent event, String inputString) {
        ArrayList<Message> messageArrayList = MentionUtil.getMessagesURL(event.getMessage(), inputString).getList();
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
    }

    @ControllerMessage(state = UPDATE_TITLE)
    public Response onMessageUpdateTitle(MessageCreateEvent event, String inputString) {
        if (inputString.length() > 0 && inputString.length() <= 256) {
            title = inputString;
            setLog(LogStatus.SUCCESS, getString("titleset", inputString));
            setState(CONFIGURE_MESSAGE);
            return Response.TRUE;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", "256"));
            return Response.FALSE;
        }
    }

    @ControllerMessage(state = UPDATE_DESC)
    public Response onMessageUpdateDesc(MessageCreateEvent event, String inputString) {
        if (inputString.length() > 0 && inputString.length() <= 1024) {
            description = inputString;
            setLog(LogStatus.SUCCESS, getString("descriptionset", inputString));
            setState(CONFIGURE_MESSAGE);
            return Response.TRUE;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", "1024"));
            return Response.FALSE;
        }
    }

    @ControllerMessage(state = ADD_SLOT)
    public Response onMessageAddSlot(MessageCreateEvent event, String inputString) {
        if (inputString.length() > 0) {
            boolean ok = false;
            List<Emoji> emojis = MentionUtil.getEmojis(event.getMessage(), inputString).getList();
            List<Role> roles = MentionUtil.getRoles(event.getMessage(), inputString).getList();

            if (emojis.size() > 0) {
                if (processEmoji(emojis.get(0))) {
                    ok = true;
                } else {
                    return Response.FALSE;
                }
            }

            if (roles.size() > 0) {
                if (processRole(roles))
                    ok = true;
                else
                    return Response.FALSE;
            }

            if (ok) return Response.TRUE;
        }

        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
        return Response.FALSE;
    }

    private boolean processEmoji(Emoji emoji) {
        if (emoji.isUnicodeEmoji() || DiscordApiCollection.getInstance().customEmojiIsKnown(emoji.asCustomEmoji().get()).isPresent()) {
            for(EmojiConnection emojiConnection: new ArrayList<>(emojiConnections)) {
                if(emojiConnection.getEmojiTag().equalsIgnoreCase(emoji.getMentionTag())) {
                    setLog(LogStatus.FAILURE, getString("emojialreadyexists"));
                    return false;
                }
            }

            this.emojiTemp = emoji;
            return true;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "emojiunknown"));
            return false;
        }
    }

    private boolean processRole(List<Role> list) {
        Role roleTest = list.get(0);
        if (!checkRoleWithLog(roleTest))
            return false;
        roleTemp = roleTest;
        return true;
    }

    @ControllerReaction(state = ADD_OR_EDIT)
    public boolean onReactionAddOrEdit(SingleReactionEvent event, int i) {
        switch (i) {
            case -1:
                removeNavigationWithMessage();
                return false;

            case 0:
                setState(ADD_MESSAGE);
                editMode = false;
                return true;

            case 1:
                setState(EDIT_MESSAGE);
                editMode = true;
                return true;

            default:
                return false;
        }
    }

    @ControllerReaction(state = ADD_MESSAGE)
    public boolean onReactionAddMessage(SingleReactionEvent event, int i) {
        switch (i) {
            case -1:
                setState(ADD_OR_EDIT);
                return true;

            case 0:
                if (channel != null) {
                    setState(CONFIGURE_MESSAGE);
                    return true;
                }

            default:
                return false;
        }
    }

    @ControllerReaction(state = EDIT_MESSAGE)
    public boolean onReactionEditMessage(SingleReactionEvent event, int i) {
        switch (i) {
            case -1:
                setState(ADD_OR_EDIT);
                return true;

            case 0:
                if (editMessage != null) {
                    updateValuesFromMessage(editMessage);
                    setState(CONFIGURE_MESSAGE);
                    return true;
                }

            default:
                return false;
        }
    }

    @ControllerReaction(state = CONFIGURE_MESSAGE)
    public boolean onReactionConfigureMessage(SingleReactionEvent event, int i) throws ExecutionException, InterruptedException {
        switch (i) {
            case -1:
                if (!editMode) setState(ADD_MESSAGE);
                else setState(EDIT_MESSAGE);
                return true;

            case 0:
                setState(UPDATE_TITLE);
                return true;

            case 1:
                setState(UPDATE_DESC);
                return true;

            case 2:
                if (emojiConnections.size() < MAX_LINKS) setState(ADD_SLOT);
                else {
                    setLog(LogStatus.FAILURE, getString("toomanyshortcuts", String.valueOf(MAX_LINKS)));
                }
                roleTemp = null;
                emojiTemp = null;
                return true;

            case 3:
                if (emojiConnections.size() > 0) setState(REMOVE_SLOT);
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
                    setState(EXAMPLE);
                } else {
                    setLog(LogStatus.FAILURE, getString("noshortcuts"));
                }
                return true;

            case 7:
                if (emojiConnections.size() > 0) {
                    if (getLinkString().length() <= 1024) {
                        if (sendMessage()) {
                            setState(SENT);
                            removeNavigation();
                        }
                    } else {
                        setLog(LogStatus.FAILURE, getString("shortcutstoolong"));
                    }
                } else {
                    setLog(LogStatus.FAILURE, getString("noshortcuts"));
                }
                return true;

            default:
                return false;
        }
    }

    @ControllerReaction(state = ADD_SLOT)
    public boolean onReactionAddSlot(SingleReactionEvent event, int i) {
        if (i == 0 && roleTemp != null && emojiTemp != null) {
            emojiConnections.add(new EmojiConnection(emojiTemp, roleTemp.getMentionTag()));
            setState(CONFIGURE_MESSAGE);
            setLog(LogStatus.SUCCESS, getString("linkadded"));
            return true;
        }

        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
            return true;
        }

        event.getMessage().get().removeReactionByEmoji(event.getUser().get(), event.getEmoji());
        return calculateEmoji(event.getEmoji());
    }

    @ControllerReaction(state = REMOVE_SLOT)
    public boolean onReactionRemoveSlot(SingleReactionEvent event, int i) {
        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
            return true;
        }
        if (i < emojiConnections.size() && i != -2) {
            setLog(LogStatus.SUCCESS, getString("linkremoved"));
            emojiConnections.remove(i);
            if (emojiConnections.size() == 0) setState(CONFIGURE_MESSAGE);
            return true;
        }
        return false;
    }

    @ControllerReaction(state = SENT)
    public boolean onReactionSent(SingleReactionEvent event, int i) {
        return false;
    }

    @ControllerReaction
    public boolean onReactionDefault(SingleReactionEvent event, int i) {
        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
            return true;
        }
        return false;
    }

    private boolean sendMessage() throws ExecutionException, InterruptedException {
        Message m;
        if (!editMode) {
            if (checkWriteInChannelWithLog(channel)) {
                m = channel.sendMessage(getMessageEmbed(false)).get();
                if (channel.canYouAddNewReactions()) {
                    for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
                        emojiConnection.addReaction(m);
                    }
                }
                return true;
            } else return false;
        } else {
            editMessage.edit(getMessageEmbed(false)).exceptionally(ExceptionLogger.get());
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
            return true;
        }
    }

    private boolean calculateEmoji(Emoji emoji) {
        if (emoji == null || (emoji.isCustomEmoji() && DiscordApiCollection.getInstance().customEmojiIsKnown(emoji.asCustomEmoji().get()).isEmpty())) {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "emojiunknown"));
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
    
    @Draw(state = ADD_OR_EDIT)
    public EmbedBuilder onDrawAddOrEdit(DiscordApi api) {
        setOptions(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"));
    }

    @Draw(state = ADD_MESSAGE)
    public EmbedBuilder onDrawAddMessage(DiscordApi api) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        if (channel != null) setOptions(new String[]{TextManager.getString(getLocale(),TextManager.GENERAL,"continue")});
        return EmbedFactory.getEmbedDefault(this, getString("state1_description", Optional.ofNullable(channel).map(Mentionable::getMentionTag).orElse(notSet)), getString("state1_title"));
    }

    @Draw(state = EDIT_MESSAGE)
    public EmbedBuilder onDrawEditMessage(DiscordApi api) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        if (editMessage != null) setOptions(new String[]{TextManager.getString(getLocale(),TextManager.GENERAL,"continue")});
        return EmbedFactory.getEmbedDefault(this, getString("state2_description", Optional.ofNullable(editMessage).map(DiscordEntity::getIdAsString).orElse(notSet)), getString("state2_title"));
    }

    @Draw(state = CONFIGURE_MESSAGE)
    public EmbedBuilder onDrawConfigureMessage(DiscordApi api) throws IOException {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        setOptions(getString("state3_options").split("\n"));

        String add;
        if (editMode) add = "edit";
        else add = "new";

        return EmbedFactory.getEmbedDefault(this, getString("state3_description"), getString("state3_title_"+add))
                .addField(getString("state3_mtitle"), StringUtil.escapeMarkdown(Optional.ofNullable(title).orElse(notSet)), true)
                .addField(getString("state3_mdescription"), StringUtil.shortenString(StringUtil.escapeMarkdown(Optional.ofNullable(description).orElse(notSet)), 1024), true)
                .addField(getString("state3_mshortcuts"), StringUtil.shortenString(Optional.ofNullable(getLinkString()).orElse(notSet), 1024), false)
                .addField(getString("state3_mproperties"), getString("state3_mproperties_desc", StringUtil.getOnOffForBoolean(getLocale(), removeRole), StringUtil.getOnOffForBoolean(getLocale(), multipleRoles)), false);
    }

    @Draw(state = UPDATE_TITLE)
    public EmbedBuilder onDrawUpdateTitle(DiscordApi api) {
        return EmbedFactory.getEmbedDefault(this, getString("state4_description"), getString("state4_title"));
    }

    @Draw(state = UPDATE_DESC)
    public EmbedBuilder onDrawUpdateDesc(DiscordApi api) {
        return EmbedFactory.getEmbedDefault(this, getString("state5_description"), getString("state5_title"));
    }

    @Draw(state = ADD_SLOT)
    public EmbedBuilder onDrawAddSlot(DiscordApi api) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        if (roleTemp != null && emojiTemp != null) setOptions(new String[]{getString("state6_options")});
        return EmbedFactory.getEmbedDefault(this, getString("state6_description", Optional.ofNullable(emojiTemp).map(Mentionable::getMentionTag).orElse(notSet), Optional.ofNullable(roleTemp).map(Role::getMentionTag).orElse(notSet)), getString("state6_title"));
    }

    @Draw(state = REMOVE_SLOT)
    public EmbedBuilder onDrawRemoveSlot(DiscordApi api) {
        ArrayList<String> optionsDelete = new ArrayList<>();
        for(EmojiConnection emojiConnection: new ArrayList<>(emojiConnections)) {
            optionsDelete.add(emojiConnection.getEmojiTag() + " " + emojiConnection.getConnection());
        }
        setOptions(optionsDelete.toArray(new String[0]));

        return EmbedFactory.getEmbedDefault(this, getString("state7_description"), getString("state7_title"));
    }

    @Draw(state = EXAMPLE)
    public EmbedBuilder onDrawExample(DiscordApi api) {
        return getMessageEmbed(true);
    }

    @Draw(state = SENT)
    public EmbedBuilder onDrawSent(DiscordApi api) {
        return EmbedFactory.getEmbedDefault(this, getString("state9_description"), getString("state9_title"));
    }

    @Override
    public void onNavigationTimeOut(Message message) {}

    @Override
    public int getMaxReactionNumber() {
        return 12;
    }

    private EmbedBuilder getMessageEmbed(boolean test) {
        String titleAdd = "";
        String identity = "";
        if (!test) identity = Emojis.EMPTY_EMOJI;
        if (!removeRole && !test) titleAdd = Emojis.EMPTY_EMOJI;
        if (!multipleRoles && !test) titleAdd += Emojis.EMPTY_EMOJI + Emojis.EMPTY_EMOJI;

        return EmbedFactory.getEmbedDefault()
                .setTitle(getEmoji() + " " + (title != null ? title : getString("title")) + identity + titleAdd)
                .setDescription(description)
                .addField(TextManager.getString(getLocale(), TextManager.GENERAL, "options"), getLinkString());
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
        while(title.endsWith(Emojis.EMPTY_EMOJI)) {
            title = title.substring(0, title.length() - 1);
            hiddenNumber++;
        }
        removeRole = (hiddenNumber & 0x1) <= 0;
        multipleRoles = (hiddenNumber & 0x2) <= 0;
        this.title = StringUtil.trimString(title.substring(3));
        if (embed.getDescription().isPresent()) this.description = StringUtil.trimString(embed.getDescription().get());

        emojiConnections = new ArrayList<>();
        checkRolesWithLog(MentionUtil.getRoles(editMessage, embed.getFields().get(0).getValue()).getList(), null);
        for(String line: embed.getFields().get(0).getValue().split("\n")) {
            String[] parts = line.split(" → ");
            if (parts[0].startsWith("<")) {
                MentionUtil.getCustomEmojiByTag(parts[0]).stream().limit(1).forEach(customEmoji -> emojiConnections.add(new EmojiConnection(customEmoji, parts[1])));
            } else {
                emojiConnections.add(new EmojiConnection(parts[0], parts[1]));
            }
        }
    }

    private boolean messageIsReactionMessage(Message message) {
        if (message.getAuthor().isYourself() && message.getEmbeds().size() > 0) {
            Embed embed = message.getEmbeds().get(0);
            if (embed.getTitle().isPresent() && embed.getAuthor().isEmpty()) {
                String title = embed.getTitle().get();
                return title.startsWith(getEmoji()) && title.endsWith(Emojis.EMPTY_EMOJI);
            }
        }
        return false;
    }

    @Override
    public void onReactionAddStatic(Message message, ReactionAddEvent event) throws Throwable {
        User user =  event.getUser().get();

        if (event.getEmoji().isUnicodeEmoji() &&
                event.getEmoji().asUnicodeEmoji().get().equals("⭐") &&
                PermissionUtil.getMissingPermissionListForUser(event.getServer().get(), event.getServerTextChannel().get(),  user, getUserPermissions()).isEmpty()
        ) {
            user.sendMessage(EmbedFactory.getEmbedDefault(this, getString("messageid", message.getLink().toString())));
        }

        updateValuesFromMessage(message);
        if (!block.contains(user.getId())) {
            try {
                if (!multipleRoles) {
                    block.add(user.getId());
                    if (removeMultipleRoles(event))
                        return;
                }

                if (!giveRole(event))
                    event.removeReaction();
            } catch (Throwable e) {
                event.removeReaction();
                throw e;
            } finally {
                if (!multipleRoles)
                    block.remove(user.getId());
            }
        }
    }

    private boolean giveRole(ReactionAddEvent event) throws ExecutionException, InterruptedException {
        for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
            if (emojiConnection.getEmojiTag().equalsIgnoreCase(event.getEmoji().getMentionTag())) {
                Optional<Role> rOpt = MentionUtil.getRoleByTag(event.getServer().get(), emojiConnection.getConnection());

                if (rOpt.isEmpty())
                    return true;

                Role r = rOpt.get();
                if (PermissionCheckRuntime.getInstance().botCanManageRoles(getLocale(), getClass(), r))
                     event.getUser().get().addRole(r).get();
                return true;
            }
        }

        return false;
    }

    private boolean removeMultipleRoles(ReactionAddEvent event) throws ExecutionException, InterruptedException {
        for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
            Optional<Role> rOpt = MentionUtil.getRoleByTag(event.getServer().get(), emojiConnection.getConnection());
            if (rOpt.isPresent()) {
                Role r = rOpt.get();
                if (event.getUser().get().getRoles(r.getServer()).contains(r) && PermissionCheckRuntime.getInstance().botCanManageRoles(getLocale(), getClass(), r)) {
                    if (!removeRole) return true;
                    r.removeUser( event.getUser().get()).get();
                }
            }
        }

        return false;
    }

    @Override
    public void onReactionRemoveStatic(Message message, ReactionRemoveEvent event) throws InterruptedException {
        updateValuesFromMessage(message);
        if (removeRole) {
            for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
                if (emojiConnection.getEmojiTag().equalsIgnoreCase(event.getEmoji().getMentionTag())) {
                    Optional<Role> rOpt = MentionUtil.getRoleByTag(event.getServer().get(), emojiConnection.getConnection());
                    if (rOpt.isEmpty()) return;
                    Role r = rOpt.get();
                    try {
                        User user =  event.getUser().get();
                        if (event.getServer().get().getMembers().contains(user) && PermissionCheckRuntime.getInstance().botCanManageRoles(getLocale(), getClass(), r))
                            user.removeRole(r).get();
                    } catch (ExecutionException e) {
                        LOGGER.error("Could not remove role", e);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public String getTitleStartIndicator() {
        return getEmoji();
    }

}
