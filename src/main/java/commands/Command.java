package commands;

import commands.listeners.*;
import constants.LogStatus;
import core.MainLogger;
import core.Program;
import core.TextManager;
import core.atomicassets.AtomicGuild;
import core.atomicassets.AtomicGuildMessageChannel;
import core.atomicassets.AtomicMember;
import core.components.ActionRows;
import core.interactionresponse.InteractionResponse;
import core.schedule.MainScheduler;
import core.utils.*;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.user.UserEntity;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageRequest;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public abstract class Command implements OnTriggerListener {

    private final long id = System.nanoTime();
    private final Category category;
    private final String prefix;
    private Locale locale;
    private final CommandProperties commandProperties;
    private final Map<String, Object> attachments = new HashMap<>();
    private final ArrayList<Runnable> completedListeners = new ArrayList<>();
    private final AtomicBoolean processing = new AtomicBoolean(false);
    private AtomicGuild atomicGuild;
    private AtomicGuildMessageChannel atomicGuildMessageChannel;
    private AtomicMember atomicMember;
    private Message drawMessage = null;
    private LogStatus logStatus = null;
    private String log = "";
    private CommandEvent commandEvent = null;
    private InteractionResponse interactionResponse;
    private boolean useInteractionResponse = true;
    private GuildEntity guildEntity;
    private boolean canHaveTimeOut = true;
    private List<ActionRow> actionRows = Collections.emptyList();
    private List<MessageEmbed> additionalEmbeds = Collections.emptyList();
    private Map<String, InputStream> fileAttachmentMap = new HashMap<>();
    private Collection<Message.MentionType> allowedMentions = MessageRequest.getDefaultMentions();
    private String memberEffectiveName;
    private String memberMention;
    private String memberEffectiveAvatarUrl;
    private String username;
    private boolean ephemeralMessages = false;
    private String args;

    public Command(Locale locale, String prefix) {
        this.locale = locale;
        this.prefix = prefix;
        commandProperties = this.getClass().getAnnotation(CommandProperties.class);
        category = Category.findCategoryByCommand(this.getClass());
    }

    public void setAdditionalEmbeds(MessageEmbed... additionalEmbeds) {
        this.additionalEmbeds = List.of(additionalEmbeds);
    }

    public void setAdditionalEmbeds(List<MessageEmbed> additionalEmbeds) {
        this.additionalEmbeds = additionalEmbeds;
    }

    public void addFileAttachment(InputStream data, String name) {
        this.fileAttachmentMap.put(name, data);
    }

    public void addAllFileAttachments(Map<String, InputStream> fileAttachmentMap) {
        this.fileAttachmentMap.putAll(fileAttachmentMap);
    }

    public void setAllowedMentions(Collection<Message.MentionType> allowedMentions) {
        this.allowedMentions = allowedMentions;
    }

    public void setActionRows(ActionRow... actionRows) {
        this.actionRows = List.of(actionRows);
    }

    public void setActionRows(List<ActionRow> actionRows) {
        this.actionRows = actionRows;
    }

    public List<ActionRow> getActionRows() {
        return actionRows;
    }

    public void setComponents(Button... buttons) {
        setComponents(List.of(buttons));
    }

    public void setComponents(SelectMenu... menus) {
        setComponents(List.of(menus));
    }

    public void setComponents(List<? extends ItemComponent> components) {
        this.actionRows = ActionRows.of(components);
    }

    public void setComponents(String... options) {
        setComponents(options, null, null, null);
    }

    public void setComponents(String[] options, Set<Integer> successIndexes) {
        setComponents(options, successIndexes, null, null);
    }

    public void setComponents(String[] options, Set<Integer> successIndexes, Set<Integer> dangerIndexes) {
        setComponents(options, successIndexes, dangerIndexes, null);
    }

    public void setComponents(String[] options, Set<Integer> successIndexes, Set<Integer> dangerIndexes, Set<Integer> disabledIndexes) {
        if (successIndexes == null) {
            successIndexes = Collections.emptySet();
        }
        if (dangerIndexes == null) {
            dangerIndexes = Collections.emptySet();
        }
        if (disabledIndexes == null) {
            disabledIndexes = Collections.emptySet();
        }

        if (options != null) {
            setComponents(optionsToButtons(options, successIndexes, dangerIndexes, disabledIndexes));
        } else {
            setActionRows();
        }
    }

    public List<Button> optionsToButtons(String[] options, Set<Integer> successIndexes, Set<Integer> dangerIndexes, Set<Integer> disabledIndexes) {
        ArrayList<Button> buttonList = new ArrayList<>();
        if (options != null) {
            for (int i = 0; i < options.length; i++) {
                if (options[i].isEmpty()) {
                    continue;
                }

                ButtonStyle buttonStyle = ButtonStyle.PRIMARY;
                if (successIndexes.contains(i)) {
                    buttonStyle = ButtonStyle.SUCCESS;
                } else if (dangerIndexes.contains(i)) {
                    buttonStyle = ButtonStyle.DANGER;
                }

                Button button = Button.of(
                        buttonStyle,
                        String.valueOf(i),
                        StringUtil.shortenString(options[i], Button.LABEL_MAX_LENGTH)
                );
                if (disabledIndexes.contains(i)) {
                    button = button.asDisabled();
                }

                buttonList.add(button);
            }
        }
        return buttonList;
    }

    public CompletableFuture<Message> drawMessageNew(EmbedBuilder eb) {
        return drawMessage(eb, true);
    }

    public CompletableFuture<Message> drawMessageNew(String content) {
        return drawMessage(content, true);
    }

    public CompletableFuture<Message> drawMessage(EmbedBuilder eb) {
        return drawMessage(eb, false);
    }

    public CompletableFuture<Message> drawMessage(String content) {
        return drawMessage(content, false);
    }

    private CompletableFuture<Message> drawMessage(EmbedBuilder eb, boolean newMessage) {
        GuildMessageChannel channel = getGuildMessageChannel().orElse(null);
        if (channel != null) {
            if (BotPermissionUtil.canWriteEmbed(channel) || (useInteractionResponse && interactionResponse != null && interactionResponse.isValid())) {
                EmbedUtil.addLog(eb, logStatus, log);
                return drawMessage(channel, null, eb, newMessage);
            } else {
                return CompletableFuture.failedFuture(new PermissionException("Missing permissions"));
            }
        } else {
            return CompletableFuture.failedFuture(new NoSuchElementException("Missing text channel"));
        }
    }

    private CompletableFuture<Message> drawMessage(String content, boolean newMessage) {
        return getGuildMessageChannel()
                .map(channel -> drawMessage(channel, content, null, newMessage))
                .orElse(CompletableFuture.failedFuture(new NoSuchElementException("No such channel")));
    }

    private synchronized CompletableFuture<Message> drawMessage(GuildMessageChannel channel, String content, EmbedBuilder eb, boolean newMessage) {
        List<MessageEmbed> additionalEmbeds = this.additionalEmbeds;
        List<ActionRow> actionRows = this.actionRows;
        Map<String, InputStream> fileAttachmentMap = this.fileAttachmentMap;
        Collection<Message.MentionType> allowedMentions = this.allowedMentions;
        resetDrawState();

        CompletableFuture<Message> future = new CompletableFuture<>();
        ArrayList<MessageEmbed> embeds = new ArrayList<>();
        try {
            if (eb != null) {
                embeds.add(eb.build());
            }
            if (BotPermissionUtil.canWriteEmbed(channel)) {
                embeds.addAll(additionalEmbeds);
            }
        } catch (Throwable e) {
            StringBuilder sb = new StringBuilder("Embed exception with fields:");
            if (eb != null) {
                eb.getFields().forEach(field -> sb
                        .append("\nKey: \"")
                        .append(field.getName())
                        .append("\"; Value: \"")
                        .append(field.getValue())
                        .append("\"")
                );
            }
            MainLogger.get().error(sb.toString(), e);
            throw e;
        }

        if (actionRows == null) {
            actionRows = Collections.emptyList();
        }

        HashSet<String> usedIds = new HashSet<>();
        for (ActionRow actionRow : actionRows) {
            for (ActionComponent component : actionRow.getActionComponents()) {
                String id = component.getId();
                if (id != null) {
                    if (usedIds.contains(id)) {
                        future.completeExceptionally(new Exception("Duplicate custom id \"" + id + "\""));
                        return future;
                    }
                    usedIds.add(id);
                }
            }
        }

        RestAction<Message> action;
        if (drawMessage == null || newMessage) {
            action = drawMessageProcessNew(channel, content, embeds, actionRows, fileAttachmentMap, allowedMentions, false);
        } else {
            action = drawMessageProcessEdit(channel, content, embeds, actionRows, allowedMentions);
        }
        processAction(channel, content, embeds, actionRows, fileAttachmentMap, allowedMentions, newMessage, action,
                future, true);
        return future;
    }

    private RestAction<Message> drawMessageProcessNew(GuildMessageChannel channel, String content, ArrayList<MessageEmbed> embeds,
                                                      List<ActionRow> actionRows, Map<String, InputStream> fileAttachmentMap,
                                                      Collection<Message.MentionType> allowedMentions, boolean forceTextMessage
    ) {
        MessageCreateAction messageAction;
        if (commandEvent.isMessageReceivedEvent() || forceTextMessage) {
            if (commandEvent.isMessageReceivedEvent()) {
                Message message = commandEvent.getMessageReceivedEvent().getMessage();
                if (content != null) {
                    messageAction = JDAUtil.replyMessage(message, getGuildEntity(), content)
                            .setEmbeds(embeds);
                } else {
                    messageAction = JDAUtil.replyMessageEmbeds(message, getGuildEntity(), embeds);
                }
            } else {
                if (content != null) {
                    messageAction = commandEvent.getMessageChannel().sendMessage(content)
                            .setEmbeds(embeds);
                } else {
                    messageAction = commandEvent.getMessageChannel().sendMessageEmbeds(embeds);
                }
            }
        } else {
            if (content != null) {
                messageAction = commandEvent.replyMessage(getGuildEntity(), ephemeralMessages, content)
                        .setEmbeds(embeds);
            } else {
                messageAction = commandEvent.replyMessageEmbeds(getGuildEntity(), ephemeralMessages, embeds);
            }
        }

        if (BotPermissionUtil.canWrite(channel, Permission.MESSAGE_ATTACH_FILES)) {
            if (!fileAttachmentMap.isEmpty()) {
                for (String fileName : fileAttachmentMap.keySet()) {
                    messageAction = messageAction.addFiles(FileUpload.fromData(fileAttachmentMap.get(fileName), fileName));
                }
            }
        }
        messageAction = messageAction.setAllowedMentions(allowedMentions);
        return messageAction.setComponents(actionRows);
    }

    private RestAction<Message> drawMessageProcessEdit(GuildMessageChannel channel, String content, ArrayList<MessageEmbed> embeds,
                                                       List<ActionRow> actionRows, Collection<Message.MentionType> allowedMentions
    ) {
        if (useInteractionResponse && interactionResponse != null && interactionResponse.isValid()) {
            return interactionResponse.editMessageEmbeds(embeds, actionRows);
        } else {
            if (content != null) {
                return channel.editMessageById(drawMessage.getIdLong(), content)
                        .setEmbeds(embeds)
                        .setComponents(actionRows)
                        .setAllowedMentions(allowedMentions);
            } else {
                return channel.editMessageEmbedsById(drawMessage.getIdLong(), embeds)
                        .setAllowedMentions(allowedMentions)
                        .setComponents(actionRows);
            }
        }
    }

    private void processAction(GuildMessageChannel channel, String content, ArrayList<MessageEmbed> embeds, List<ActionRow> actionRows,
                               Map<String, InputStream> fileAttachmentMap, Collection<Message.MentionType> allowedMentions,
                               boolean newMessage, RestAction<Message> action, CompletableFuture<Message> future,
                               boolean handleUnknownInteractionExceptions
    ) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        action.queue(message -> {
            if (!newMessage) {
                this.drawMessage = message;
            }
            future.complete(message);
        }, e -> {
            String message = e.getLocalizedMessage();
            if (handleUnknownInteractionExceptions &&
                    (message.contains("10062") || message.contains("interaction callback failure") || message.contains("Timed out"))
            ) {
                try {
                    MainLogger.get().warn("Unknown interaction for command {}", getTrigger());

                    RestAction<Message> newAction;
                    if (this.drawMessage == null || newMessage) {
                        newAction = drawMessageProcessNew(channel, content, embeds, actionRows, fileAttachmentMap, allowedMentions, true);
                    } else {
                        this.interactionResponse = null;
                        newAction = drawMessageProcessEdit(channel, content, embeds, actionRows, allowedMentions);
                    }

                    processAction(channel, content, embeds, actionRows, fileAttachmentMap, allowedMentions, newMessage,
                            newAction, future, false);
                    return;
                } catch (Throwable newException) {
                    MainLogger.get().error("Exception on unknown interaction exception handler", newException);
                }
            }

            MainLogger.get().error("Draw exception for \"{}\"", getTrigger(), ExceptionUtil.generateForStack(stackTrace, e.getLocalizedMessage()));
            future.completeExceptionally(e);
        });
    }

    public void resetDrawMessage() {
        drawMessage = null;
    }

    public void setDrawMessage(Message drawMessage) {
        this.drawMessage = drawMessage;
    }

    public Optional<Message> getDrawMessage() {
        return Optional.ofNullable(drawMessage);
    }

    public Optional<Long> getDrawMessageId() {
        return getDrawMessage().map(ISnowflake::getIdLong);
    }

    public void deferReply() {
        commandEvent.deferReply(getEphemeralMessages(), processing);
    }

    public LogStatus getLogStatus() {
        return logStatus;
    }

    public String getLog() {
        return log;
    }

    public void setLog(LogStatus logStatus, String string) {
        this.log = string;
        this.logStatus = logStatus;
    }

    public void resetDrawState() {
        this.log = "";
        this.logStatus = null;
        this.actionRows = Collections.emptyList();
        this.additionalEmbeds = Collections.emptyList();
        this.fileAttachmentMap = new HashMap<>();
        this.allowedMentions = MessageRequest.getDefaultMentions();
    }

    public void registerStaticReactionMessage(Message message, String secondaryId) {
        DBStaticReactionMessages.getInstance()
                .retrieve(message.getGuild().getIdLong())
                .put(message.getIdLong(), new StaticReactionMessageData(message, getTrigger(), secondaryId));
    }

    public void registerStaticReactionMessage(Message message) {
        DBStaticReactionMessages.getInstance()
                .retrieve(message.getGuild().getIdLong())
                .put(message.getIdLong(), new StaticReactionMessageData(message, getTrigger()));
    }

    public void registerStaticReactionMessage(GuildMessageChannel channel, long messageId) {
        DBStaticReactionMessages.getInstance()
                .retrieve(channel.getGuild().getIdLong())
                .put(messageId, new StaticReactionMessageData(
                        channel.getGuild().getIdLong(),
                        channel.getIdLong(),
                        messageId,
                        getTrigger()
                ));
    }

    public void deregisterListeners() {
        CommandContainer.deregisterListeners(this);
    }

    public synchronized void onListenerTimeOutSuper() throws Throwable {
        if (canHaveTimeOut) {
            canHaveTimeOut = false;
            onListenerTimeOut();
        }
    }

    protected void onListenerTimeOut() throws Throwable {
    }

    public String getString(String key, String... args) {
        String text = TextManager.getString(locale, category, commandProperties.trigger() + "_" + key, args);
        if (prefix != null) text = text.replace("{PREFIX}", prefix);
        return text;
    }

    public String getString(String key, int option, String... args) {
        String text = TextManager.getString(locale, category, commandProperties.trigger() + "_" + key, option, args);
        if (prefix != null) text = text.replace("{PREFIX}", prefix);
        return text;
    }

    public String getString(String key, boolean secondOption, String... args) {
        String text = TextManager.getString(locale, category, commandProperties.trigger() + "_" + key, secondOption, args);
        if (prefix != null) text = text.replace("{PREFIX}", prefix);
        return text;
    }

    public CommandLanguage getCommandLanguage() {
        String title = getString("title");
        String descShort = getString("description");
        String descLong = getString("helptext");
        String usage = getString("usage");
        String examples = getString("examples");
        return new CommandLanguage(title, descShort, descLong, usage, examples);
    }

    public Permission[] getAdjustedUserGuildPermissions() {
        return commandProperties.userGuildPermissions();
    }

    public Permission[] getAdjustedUserChannelPermissions() {
        Permission[] permissions = commandProperties.userChannelPermissions();
        return processUserPermissions(permissions);
    }

    public Permission[] getUserPermissions() {
        List<Permission> permissionList = new ArrayList<>(Arrays.asList(getCommandProperties().userGuildPermissions()));
        permissionList.addAll(Arrays.asList(getCommandProperties().userChannelPermissions()));
        return permissionList.toArray(new Permission[0]);
    }

    private Permission[] processUserPermissions(Permission[] permissions) {
        if (Arrays.stream(permissions).anyMatch(permission -> permission == Permission.ADMINISTRATOR)) {
            return new Permission[]{Permission.ADMINISTRATOR};
        }

        if ((this instanceof OnReactionListener || this instanceof OnStaticReactionAddListener || this instanceof OnStaticReactionRemoveListener) &&
                Arrays.stream(permissions).noneMatch(permission -> permission == Permission.MESSAGE_HISTORY)
        ) {
            permissions = Arrays.copyOf(permissions, permissions.length + 1);
            permissions[permissions.length - 1] = Permission.MESSAGE_HISTORY;
        }

        return permissions;
    }

    public boolean isModCommand() {
        return Arrays.stream(commandProperties.userGuildPermissions()).anyMatch(p -> p != Permission.MESSAGE_HISTORY) ||
                Arrays.stream(commandProperties.userChannelPermissions()).anyMatch(p -> p != Permission.MESSAGE_HISTORY);
    }

    public Permission[] getAdjustedBotGuildPermissions() {
        return commandProperties.botGuildPermissions();
    }

    public Permission[] getAdjustedBotChannelPermissions(boolean enactEmbedRequirement) {
        Permission[] permissions = commandProperties.botChannelPermissions();
        return processBotPermissions(permissions, enactEmbedRequirement);
    }

    private Permission[] processBotPermissions(Permission[] permissions, boolean enactEmbedRequirement) {
        if (Arrays.stream(permissions).anyMatch(permission -> permission == Permission.ADMINISTRATOR)) {
            return new Permission[]{Permission.ADMINISTRATOR};
        }

        if ((this instanceof OnReactionListener || this instanceof OnStaticReactionAddListener || this instanceof OnStaticReactionRemoveListener) &&
                Arrays.stream(permissions).noneMatch(permission -> permission == Permission.MESSAGE_ADD_REACTION)
        ) {
            permissions = Arrays.copyOf(permissions, permissions.length + 1);
            permissions[permissions.length - 1] = Permission.MESSAGE_ADD_REACTION;
        }

        if (this instanceof OnStaticReactionAddListener || this instanceof OnStaticReactionRemoveListener) {
            if (Arrays.stream(permissions).noneMatch(permission -> permission == Permission.VIEW_CHANNEL)) {
                permissions = Arrays.copyOf(permissions, permissions.length + 1);
                permissions[permissions.length - 1] = Permission.VIEW_CHANNEL;
            }
            if (Arrays.stream(permissions).noneMatch(permission -> permission == Permission.MESSAGE_HISTORY)) {
                permissions = Arrays.copyOf(permissions, permissions.length + 1);
                permissions[permissions.length - 1] = Permission.MESSAGE_HISTORY;
            }
        }

        if (this instanceof OnMessageInputListener) {
            if (Arrays.stream(permissions).noneMatch(permission -> permission == Permission.VIEW_CHANNEL)) {
                permissions = Arrays.copyOf(permissions, permissions.length + 1);
                permissions[permissions.length - 1] = Permission.VIEW_CHANNEL;
            }
        }

        if (enactEmbedRequirement && commandProperties.requiresEmbeds()) {
            permissions = Arrays.copyOf(permissions, permissions.length + 1);
            permissions[permissions.length - 1] = Permission.MESSAGE_EMBED_LINKS;
        }

        return permissions;
    }

    public boolean canRunOnGuild(long guildId, long userId) {
        long[] allowedServerIds = commandProperties.exclusiveGuilds();
        long[] allowedUserIds = commandProperties.exclusiveUsers();

        return ((allowedServerIds.length == 0) || Arrays.stream(allowedServerIds).anyMatch(checkServerId -> checkServerId == guildId)) &&
                ((allowedUserIds.length == 0) || Arrays.stream(allowedUserIds).anyMatch(checkUserId -> checkUserId == userId)) &&
                (!commandProperties.onlyPublicVersion() || Program.publicInstance());
    }

    public Optional<LocalDate> getReleaseDate() {
        int[] releaseDateArray = commandProperties.releaseDate();
        return Optional.ofNullable(releaseDateArray.length == 3 ? LocalDate.of(releaseDateArray[0], releaseDateArray[1], releaseDateArray[2]) : null);
    }

    public void addCompletedListener(Runnable runnable) {
        completedListeners.add(runnable);
    }

    public List<Runnable> getCompletedListeners() {
        return Collections.unmodifiableList(completedListeners);
    }

    public long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getTrigger() {
        return getCommandProperties().trigger();
    }

    public void addAttachment(String key, Object object) {
        if (object != null) {
            attachments.put(key, object);
        }
    }

    public<T> T getAttachment(String key, Class<T> clazz) {
        //noinspection unchecked
        return attachments.containsKey(key) ? (T) attachments.get(key) : null;
    }

    public void removeAttachment(String key) {
        attachments.remove(key);
    }

    public boolean hasAttachment(String key) {
        return attachments.containsKey(key);
    }

    public void setEphemeralMessages(boolean ephemeralMessages) {
        this.ephemeralMessages = ephemeralMessages;
    }

    public boolean getEphemeralMessages() {
        return ephemeralMessages;
    }

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
    }

    public boolean getProcessing() {
        return processing.get();
    }

    public void setProcessing(boolean processing) {
        this.processing.set(processing);
    }

    public void setAtomicGuild(Guild guild) {
        atomicGuild = new AtomicGuild(guild);
    }

    public void setAtomicAssets(GuildMessageChannel channel, Member member) {
        atomicGuild = new AtomicGuild(channel.getGuild());
        atomicGuildMessageChannel = new AtomicGuildMessageChannel(channel);
        atomicMember = new AtomicMember(member);
        memberEffectiveName = member.getEffectiveName();
        memberMention = member.getAsMention();
        memberEffectiveAvatarUrl = member.getEffectiveAvatarUrl();
        username = member.getUser().getName();
    }

    public CommandEvent getCommandEvent() {
        return commandEvent;
    }

    public void setCommandEvent(CommandEvent commandEvent) {
        this.commandEvent = commandEvent;
    }

    public void setInteractionResponse(InteractionResponse interactionResponse) {
        this.interactionResponse = interactionResponse;
    }

    public InteractionResponse getInteractionResponse() {
        return interactionResponse;
    }

    public void disableInteractionResponse() {
        useInteractionResponse = false;
    }

    public GuildEntity getGuildEntity() {
        return guildEntity;
    }

    public UserEntity getUserEntity() {
        if (guildEntity == null) {
            return null;
        }
        return guildEntity.getEntityManager().findUserEntity(atomicMember.getIdLong());
    }

    public UserEntity getUserEntityReadOnly() {
        if (guildEntity == null) {
            return null;
        }
        return guildEntity.getEntityManager().findUserEntityReadOnly(atomicMember.getIdLong());
    }

    public EntityManagerWrapper getEntityManager() {
        if (guildEntity == null) {
            return null;
        }
        return guildEntity.getEntityManager();
    }

    public void setGuildEntity(GuildEntity newGuildEntity) {
        EntityManagerWrapper currentEntityManager = getEntityManager();
        if (currentEntityManager == null || !currentEntityManager.isOpen()) {
            guildEntity = newGuildEntity;
        } else {
            newGuildEntity.getEntityManager().extendOther(currentEntityManager);
        }
        setLocale(newGuildEntity.getLocale());
    }

    public GuildEntity refreshGuildEntity() {
        GuildEntity guildEntity = HibernateManager.findGuildEntity(getGuildId().get(), getClass());
        setGuildEntity(guildEntity);
        return guildEntity;
    }

    public Optional<Guild> getGuild() {
        return Optional.ofNullable(atomicGuild)
                .flatMap(AtomicGuild::get);
    }

    public Optional<GuildMessageChannel> getGuildMessageChannel() {
        return Optional.ofNullable(atomicGuildMessageChannel)
                .flatMap(AtomicGuildMessageChannel::get);
    }

    public Optional<Member> getMember() {
        return Optional.ofNullable(atomicMember)
                .flatMap(AtomicMember::get);
    }

    public Optional<String> getMemberEffectiveName() {
        return Optional.ofNullable(memberEffectiveName);
    }

    public Optional<String> getMemberAsMention() {
        return Optional.ofNullable(memberMention);
    }

    public Optional<String> getMemberEffectiveAvatarUrl() {
        return Optional.ofNullable(memberEffectiveAvatarUrl);
    }

    public Optional<String> getUsername() {
        return Optional.ofNullable(username);
    }

    public Optional<Long> getGuildId() {
        return Optional.ofNullable(atomicGuild)
                .map(AtomicGuild::getIdLong);
    }

    public Optional<Long> getChannelId() {
        return Optional.ofNullable(atomicGuildMessageChannel)
                .map(AtomicGuildMessageChannel::getIdLong);
    }

    public Optional<Long> getMemberId() {
        return Optional.ofNullable(atomicMember)
                .map(AtomicMember::getIdLong);
    }

    public static Category getCategory(Class<? extends Command> clazz) {
        return Category.findCategoryByCommand(clazz);
    }

    public static Category getCategory(KClass<? extends Command> clazz) {
        return Category.findCategoryByCommand(JvmClassMappingKt.getJavaClass(clazz));
    }

    public CommandProperties getCommandProperties() {
        return commandProperties;
    }

    public static CommandProperties getCommandProperties(Class<? extends Command> clazz) {
        return clazz.getAnnotation(CommandProperties.class);
    }

    public static CommandLanguage getCommandLanguage(Class<? extends Command> clazz, Locale locale) {
        String trigger = getCommandProperties(clazz).trigger();
        Category category = getCategory(clazz);

        String title = TextManager.getString(locale, category, trigger + "_title");
        String descShort = TextManager.getString(locale, category, trigger + "_description");
        String descLong = TextManager.getString(locale, category, trigger + "_helptext");
        String usage = TextManager.getString(locale, category, trigger + "_usage");
        String examples = TextManager.getString(locale, category, trigger + "_examples");
        return new CommandLanguage(title, descShort, descLong, usage, examples);
    }

    public ScheduledFuture<?> schedule(Duration duration, Runnable command) {
        return MainScheduler.schedule(duration, () -> {
            try (GuildEntity guildEntity = refreshGuildEntity()) {
                command.run();
            }
        });
    }

    public void poll(Duration duration, Supplier<Boolean> command) {
        MainScheduler.poll(duration, () -> {
            try (GuildEntity guildEntity = refreshGuildEntity()) {
                return command.get();
            }
        });
    }

}
