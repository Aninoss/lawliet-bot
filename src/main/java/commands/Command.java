package commands;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import commands.listeners.*;
import constants.LogStatus;
import core.InteractionResponse;
import core.MainLogger;
import core.Program;
import core.TextManager;
import core.atomicassets.AtomicGuild;
import core.atomicassets.AtomicMember;
import core.atomicassets.AtomicTextChannel;
import core.components.ActionRows;
import core.schedule.MainScheduler;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.EmojiUtil;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.RestAction;
import org.json.JSONObject;

public abstract class Command implements OnTriggerListener {

    private final long id = System.nanoTime();
    private final String category;
    private final String prefix;
    private Locale locale;
    private final CommandProperties commandProperties;
    private final JSONObject attachments = new JSONObject();
    private boolean loadingReactionSet = false;
    private final ArrayList<Runnable> completedListeners = new ArrayList<>();
    private AtomicBoolean isProcessing;
    private AtomicGuild atomicGuild;
    private AtomicTextChannel atomicTextChannel;
    private AtomicMember atomicMember;
    private Message drawMessage = null;
    private LogStatus logStatus = null;
    private String log = "";
    private GuildMessageReceivedEvent guildMessageReceivedEvent = null;
    private InteractionResponse interactionResponse;
    private boolean canHaveTimeOut = true;
    private List<ActionRow> actionRows = Collections.emptyList();
    private List<MessageEmbed> additionalEmbeds = Collections.emptyList();
    private String memberEffectiveName;
    private String memberMention;
    private String memberEffectiveAvatarUrl;
    private String memberTag;

    public Command(Locale locale, String prefix) {
        this.locale = locale;
        this.prefix = prefix;
        commandProperties = this.getClass().getAnnotation(CommandProperties.class);
        category = CategoryCalculator.getCategoryByCommand(this.getClass());
    }

    public void addLoadingReaction(Message message, AtomicBoolean isProcessing) {
        this.isProcessing = isProcessing;
        MainScheduler.getInstance().schedule(
                2500, ChronoUnit.MILLIS,
                getTrigger() + "_idle",
                () -> addLoadingReactionInstantly(message, isProcessing)
        );
    }

    public void addLoadingReactionInstantly() {
        if (isProcessing != null) {
            addLoadingReactionInstantly(guildMessageReceivedEvent.getMessage(), isProcessing);
        }
    }

    public void addLoadingReactionInstantly(Message message, AtomicBoolean isProcessing) {
        TextChannel channel = message.getTextChannel();
        if (isProcessing.get() &&
                !loadingReactionSet && BotPermissionUtil.canReadHistory(channel, Permission.MESSAGE_ADD_REACTION) &&
                !getCommandProperties().turnOffLoadingReaction()
        ) {
            loadingReactionSet = true;

            String reaction = EmojiUtil.getLoadingEmojiTag(message.getTextChannel());
            message.addReaction(reaction).queue();
            MainScheduler.getInstance().poll(100, getTrigger() + "_loading", () -> {
                if (isProcessing.get()) {
                    return true;
                } else {
                    message.removeReaction(reaction).queue();
                    loadingReactionSet = false;
                    return false;
                }
            });
        }
    }

    public void setAdditionalEmbeds(MessageEmbed... additionalEmbeds) {
        this.additionalEmbeds = List.of(additionalEmbeds);
    }

    public void setAdditionalEmbeds(List<MessageEmbed> additionalEmbeds) {
        this.additionalEmbeds = additionalEmbeds;
    }

    public void setActionRow(ActionRow actionRow) {
        setActionRows(List.of(actionRow));
    }

    public void setActionRows(List<ActionRow> actionRows) {
        this.actionRows = actionRows;
    }

    public void setButtons(Button... buttons) {
        setButtons(List.of(buttons));
    }

    public void setButtons(List<Button> buttons) {
        this.actionRows = ActionRows.of(buttons);
    }

    public List<ActionRow> getActionRows() {
        return actionRows;
    }

