package commands.runnables.utilitycategory;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.CommandEvent;
import commands.listeners.*;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.*;
import core.atomicassets.AtomicRole;
import core.atomicassets.AtomicTextChannel;
import core.atomicassets.MentionableAtomicAsset;
import core.cache.ReactionMessagesCache;
import core.emojiconnection.EmojiConnection;
import core.utils.*;
import modules.ReactionMessage;
import modules.ReactionRoles;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "reactionroles",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        botGuildPermissions = { Permission.MANAGE_ROLES, Permission.MESSAGE_HISTORY },
        userGuildPermissions = Permission.MANAGE_ROLES,
        emoji = "☑️️",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "rmess", "reactionrole", "rroles", "selfrole", "selfroles", "sroles", "srole" }
)
public class ReactionRolesCommand extends NavigationAbstract implements OnReactionListener, OnStaticReactionAddListener, OnStaticReactionRemoveListener {

    public final static int TITLE_LENGTH_MAX = 250;
    public final static int DESC_LENGTH_MAX = 1024;
    public final static int SLOTS_TEXT_LENGTH_MAX = 1024;
    public static final int MAX_SLOTS = 20;

    private final static int
            ADD_OR_EDIT = 0,
            ADD_MESSAGE = 1,
            EDIT_MESSAGE = 2,
            CONFIGURE_MESSAGE = 3,
            UPDATE_TITLE = 4,
            UPDATE_DESC = 5,
            UPDATE_IMAGE = 10,
            ADD_SLOT = 6,
            REMOVE_SLOT = 7,
            EXAMPLE = 8,
            SENT = 9;

    private String title;
    private String description;
    private List<EmojiConnection> emojiConnections = new ArrayList<>();
    private Emoji emojiTemp;
    private String banner;
    private AtomicRole roleTemp;
    private AtomicTextChannel atomicTextChannel;
    private boolean removeRole = true;
    private boolean editMode = false;
    private boolean multipleRoles = true;
    private long editMessageId = 0L;
    private File imageCdn = null;

    private static final Cache<Long, Boolean> blockCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .build();

