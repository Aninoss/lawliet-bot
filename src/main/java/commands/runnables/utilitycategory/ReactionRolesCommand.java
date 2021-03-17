package commands.runnables.utilitycategory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.listeners.CommandProperties;
import commands.listeners.OnStaticReactionAddListener;
import commands.listeners.OnStaticReactionRemoveListener;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.LogStatus;
import constants.Response;
import core.*;
import core.atomicassets.AtomicRole;
import core.atomicassets.AtomicTextChannel;
import core.atomicassets.MentionableAtomicAsset;
import core.emojiconnection.EmojiConnection;
import core.utils.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;

@CommandProperties(
        trigger = "reactionroles",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        botGuildPermissions = { Permission.MANAGE_ROLES, Permission.MESSAGE_HISTORY },
        userGuildPermissions = Permission.MANAGE_ROLES,
        emoji = "☑️️",
        executableWithoutArgs = true,
        aliases = { "rmess", "reactionrole", "rroles", "selfrole", "selfroles", "sroles", "srole" }
)
public class ReactionRolesCommand extends NavigationAbstract implements OnStaticReactionAddListener, OnStaticReactionRemoveListener {

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
    private String emojiTemp;
    private AtomicRole roleTemp;
    private AtomicTextChannel channel;
    private boolean removeRole = true;
    private boolean editMode = false;
    private boolean multipleRoles = true;
    private Message editMessage;

    private static final Cache<Long, Boolean> blockCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .build();