    public synchronized CompletableFuture<Void> redrawMessageWithoutButtons() {
        setButtons();
        if (drawMessage.getEmbeds().size() > 1) {
            ArrayList<MessageEmbed> embeds = new ArrayList<>();
            for (int i = 1; i < drawMessage.getEmbeds().size(); i++) {
                embeds.add(drawMessage.getEmbeds().get(i));
            }
            setAdditionalEmbeds(embeds);
        }
        return drawMessage(new EmbedBuilder(drawMessage.getEmbeds().get(0))).thenApply(m -> null);
    }

    public synchronized CompletableFuture<Long> drawMessage(EmbedBuilder eb) {
        EmbedUtil.addLog(eb, logStatus, log);

        CompletableFuture<Long> future = new CompletableFuture<>();
        getTextChannel().ifPresentOrElse(channel -> {
            if (BotPermissionUtil.canWriteEmbed(channel)) {
                ArrayList<MessageEmbed> embeds = new ArrayList<>();
                try {
                    embeds.add(eb.build());
                    embeds.addAll(additionalEmbeds);
                    additionalEmbeds = Collections.emptyList();
                } catch (Throwable e) {
                    StringBuilder sb = new StringBuilder("Embed exception with fields:");
                    eb.getFields().forEach(field -> sb
                            .append("\nKey: \"")
                            .append(field.getName())
                            .append("\"; Value: \"")
                            .append(field.getValue())
                            .append("\"")
                    );
                    MainLogger.get().error(sb.toString(), e);
                    throw e;
                }

                if (actionRows == null) {
                    actionRows = Collections.emptyList();
                }

                RestAction<Message> action;
                if (drawMessage == null) {
                    action = channel.sendMessageEmbeds(embeds)
                            .setActionRows(actionRows);
                } else {
                    if (interactionResponse != null &&
                            (BotPermissionUtil.canUseExternalEmojisInInteraction(channel) || !getCommandProperties().usesExtEmotes())
                    ) {
                        action = interactionResponse.editMessageEmbeds(embeds, actionRows);
                    } else {
                        action = channel.editMessageEmbedsById(drawMessage.getIdLong(), embeds)
                                .setActionRows(actionRows);
                    }
                }
                action.queue(message -> {
                    drawMessage = message;
                    future.complete(drawMessage.getIdLong());
                }, future::completeExceptionally);
            } else {
                future.completeExceptionally(new PermissionException("Missing permissions"));
            }
        }, () -> future.completeExceptionally(new NoSuchElementException("No such text channel")));

        resetLog();
        return future;
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

    public void resetLog() {
        this.log = "";
        this.logStatus = null;
    }

    public void registerStaticReactionMessage(Message message) {
        DBStaticReactionMessages.getInstance()
                .retrieve(message.getGuild().getIdLong())
                .put(message.getIdLong(), new StaticReactionMessageData(message, getTrigger()));
    }

    public void registerStaticReactionMessage(TextChannel channel, long messageId) {
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
        CommandContainer.getInstance().deregisterListeners(this);
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
        String descLong = getString("helptext");
        String usage = getString("usage");
        String examples = getString("examples");
        return new CommandLanguage(title, descLong, usage, examples);
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
            return new Permission[] { Permission.ADMINISTRATOR };
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

    public Permission[] getAdjustedBotChannelPermissions() {
        Permission[] permissions = commandProperties.botChannelPermissions();
        return processBotPermissions(permissions);
    }

    private Permission[] processBotPermissions(Permission[] permissions) {
        if (Arrays.stream(permissions).anyMatch(permission -> permission == Permission.ADMINISTRATOR)) {
            return new Permission[] { Permission.ADMINISTRATOR };
        }

        if (this instanceof OnReactionListener || this instanceof OnStaticReactionAddListener || this instanceof OnStaticReactionRemoveListener) {
            if (Arrays.stream(permissions).noneMatch(permission -> permission == Permission.MESSAGE_HISTORY)) {
                permissions = Arrays.copyOf(permissions, permissions.length + 1);
                permissions[permissions.length - 1] = Permission.MESSAGE_HISTORY;
            }
            if (Arrays.stream(permissions).noneMatch(permission -> permission == Permission.MESSAGE_ADD_REACTION)) {
                permissions = Arrays.copyOf(permissions, permissions.length + 1);
                permissions[permissions.length - 1] = Permission.MESSAGE_ADD_REACTION;
            }
        }

        return permissions;
    }

    public boolean canRunOnGuild(long guildId, long userId) {
        long[] allowedServerIds = commandProperties.exclusiveGuilds();
        long[] allowedUserIds = commandProperties.exclusiveUsers();

        return ((allowedServerIds.length == 0) || Arrays.stream(allowedServerIds).anyMatch(checkServerId -> checkServerId == guildId)) &&
                ((allowedUserIds.length == 0) || Arrays.stream(allowedUserIds).anyMatch(checkUserId -> checkUserId == userId)) &&
                (!commandProperties.onlyPublicVersion() || Program.publicVersion());
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

    public String getCategory() {
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

    public JSONObject getAttachments() {
        return attachments;
    }

    public void setAtomicAssets(TextChannel textChannel, Member member) {
        atomicGuild = new AtomicGuild(textChannel.getGuild());
        atomicTextChannel = new AtomicTextChannel(textChannel);
        atomicMember = new AtomicMember(member);
        memberEffectiveName = member.getEffectiveName();
        memberMention = member.getAsMention();
        memberEffectiveAvatarUrl = member.getUser().getEffectiveAvatarUrl();
        memberTag = member.getUser().getAsTag();
    }

    public Optional<GuildMessageReceivedEvent> getGuildMessageReceivedEvent() {
        return Optional.ofNullable(guildMessageReceivedEvent);
    }

    public void setGuildMessageReceivedEvent(GuildMessageReceivedEvent event) {
        this.guildMessageReceivedEvent = event;
    }

    public void setInteractionResponse(InteractionResponse interactionResponse) {
        this.interactionResponse = interactionResponse;
    }

    public InteractionResponse getInteractionResponse() {
        return interactionResponse;
    }

    public Optional<Guild> getGuild() {
        return Optional.ofNullable(atomicGuild)
                .flatMap(AtomicGuild::get);
    }

    public Optional<TextChannel> getTextChannel() {
        return Optional.ofNullable(atomicTextChannel)
                .flatMap(AtomicTextChannel::get);
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

    public Optional<String> getMemberAsTag() {
        return Optional.ofNullable(memberTag);
    }

    public Optional<Long> getGuildId() {
        return Optional.ofNullable(atomicGuild)
                .map(AtomicGuild::getIdLong);
    }

    public Optional<Long> getTextChannelId() {
        return Optional.ofNullable(atomicTextChannel)
                .map(AtomicTextChannel::getIdLong);
    }

    public Optional<Long> getMemberId() {
        return Optional.ofNullable(atomicMember)
                .map(AtomicMember::getIdLong);
    }

    public CommandProperties getCommandProperties() {
        return commandProperties;
    }

    public static String getCategory(Class<? extends Command> clazz) {
        return CategoryCalculator.getCategoryByCommand(clazz);
    }

    public static CommandProperties getCommandProperties(Class<? extends Command> clazz) {
        return clazz.getAnnotation(CommandProperties.class);
    }

    public static CommandLanguage getCommandLanguage(Class<? extends Command> clazz, Locale locale) {
        String trigger = getCommandProperties(clazz).trigger();
        String category = getCategory(clazz);

        String title = TextManager.getString(locale, category, trigger + "_title");
        String descLong = TextManager.getString(locale, category, trigger + "_helptext");
        String usage = TextManager.getString(locale, category, trigger + "_usage");
        String examples = TextManager.getString(locale, category, trigger + "_examples");
        return new CommandLanguage(title, descLong, usage, examples);
    }

}