    public ReactionRolesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerNavigationListener(event.getMember());
        registerReactionListener(event.getMember());
        return true;
    }

    @ControllerMessage(state = ADD_MESSAGE)
    public MessageInputResponse onMessageAddMessage(MessageReceivedEvent event, String input) {
        List<TextChannel> serverTextChannel = MentionUtil.getTextChannels(event.getGuild(), input).getList();
        if (serverTextChannel.size() > 0) {
            if (checkWriteEmbedInChannelWithLog(serverTextChannel.get(0))) {
                atomicTextChannel = new AtomicTextChannel(serverTextChannel.get(0));
                setLog(LogStatus.SUCCESS, getString("channelset"));
                return MessageInputResponse.SUCCESS;
            } else {
                return MessageInputResponse.FAILED;
            }
        }
        setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
        return MessageInputResponse.FAILED;
    }

    @ControllerMessage(state = UPDATE_TITLE)
    public MessageInputResponse onMessageUpdateTitle(MessageReceivedEvent event, String input) {
        if (input.length() > 0 && input.length() <= TITLE_LENGTH_MAX) {
            title = input;
            setLog(LogStatus.SUCCESS, getString("titleset", input));
            setState(CONFIGURE_MESSAGE);
            return MessageInputResponse.SUCCESS;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", StringUtil.numToString(TITLE_LENGTH_MAX)));
            return MessageInputResponse.FAILED;
        }
    }

    @ControllerMessage(state = UPDATE_DESC)
    public MessageInputResponse onMessageUpdateDesc(MessageReceivedEvent event, String input) {
        if (input.length() > 0 && input.length() <= DESC_LENGTH_MAX) {
            description = input;
            setLog(LogStatus.SUCCESS, getString("descriptionset", input));
            setState(CONFIGURE_MESSAGE);
            return MessageInputResponse.SUCCESS;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", StringUtil.numToString(DESC_LENGTH_MAX)));
            return MessageInputResponse.FAILED;
        }
    }

    @ControllerMessage(state = UPDATE_IMAGE)
    public MessageInputResponse onMessageUpdateImage(MessageReceivedEvent event, String input) {
        List<Message.Attachment> attachments = event.getMessage().getAttachments();
        if (attachments.size() > 0) {
            Message.Attachment attachment = attachments.get(0);
            LocalFile tempFile = new LocalFile(LocalFile.Directory.CDN, String.format("reactionroles/%s.%s", RandomUtil.generateRandomString(30), attachment.getFileExtension()));
            boolean success = FileUtil.downloadImageAttachment(attachment, tempFile);
            if (success) {
                banner = uploadFile(tempFile);
                setLog(LogStatus.SUCCESS, getString("imageset"));
                setState(CONFIGURE_MESSAGE);
                return MessageInputResponse.SUCCESS;
            }
        }

        setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
        return MessageInputResponse.FAILED;
    }

    private String uploadFile(LocalFile file) {
        if (imageCdn != null) {
            imageCdn.delete();
        }

        imageCdn = file;
        return file.cdnGetUrl();
    }

    @ControllerMessage(state = ADD_SLOT)
    public MessageInputResponse onMessageAddSlot(MessageReceivedEvent event, String input) {
        if (input.length() > 0) {
            boolean ok = false;
            List<Emoji> emojis = MentionUtil.getEmojis(event.getMessage(), input).getList();
            List<Role> roles = MentionUtil.getRoles(event.getGuild(), input).getList();

            if (emojis.size() > 0) {
                if (processEmoji(emojis.get(0))) {
                    ok = true;
                } else {
                    return MessageInputResponse.FAILED;
                }
            }

            if (roles.size() > 0) {
                if (processRole(event.getMember(), roles)) {
                    ok = true;
                } else {
                    return MessageInputResponse.FAILED;
                }
            }

            if (ok) return MessageInputResponse.SUCCESS;
        }

        setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
        return MessageInputResponse.FAILED;
    }

    private boolean processEmoji(Emoji emoji) {
        if (emoji instanceof UnicodeEmoji || ShardManager.customEmojiIsKnown((CustomEmoji) emoji)) {
            for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
                if (emojiConnection.isEmoji(emoji)) {
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

    private boolean processRole(Member member, List<Role> list) {
        Role roleTest = list.get(0);
        if (!checkRolesWithLog(member, List.of(roleTest))) {
            return false;
        }

        roleTemp = new AtomicRole(roleTest);
        return true;
    }

    @ControllerButton(state = ADD_OR_EDIT)
    public boolean onButtonAddOrEdit(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                deregisterListenersWithComponentMessage();
                return false;

            case 0:
                setState(ADD_MESSAGE);
                editMode = false;
                return true;

            case 1:
                if (ReactionRoles.getReactionMessagesInGuild(event.getGuild().getIdLong()).size() > 0) {
                    setState(EDIT_MESSAGE);
                    editMode = true;
                    return true;
                } else {
                    setLog(LogStatus.FAILURE, getString("noreactionmessage"));
                    return true;
                }

            default:
                return false;
        }
    }

    @ControllerButton(state = ADD_MESSAGE)
    public boolean onButtonAddMessage(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                setState(ADD_OR_EDIT);
                return true;

            case 0:
                if (atomicTextChannel != null) {
                    setState(CONFIGURE_MESSAGE);
                    return true;
                }

            default:
                return false;
        }
    }

    @ControllerButton(state = EDIT_MESSAGE)
    public boolean onButtonEditMessage(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(ADD_OR_EDIT);
            return true;
        } else if (i >= 0) {
            List<ReactionMessage> reactionMessages = ReactionRoles.getReactionMessagesInGuild(event.getGuild().getIdLong());
            if (i < reactionMessages.size()) {
                ReactionMessage reactionMessage = reactionMessages.get(i);
                StandardGuildMessageChannel channel = reactionMessage.getStandardGuildMessageChannel().get();
                if (checkWriteEmbedInChannelWithLog(channel)) {
                    editMessageId = reactionMessage.getMessageId();
                    updateValuesFromMessage(reactionMessage);
                    setState(CONFIGURE_MESSAGE);
                }

                return true;
            }
        }

        return false;
    }

    @ControllerButton(state = CONFIGURE_MESSAGE)
    public boolean onButtonConfigureMessage(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                if (!editMode) {
                    setState(ADD_MESSAGE);
                } else {
                    imageCdn = null;
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
                setState(UPDATE_IMAGE);
                return true;

            case 3:
                if (emojiConnections.size() < MAX_SLOTS) {
                    setState(ADD_SLOT);
                } else {
                    setLog(LogStatus.FAILURE, getString("toomanyshortcuts", String.valueOf(MAX_SLOTS)));
                }
                roleTemp = null;
                emojiTemp = null;
                return true;

            case 4:
                if (emojiConnections.size() > 0) {
                    setState(REMOVE_SLOT);
                } else {
                    setLog(LogStatus.FAILURE, getString("noshortcuts"));
                }
                return true;

            case 5:
                removeRole = !removeRole;
                setLog(LogStatus.SUCCESS, getString("roleremoveset"));
                return true;

            case 6:
                multipleRoles = !multipleRoles;
                setLog(LogStatus.SUCCESS, getString("multiplerolesset"));
                return true;

            case 7:
                if (emojiConnections.size() > 0) {
                    if (ReactionRoles.generateLinkString(emojiConnections).length() <= SLOTS_TEXT_LENGTH_MAX) {
                        setState(EXAMPLE);
                    } else {
                        setLog(LogStatus.FAILURE, getString("shortcutstoolong"));
                    }
                } else {
                    setLog(LogStatus.FAILURE, getString("noshortcuts"));
                }
                return true;

            case 8:
                TextChannel textChannel = atomicTextChannel.get().orElse(null);
                String errorMessage = ReactionRoles.sendMessage(getLocale(), textChannel, title, description,
                        emojiConnections, removeRole, multipleRoles, banner, editMode, editMessageId);
                if (errorMessage != null) {
                    setLog(LogStatus.FAILURE, errorMessage);
                    return true;
                }

                setState(SENT);
                deregisterListeners();
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = UPDATE_IMAGE)
    public boolean onButtonUpdateImage(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
            return true;
        } else if (i == 0) {
            if (imageCdn != null) {
                imageCdn.delete();
                imageCdn = null;
            }
            banner = null;
            setLog(LogStatus.SUCCESS, getString("imageset"));
            setState(CONFIGURE_MESSAGE);
            return true;
        }

        return false;
    }

    @ControllerButton(state = ADD_SLOT)
    public boolean onButtonAddSlot(ButtonInteractionEvent event, int i) {
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

        return false;
    }

    @Override
    public boolean onReaction(@NotNull GenericMessageReactionEvent event) throws Throwable {
        if (getState() == ADD_SLOT && event instanceof MessageReactionAddEvent) {
            processEmoji(event.getEmoji());
            processDraw(event.getMember(), true).exceptionally(ExceptionLogger.get());
            if (BotPermissionUtil.can(event.getGuildChannel(), Permission.MESSAGE_MANAGE)) {
                event.getReaction().removeReaction(event.getUser()).queue();
            }
            return false;
        }

        return false;
    }

    @ControllerButton(state = REMOVE_SLOT)
    public boolean onButtonRemoveSlot(ButtonInteractionEvent event, int i) {
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

    @ControllerButton(state = SENT)
    public boolean onButtonSent(ButtonInteractionEvent event, int i) {
        return false;
    }

    @ControllerButton
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
            return true;
        }
        return false;
    }

    @Draw(state = ADD_OR_EDIT)
    public EmbedBuilder onDrawAddOrEdit(Member member) {
        setComponents(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"));
    }

    @Draw(state = ADD_MESSAGE)
    public EmbedBuilder onDrawAddMessage(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        if (atomicTextChannel != null) {
            setComponents(TextManager.getString(getLocale(), TextManager.GENERAL, "continue"));
        }
        return EmbedFactory.getEmbedDefault(this, getString("state1_description", Optional.ofNullable(atomicTextChannel).map(MentionableAtomicAsset::getPrefixedNameInField).orElse(notSet)), getString("state1_title"));
    }

    @Draw(state = EDIT_MESSAGE)
    public EmbedBuilder onDrawEditMessage(Member member) {
        List<ReactionMessage> reactionMessages = ReactionRoles.getReactionMessagesInGuild(member.getGuild().getIdLong());
        String[] options = new String[reactionMessages.size()];
        for (int i = 0; i < reactionMessages.size(); i++) {
            ReactionMessage reactionMessage = reactionMessages.get(i);
            AtomicTextChannel channel = new AtomicTextChannel(reactionMessage.getGuildId(), reactionMessage.getStandardGuildMessageChannelId());
            options[i] = getString("state2_template", reactionMessage.getTitle(), channel.getPrefixedName());
        }

        setComponents(options);
        return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));
    }

    @Draw(state = CONFIGURE_MESSAGE)
    public EmbedBuilder onDrawConfigureMessage(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        setComponents(getString("state3_options").split("\n"));

        String add;
        if (editMode) {
            add = "edit";
        } else {
            add = "new";
        }

        TextChannel textChannel = getTextChannel().get();
        return EmbedFactory.getEmbedDefault(this, getString("state3_description"), getString("state3_title_" + add))
                .addField(getString("state3_mtitle"), StringUtil.escapeMarkdown(Optional.ofNullable(title).orElse(notSet)), true)
                .addField(getString("state3_mdescription"), StringUtil.shortenString(StringUtil.escapeMarkdown(Optional.ofNullable(description).orElse(notSet)), SLOTS_TEXT_LENGTH_MAX), true)
                .addField(getString("state3_mimage"), StringUtil.getOnOffForBoolean(textChannel, getLocale(), banner != null), true)
                .addField(getString("state3_mshortcuts"), StringUtil.shortenString(Optional.ofNullable(ReactionRoles.generateLinkString(emojiConnections)).orElse(notSet), SLOTS_TEXT_LENGTH_MAX), false)
                .addField(getString("state3_mproperties"), getString("state3_mproperties_desc", StringUtil.getOnOffForBoolean(textChannel, getLocale(), removeRole), StringUtil.getOnOffForBoolean(textChannel, getLocale(), multipleRoles)), false);
    }

    @Draw(state = UPDATE_TITLE)
    public EmbedBuilder onDrawUpdateTitle(Member member) {
        return EmbedFactory.getEmbedDefault(this, getString("state4_description"), getString("state4_title"));
    }

    @Draw(state = UPDATE_DESC)
    public EmbedBuilder onDrawUpdateDesc(Member member) {
        return EmbedFactory.getEmbedDefault(this, getString("state5_description"), getString("state5_title"));
    }

    @Draw(state = UPDATE_IMAGE)
    public EmbedBuilder onDrawUpdateImage(Member member) {
        setComponents(getString("state10_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state10_description"), getString("state10_title"));
    }

    @Draw(state = ADD_SLOT)
    public EmbedBuilder onDrawAddSlot(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        if (roleTemp != null && emojiTemp != null) setComponents(getString("state6_options"));
        return EmbedFactory.getEmbedDefault(this, getString("state6_description", Optional.ofNullable(emojiTemp).map(Emoji::getFormatted).orElse(notSet), Optional.ofNullable(roleTemp).map(MentionableAtomicAsset::getPrefixedNameInField).orElse(notSet)), getString("state6_title"));
    }

    @Draw(state = REMOVE_SLOT)
    public EmbedBuilder onDrawRemoveSlot(Member member) {
        ArrayList<Button> buttons = new ArrayList<>();
        ArrayList<EmojiConnection> tempConnections = new ArrayList<>(emojiConnections);
        for (int i = 0; i < tempConnections.size(); i++) {
            long roleId = StringUtil.filterLongFromString(tempConnections.get(i).getConnection());
            Button button = Button.of(
                    ButtonStyle.PRIMARY,
                    String.valueOf(i),
                    StringUtil.shortenString(new AtomicRole(getGuildId().get(), roleId).getPrefixedName(), 80)
            );
            buttons.add(button);
        }
        setComponents(buttons);

        return EmbedFactory.getEmbedDefault(this, getString("state7_description"), getString("state7_title"));
    }

    @Draw(state = EXAMPLE)
    public EmbedBuilder onDrawExample(Member member) {
        return ReactionRoles.getMessageEmbed(getLocale(), title, description, emojiConnections, removeRole, multipleRoles, banner, true);
    }

    @Draw(state = SENT)
    public EmbedBuilder onDrawSent(Member member) {
        return EmbedFactory.getEmbedDefault(this, getString("state9_description"), getString("state9_title"));
    }

    @Override
    public void onStaticReactionAdd(@NotNull Message message, @NotNull MessageReactionAddEvent event) {
        if (event.getChannel() instanceof TextChannel) {
            Member member = event.getMember();
            ReactionMessagesCache.get(message).ifPresent(reactionMessage -> {
                updateValuesFromMessage(reactionMessage);
                if (!blockCache.asMap().containsKey(member.getIdLong())) {
                    GlobalThreadPool.getExecutorService().submit(() -> {
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
                    });
                }
            });
        }
    }

    private void updateValuesFromMessage(ReactionMessage message) {
        this.title = message.getTitle();
        this.description = message.getDescription().orElse(null);
        this.banner = message.getBanner().orElse(null);
        this.multipleRoles = message.isMultipleRoles();
        this.removeRole = message.isRemoveRole();
        this.emojiConnections = message.getEmojiConnections();
        this.atomicTextChannel = new AtomicTextChannel(message.getGuildId(), message.getStandardGuildMessageChannelId());
    }

    private void giveRole(MessageReactionAddEvent event) {
        for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
            if (emojiConnection.isEmoji(event.getEmoji())) {
                Optional<Role> rOpt = MentionUtil.getRoleByTag(event.getGuild(), emojiConnection.getConnection());
                if (rOpt.isEmpty()) {
                    return;
                }

                Role r = rOpt.get();
                if (PermissionCheckRuntime.botCanManageRoles(getLocale(), getClass(), r)) {
                    event.getGuild().addRoleToMember(event.getMember(), r)
                            .reason(getCommandLanguage().getTitle())
                            .complete();
                }
                return;
            }
        }

    }

    private boolean removeMultipleRoles(MessageReactionAddEvent event) {
        for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
            Optional<Role> rOpt = MentionUtil.getRoleByTag(event.getGuild(), emojiConnection.getConnection());
            if (rOpt.isPresent()) {
                Role r = rOpt.get();
                if (event.getMember().getRoles().contains(r) && PermissionCheckRuntime.botCanManageRoles(getLocale(), getClass(), r)) {
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
    public void onStaticReactionRemove(@NotNull Message message, @NotNull MessageReactionRemoveEvent event) {
        if (event.getChannel() instanceof TextChannel) {
            ReactionMessagesCache.get(message).ifPresent(reactionMessage -> {
                updateValuesFromMessage(reactionMessage);
                if (removeRole) {
                    for (EmojiConnection emojiConnection : new ArrayList<>(emojiConnections)) {
                        if (emojiConnection.isEmoji(event.getEmoji())) {
                            Optional<Role> rOpt = MentionUtil.getRoleByTag(event.getGuild(), emojiConnection.getConnection());
                            if (rOpt.isEmpty()) return;

                            Role role = rOpt.get();
                            if (PermissionCheckRuntime.botCanManageRoles(getLocale(), getClass(), role)) {
                                event.getGuild().removeRoleFromMember(UserSnowflake.fromId(event.getUserId()), role)
                                        .reason(getCommandLanguage().getTitle())
                                        .queue();
                            }
                            break;
                        }
                    }
                }
            });
        }
    }

}