    public ReactionRolesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        registerNavigationListener(12);
        return true;
    }

    @ControllerMessage(state = ADD_MESSAGE)
    public Response onMessageAddMessage(GuildMessageReceivedEvent event, String input) {
        List<TextChannel> serverTextChannel = MentionUtil.getTextChannels(event.getMessage(), input).getList();
        if (serverTextChannel.size() > 0) {
            if (checkWriteInChannelWithLog(serverTextChannel.get(0))) {
                channel = new AtomicTextChannel(serverTextChannel.get(0));
                setLog(LogStatus.SUCCESS, getString("channelset"));
                return Response.TRUE;
            } else {
                return Response.FALSE;
            }
        }
        setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
        return Response.FALSE;
    }

    @ControllerMessage(state = EDIT_MESSAGE)
    public Response onMessageEditMessage(GuildMessageReceivedEvent event, String input) throws ExecutionException, InterruptedException {
        List<Message> messageArrayList = MentionUtil.getMessageWithLinks(event.getMessage(), input).get().getList();
        if (messageArrayList.size() > 0) {
            for (Message message : messageArrayList) {
                if (messageIsReactionMessage(message)) {
                    TextChannel messageChannel = message.getTextChannel();
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
        setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
        return Response.FALSE;
    }

    @ControllerMessage(state = UPDATE_TITLE)
    public Response onMessageUpdateTitle(GuildMessageReceivedEvent event, String input) {
        if (input.length() > 0 && input.length() <= 256) {
            title = input;
            setLog(LogStatus.SUCCESS, getString("titleset", input));
            setState(CONFIGURE_MESSAGE);
            return Response.TRUE;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", "256"));
            return Response.FALSE;
        }
    }

    @ControllerMessage(state = UPDATE_DESC)
    public Response onMessageUpdateDesc(GuildMessageReceivedEvent event, String input) {
        if (input.length() > 0 && input.length() <= 1024) {
            description = input;
            setLog(LogStatus.SUCCESS, getString("descriptionset", input));
            setState(CONFIGURE_MESSAGE);
            return Response.TRUE;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", "1024"));
            return Response.FALSE;
        }
    }

    @ControllerMessage(state = ADD_SLOT)
    public Response onMessageAddSlot(GuildMessageReceivedEvent event, String input) {
        if (input.length() > 0) {
            boolean ok = false;
            List<String> emojis = MentionUtil.getEmojis(event.getMessage(), input).getList();
            List<Role> roles = MentionUtil.getRoles(event.getMessage(), input).getList();

            if (emojis.size() > 0) {
                if (processEmoji(emojis.get(0))) {
                    ok = true;
                } else {
                    return Response.FALSE;
                }
            }

            if (roles.size() > 0) {
                if (processRole(roles)) {
                    ok = true;
                } else {
                    return Response.FALSE;
                }
            }

            if (ok) return Response.TRUE;
        }

        setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
        return Response.FALSE;
    }

    private boolean processEmoji(String emoji) {
        if (EmojiUtil.emojiIsUnicode(emoji) || ShardManager.getInstance().emoteIsKnown(emoji)) {
            for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
                if (emojiConnection.getEmojiTag().equals(emoji)) {
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
        if (!checkRoleWithLog(roleTest)) {
            return false;
        }

        roleTemp = new AtomicRole(roleTest);
        return true;
    }

    @ControllerReaction(state = ADD_OR_EDIT)
    public boolean onReactionAddOrEdit(GenericGuildMessageReactionEvent event, int i) {
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
    public boolean onReactionAddMessage(GenericGuildMessageReactionEvent event, int i) {
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
    public boolean onReactionEditMessage(GenericGuildMessageReactionEvent event, int i) {
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
    public boolean onReactionConfigureMessage(GenericGuildMessageReactionEvent event, int i) {
        switch (i) {
            case -1:
                if (!editMode) {
                    setState(ADD_MESSAGE);
                } else {
                    setState(EDIT_MESSAGE);
                }
                return true;

            case 0:
                setState(UPDATE_TITLE);
                return true;

            case 1:
                setState(UPDATE_DESC);
                return true;

            case 2:
                if (emojiConnections.size() < MAX_LINKS) {
                    setState(ADD_SLOT);
                } else {
                    setLog(LogStatus.FAILURE, getString("toomanyshortcuts", String.valueOf(MAX_LINKS)));
                }
                roleTemp = null;
                emojiTemp = null;
                return true;

            case 3:
                if (emojiConnections.size() > 0) {
                    setState(REMOVE_SLOT);
                } else {
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
                    if (getLinkString().length() <= 1024) {
                        setState(EXAMPLE);
                    } else {
                        setLog(LogStatus.FAILURE, getString("shortcutstoolong"));
                    }
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
    public boolean onReactionAddSlot(GenericGuildMessageReactionEvent event, int i) {
        if (i == 0 && roleTemp != null && emojiTemp != null) {
            emojiConnections.add(new EmojiConnection(emojiTemp, roleTemp.getAsMention()));
            setState(CONFIGURE_MESSAGE);
            setLog(LogStatus.SUCCESS, getString("linkadded"));
            return true;
        }

        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
            return true;
        }

        if (BotPermissionUtil.can(event.getChannel(), Permission.MESSAGE_MANAGE)) {
            event.getReaction().removeReaction(event.getUser()).queue();
        }
        return calculateEmoji(EmojiUtil.reactionEmoteAsMention(event.getReactionEmote()));
    }

    @ControllerReaction(state = REMOVE_SLOT)
    public boolean onReactionRemoveSlot(GenericGuildMessageReactionEvent event, int i) {
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
    public boolean onReactionSent(GenericGuildMessageReactionEvent event, int i) {
        return false;
    }

    @ControllerReaction
    public boolean onReactionDefault(GenericGuildMessageReactionEvent event, int i) {
        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
            return true;
        }
        return false;
    }

    private boolean sendMessage() {
        Message m;
        if (!editMode) {
            if (checkWriteInChannelWithLog(channel.get().orElse(null))) {
                TextChannel textChannel = channel.get().get();
                m = textChannel.sendMessage(getMessageEmbed(false).build()).complete();
                if (BotPermissionUtil.canRead(textChannel, Permission.MESSAGE_MANAGE, Permission.MESSAGE_ADD_REACTION)) {
                    RestActionQueue restActionQueue = new RestActionQueue();
                    for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
                        restActionQueue.attach(emojiConnection.addReaction(m));
                    }
                    restActionQueue
                            .getCurrentRestAction()
                            .queue();
                }
                return true;
            } else {
                return false;
            }
        } else {
            editMessage.editMessage(getMessageEmbed(false).build()).queue();
            m = editMessage;
            RestActionQueue restActionQueue = new RestActionQueue();
            for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
                boolean exist = false;
                for (MessageReaction reaction : m.getReactions()) {
                    if (emojiConnection.isEmoji(reaction.getReactionEmote())) {
                        exist = true;
                        break;
                    }
                }
                if (!exist) {
                    restActionQueue.attach(emojiConnection.addReaction(m));
                }
            }
            for (MessageReaction reaction : m.getReactions()) {
                boolean exist = false;
                for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
                    if (emojiConnection.isEmoji(reaction.getReactionEmote())) {
                        exist = true;
                        break;
                    }
                }
                if (!exist) {
                    restActionQueue.attach(reaction.clearReactions());
                }
            }
            if (restActionQueue.isSet()) {
                restActionQueue.getCurrentRestAction().queue();
            }
            return true;
        }
    }

    private boolean calculateEmoji(String emoji) {
        if (emoji == null || (!EmojiUtil.emojiIsUnicode(emoji) && !ShardManager.getInstance().emoteIsKnown(emoji))) {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "emojiunknown"));
            return true;
        }

        for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
            if (emojiConnection.getEmojiTag().equals(emoji)) {
                setLog(LogStatus.FAILURE, getString("emojialreadyexists"));
                return true;
            }
        }

        emojiTemp = emoji;
        return true;
    }

    @Draw(state = ADD_OR_EDIT)
    public EmbedBuilder onDrawAddOrEdit() {
        setOptions(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"));
    }

    @Draw(state = ADD_MESSAGE)
    public EmbedBuilder onDrawAddMessage() {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        if (channel != null) {
            setOptions(new String[] { TextManager.getString(getLocale(), TextManager.GENERAL, "continue") });
        }
        return EmbedFactory.getEmbedDefault(this, getString("state1_description", Optional.ofNullable(channel).map(MentionableAtomicAsset::getAsMention).orElse(notSet)), getString("state1_title"));
    }

    @Draw(state = EDIT_MESSAGE)
    public EmbedBuilder onDrawEditMessage() {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        if (editMessage != null) {
            setOptions(new String[] { TextManager.getString(getLocale(), TextManager.GENERAL, "continue") });
        }
        return EmbedFactory.getEmbedDefault(this, getString("state2_description", Optional.ofNullable(editMessage).map(ISnowflake::getId).orElse(notSet)), getString("state2_title"));
    }

    @Draw(state = CONFIGURE_MESSAGE)
    public EmbedBuilder onDrawConfigureMessage() {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        setOptions(getString("state3_options").split("\n"));

        String add;
        if (editMode) {
            add = "edit";
        } else {
            add = "new";
        }

        return EmbedFactory.getEmbedDefault(this, getString("state3_description"), getString("state3_title_" + add))
                .addField(getString("state3_mtitle"), StringUtil.escapeMarkdown(Optional.ofNullable(title).orElse(notSet)), true)
                .addField(getString("state3_mdescription"), StringUtil.shortenString(StringUtil.escapeMarkdown(Optional.ofNullable(description).orElse(notSet)), 1024), true)
                .addField(getString("state3_mshortcuts"), StringUtil.shortenString(Optional.ofNullable(getLinkString()).orElse(notSet), 1024), false)
                .addField(getString("state3_mproperties"), getString("state3_mproperties_desc", StringUtil.getOnOffForBoolean(getLocale(), removeRole), StringUtil.getOnOffForBoolean(getLocale(), multipleRoles)), false);
    }

    @Draw(state = UPDATE_TITLE)
    public EmbedBuilder onDrawUpdateTitle() {
        return EmbedFactory.getEmbedDefault(this, getString("state4_description"), getString("state4_title"));
    }

    @Draw(state = UPDATE_DESC)
    public EmbedBuilder onDrawUpdateDesc() {
        return EmbedFactory.getEmbedDefault(this, getString("state5_description"), getString("state5_title"));
    }

    @Draw(state = ADD_SLOT)
    public EmbedBuilder onDrawAddSlot() {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        if (roleTemp != null && emojiTemp != null) setOptions(new String[] { getString("state6_options") });
        return EmbedFactory.getEmbedDefault(this, getString("state6_description", Optional.ofNullable(emojiTemp).orElse(notSet), Optional.ofNullable(roleTemp).map(MentionableAtomicAsset::getAsMention).orElse(notSet)), getString("state6_title"));
    }

    @Draw(state = REMOVE_SLOT)
    public EmbedBuilder onDrawRemoveSlot() {
        ArrayList<String> optionsDelete = new ArrayList<>();
        for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
            optionsDelete.add(emojiConnection.getEmojiTag() + " " + emojiConnection.getConnection());
        }
        setOptions(optionsDelete.toArray(new String[0]));

        return EmbedFactory.getEmbedDefault(this, getString("state7_description"), getString("state7_title"));
    }

    @Draw(state = EXAMPLE)
    public EmbedBuilder onDrawExample() {
        return getMessageEmbed(true);
    }

    @Draw(state = SENT)
    public EmbedBuilder onDrawSent() {
        return EmbedFactory.getEmbedDefault(this, getString("state9_description"), getString("state9_title"));
    }

    private EmbedBuilder getMessageEmbed(boolean test) {
        String titleAdd = "";
        String identity = "";
        if (!test) identity = Emojis.EMPTY_EMOJI;
        if (!removeRole && !test) titleAdd = Emojis.EMPTY_EMOJI;
        if (!multipleRoles && !test) titleAdd += Emojis.EMPTY_EMOJI + Emojis.EMPTY_EMOJI;

        return EmbedFactory.getEmbedDefault()
                .setTitle(getCommandProperties().emoji() + " " + (title != null ? title : getString("title")) + identity + titleAdd)
                .setDescription(description)
                .addField(TextManager.getString(getLocale(), TextManager.GENERAL, "options"), getLinkString(), false);
    }

    private String getLinkString() {
        StringBuilder link = new StringBuilder();
        for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
            link.append(emojiConnection.getEmojiTag());
            link.append(" → ");
            link.append(emojiConnection.getConnection());
            link.append("\n");
        }
        if (link.length() == 0) return null;
        return link.toString();
    }

    private void updateValuesFromMessage(Message editMessage) {
        MessageEmbed embed = editMessage.getEmbeds().get(0);
        String title = embed.getTitle();

        int hiddenNumber = -1;
        while (title.endsWith(Emojis.EMPTY_EMOJI)) {
            title = title.substring(0, title.length() - 1);
            hiddenNumber++;
        }
        removeRole = (hiddenNumber & 0x1) <= 0;
        multipleRoles = (hiddenNumber & 0x2) <= 0;
        this.title = title.substring(3).trim();
        if (embed.getDescription() != null) {
            this.description = embed.getDescription().trim();
        }

        emojiConnections = new ArrayList<>();
        checkRolesWithLog(editMessage.getGuild(), MentionUtil.getRoles(editMessage, embed.getFields().get(0).getValue()).getList());
        for (String line : embed.getFields().get(0).getValue().split("\n")) {
            String[] parts = line.split(" → ");
            emojiConnections.add(new EmojiConnection(parts[0], parts[1]));
        }
    }

    private boolean messageIsReactionMessage(Message message) {
        if (message.getAuthor().getIdLong() == ShardManager.getInstance().getSelfId() && message.getEmbeds().size() > 0) {
            MessageEmbed embed = message.getEmbeds().get(0);
            if (embed.getTitle() != null && embed.getAuthor() == null) {
                String title = embed.getTitle();
                return title.startsWith(getCommandProperties().emoji()) && title.endsWith(Emojis.EMPTY_EMOJI);
            }
        }
        return false;
    }

    @Override
    public void onStaticReactionAdd(Message message, GuildMessageReactionAddEvent event) {
        Member member = event.getMember();

        if (EmojiUtil.reactionEmoteEqualsEmoji(event.getReactionEmote(), "⭐") &&
                BotPermissionUtil.can(member, getCommandProperties().userGuildPermissions())
        ) {
            JDAUtil.sendPrivateMessage(
                    member,
                    EmbedFactory.getEmbedDefault(this, getString("messageid", message.getJumpUrl())).build()
            ).queue();
        }

        updateValuesFromMessage(message);
        if (!blockCache.asMap().containsKey(member.getIdLong())) {
            try {
                if (!multipleRoles) {
                    blockCache.put(member.getIdLong(), true);
                    if (removeMultipleRoles(event)) {
                        return;
                    }
                }

                giveRole(event);
            } finally {
                if (!multipleRoles) {
                    blockCache.invalidate(member.getIdLong());
                }
            }
        }
    }

    private boolean giveRole(GuildMessageReactionAddEvent event) {
        for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
            if (emojiConnection.isEmoji(event.getReactionEmote())) {
                Optional<Role> rOpt = MentionUtil.getRoleByTag(event.getGuild(), emojiConnection.getConnection());
                if (rOpt.isEmpty()) {
                    return true;
                }

                Role r = rOpt.get();
                if (PermissionCheckRuntime.getInstance().botCanManageRoles(getLocale(), getClass(), r)) {
                    event.getGuild().addRoleToMember(event.getMember(), r)
                            .reason(getCommandLanguage().getTitle())
                            .complete();
                }
                return true;
            }
        }

        return false;
    }

    private boolean removeMultipleRoles(GuildMessageReactionAddEvent event) {
        for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
            Optional<Role> rOpt = MentionUtil.getRoleByTag(event.getGuild(), emojiConnection.getConnection());
            if (rOpt.isPresent()) {
                Role r = rOpt.get();
                if (event.getMember().getRoles().contains(r) && PermissionCheckRuntime.getInstance().botCanManageRoles(getLocale(), getClass(), r)) {
                    if (!removeRole) return true;
                    event.getGuild().removeRoleFromMember(event.getMember(), r)
                            .reason(getCommandLanguage().getTitle())
                            .complete();
                }
            }
        }

        return false;
    }

    @Override
    public void onStaticReactionRemove(Message message, GuildMessageReactionRemoveEvent event) {
        updateValuesFromMessage(message);
        if (removeRole) {
            for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
                if (emojiConnection.isEmoji(event.getReactionEmote())) {
                    Optional<Role> rOpt = MentionUtil.getRoleByTag(event.getGuild(), emojiConnection.getConnection());
                    if (rOpt.isEmpty()) return;
                    Role r = rOpt.get();
                    Member member = event.getMember();
                    if (event.getGuild().getMembers().contains(member) && PermissionCheckRuntime.getInstance().botCanManageRoles(getLocale(), getClass(), r)) {
                        event.getGuild().removeRoleFromMember(member, r)
                                .reason(getCommandLanguage().getTitle())
                                .queue();
                    }
                    break;
                }
            }
        }
    }

    @Override
    public String titleStartIndicator() {
        return getCommandProperties().emoji();
    }

}
