package CommandSupporters;

import CommandListeners.*;
import CommandListeners.CommandProperties;
import Commands.InformationCategory.HelpCommand;
import Constants.*;
import Core.*;
import Core.EmojiConnection.EmojiConnection;
import Core.Mention.MentionTools;
import Core.Tools.StringTools;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public abstract class Command {

    private String category, prefix;
    private CommandProperties commandProperties;
    private Message starterMessage, navigationMessage, lastUserMessage;
    private Locale locale;
    private LoadingStatus loadingStatus = LoadingStatus.OFF;
    private long reactionMessageID, reactionUserID, forwardChannelID = -1, forwardUserID = -1;
    private Countdown countdown;
    private LogStatus logStatus = null;
    private String log;
    private String[] options;
    private boolean navigationActive, loadingBlock = false, navigationPrivateMessage = false;
    private int state = 0, page = 0, pageMax = 0;
    private Thread thread;

    private enum LoadingStatus { OFF, ONGOING, FINISHED }

    public Command() {
        commandProperties = this.getClass().getAnnotation(CommandProperties.class);
        category = CategoryCalculator.getCategoryByCommand(this.getClass());
        thread = Thread.currentThread();
    }

    protected abstract boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable;

    public void onRecievedSuper(MessageCreateEvent event, String followedString) {
        updateThreadName();

        starterMessage = event.getMessage();
        lastUserMessage = event.getMessage();
        if (commandProperties.withLoadingBar()) addLoadingReaction();

        boolean successful = false;
        try {
            successful = onMessageReceived(event, followedString);
        } catch (Throwable e) {
            ExceptionHandler.handleException(e, locale, event.getServerTextChannel().get());
            return;
        } finally {
            removeLoadingReaction();
            setResultReaction(successful);
        }

        if ((this instanceof OnReactionAddListener)) {
            Message reactionMessage = ((OnReactionAddListener) this).getReactionMessage();
            if (reactionMessage != null) {
                reactionUserID = starterMessage.getUserAuthor().get().getId();
                addReactionListener(reactionMessage);
            }
        }

        if ((this instanceof OnForwardedRecievedListener)) {
            Message forwardedMessage = ((OnForwardedRecievedListener) this).getForwardedMessage();
            if (forwardedMessage != null) {
                reactionUserID = starterMessage.getUserAuthor().get().getId();
                addForwarder(forwardedMessage, starterMessage.getServerTextChannel().get(), starterMessage.getUserAuthor().get());
            }
        }

        if (this instanceof OnNavigationListener) {
            addNavigation(navigationMessage, event.getChannel(), event.getMessage().getUserAuthor().get());
        }
    }

    public void onReactionAddSuper(SingleReactionEvent event) {
        updateThreadName();

        if (countdown != null) countdown.reset();

        try {
            ((OnReactionAddListener) this).onReactionAdd(event);
        } catch (Throwable throwable) {
            ExceptionHandler.handleException(throwable, locale, event.getServerTextChannel().get());
        }
    }

    public boolean onForwardedRecievedSuper(MessageCreateEvent event) {
        updateThreadName();

        Response success = null;
        if (commandProperties.withLoadingBar()) addLoadingReaction(event.getMessage());
        if (countdown != null) countdown.reset();

        try {
            success = ((OnForwardedRecievedListener) this).onForwardedRecieved(event);
        } catch (Throwable throwable) {
            ExceptionHandler.handleException(throwable, locale, event.getServerTextChannel().get());
        } finally {
            removeLoadingReaction(event.getMessage());
            if (success != null) setResultReaction(event.getMessage(), success == Response.TRUE);
        }

        return success != null;
    }

    public boolean onNavigationMessageSuper(MessageCreateEvent event, String followedString, boolean firstTime) {
        updateThreadName();
        resetNavigation();

        Response success = null;
        if (firstTime) {
            starterMessage = event.getMessage();
            reactionUserID = starterMessage.getUserAuthor().get().getId();
            ServerTextChannel channel = event.getServerTextChannel().get();

            if (this instanceof HelpCommand && (!channel.canYouWrite() || !channel.canYouAddNewReactions() || !channel.canYouEmbedLinks())) navigationPrivateMessage = true;
        }
        lastUserMessage = event.getMessage();
        if (commandProperties.withLoadingBar()) addLoadingReaction();

        try {
            navigationActive = true;
            if (firstTime) success = onMessageReceived(event, followedString) ? Response.TRUE : Response.FALSE;
            else success = ((OnNavigationListener) this).controllerMessage(event, followedString, state);
            if (success != null || navigationMessage == null) {
                if (countdown != null) countdown.reset();
                drawSuper(event.getApi(), event.getServerTextChannel().get());
            }
        } catch (Throwable throwable) {
            ExceptionHandler.handleException(throwable, locale, event.getServerTextChannel().get());
            return true;
        } finally {
            removeLoadingReaction();
            if (success != null) {
                setResultReaction(event.getMessage(), success == Response.TRUE);
            }
        }

        if (firstTime) {
            if (success == Response.TRUE && navigationActive) {
                for (int i = -1; i < ((OnNavigationListener) this).getMaxReactionNumber(); i++) {
                    if (i == -1) {
                        if (navigationMessage != null) {
                            if (navigationMessage.getChannel().canYouUseExternalEmojis())
                                navigationMessage.addReaction(DiscordApiCollection.getInstance().getBackEmojiCustom());
                            else navigationMessage.addReaction(Settings.BACK_EMOJI);
                        }
                    }
                    if (i >= 0 && navigationMessage != null) {
                        navigationMessage.addReaction(LetterEmojis.LETTERS[i]);
                    }
                }
                if (navigationMessage != null) addNavigation(navigationMessage,navigationMessage.getChannel(), event.getMessage().getUserAuthor().get());
            }
        } else {
            if (success == Response.TRUE) event.getMessage().delete();
        }

        return success != null;
    }

    public void onNavigationReactionSuper(SingleReactionEvent event) {
        updateThreadName();

        //resetNavigation();
        if (countdown != null) countdown.reset();

        int index = -2;
        if ((event.getEmoji().isUnicodeEmoji() && event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(Settings.BACK_EMOJI)) || (event.getEmoji().isCustomEmoji() && event.getEmoji().asCustomEmoji().get().getMentionTag().equalsIgnoreCase(DiscordApiCollection.getInstance().getBackEmojiCustom().getMentionTag())))
            index = -1;
        else {
            for(int i = 0; i < ((OnNavigationListener) this).getMaxReactionNumber(); i++) {
                if (event.getEmoji().isUnicodeEmoji() && event.getEmoji().asUnicodeEmoji().get().equals(LetterEmojis.LETTERS[i])) {
                    index = i;
                    break;
                }
            }
        }
        boolean changed = true;
        try {
            if (index >= 10 && options != null && options.length > 10) {
                if (index == 10) {
                    page--;
                    if (page < 0) page = pageMax;
                } else if (index == 11) {
                    page++;
                    if (page > pageMax) page = 0;
                }
                resetNavigation();
            } else {
                if (options != null && options.length > 10 && index >= 0) index += 10 * page;
                resetNavigation();
                changed = ((OnNavigationListener) this).controllerReaction(event, index, state);
            }

            if (changed) drawSuper(event.getApi(), event.getChannel());
        } catch (Throwable throwable) {
            ExceptionHandler.handleException(throwable, locale, event.getChannel());
        }
    }

    public void drawSuper(DiscordApi api, TextChannel channel) throws Throwable {
        EmbedBuilder eb = ((OnNavigationListener) this).draw(api, state)
                .setTimestampToNow();

        if (options != null && options.length > 0) {
            String[] newOptions;

            if (options.length <= 10) { newOptions = options; }
            else {
                newOptions = new String[12];
                Arrays.fill(newOptions, "");
                if (Math.min(10, options.length - 10 * page) >= 0)
                    System.arraycopy(options, page * 10, newOptions, 0, Math.min(10, options.length - 10 * page));

                newOptions[10] = TextManager.getString(getLocale(), TextManager.GENERAL, "list_previous");
                newOptions[11] = TextManager.getString(getLocale(), TextManager.GENERAL, "list_next");
            }

            String str = EmojiConnection.getOptionsString(channel, false, newOptions);
            eb.addField(Settings.EMPTY_EMOJI, Settings.EMPTY_EMOJI);
            eb.addField(TextManager.getString(locale, TextManager.GENERAL, "options"), str);
        }

        EmbedFactory.addLog(eb, logStatus, log);
        if (options != null && options.length > 10) eb.setFooter(TextManager.getString(getLocale(), TextManager.GENERAL, "list_footer", String.valueOf(page + 1), String.valueOf(pageMax + 1)));

        if (navigationMessage == null) {
                if (navigationPrivateMessage) {
                    if (channel.canYouAddNewReactions()) starterMessage.addReaction("\u2709").get();
                    navigationMessage = starterMessage.getUserAuthor().get().sendMessage(eb).get();
                } else navigationMessage = channel.sendMessage(eb).get();
        } else {
            try {
                navigationMessage.edit(eb).get();
            } catch (Exception e) {
                //Ignore
            }
        }
    }

    private void resetNavigation() {
        log = "";
        logStatus = null;
    }

    public void addLoadingReaction() {
        addLoadingReaction(lastUserMessage);
    }

    private void addLoadingReaction(Message message) {
        try {
            if (
                    loadingStatus != LoadingStatus.FINISHED &&
                            message != null &&
                            message.getCurrentCachedInstance().isPresent() &&
                            message.getChannel().canYouAddNewReactions() &&
                            !loadingBlock &&
                            message.getReactions().stream().map(Reaction::getEmoji)
                                    .noneMatch(emoji -> emoji.equalsEmoji(Objects.requireNonNull(DiscordApiCollection.getInstance().getHomeEmojiById(407189379749117981L))) ||
                                            emoji.equalsEmoji("⏳"))
            ) {
                loadingStatus = LoadingStatus.ONGOING;

                CompletableFuture<Void> loadingBarReaction;
                if (message.getChannel().canYouUseExternalEmojis())
                    loadingBarReaction = message.addReaction(DiscordApiCollection.getInstance().getHomeEmojiById(407189379749117981L));
                else loadingBarReaction = message.addReaction("⏳");

                loadingBarReaction.thenRun(() -> loadingStatus = LoadingStatus.FINISHED);
            }
        } catch (Throwable e) {
            //Ignore
        }
    }

    public void removeLoadingReaction() {
        removeLoadingReaction(lastUserMessage);
    }

    private void removeLoadingReaction(Message message) {
        if (loadingStatus != LoadingStatus.OFF) {
            while (loadingStatus == LoadingStatus.ONGOING) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            loadingStatus = LoadingStatus.OFF;
            if (message.getCurrentCachedInstance().isPresent()) {
                if (message.getChannel().canYouUseExternalEmojis())
                    message.removeOwnReactionByEmoji(DiscordApiCollection.getInstance().getHomeEmojiById(407189379749117981L));
                else message.removeOwnReactionByEmoji("⏳");
            }
        }
    }

    private void setResultReaction(boolean successful) {
        setResultReaction(lastUserMessage, successful);
    }

    private void setResultReaction(Message message, boolean successful) {
        if (message.getChannel().canYouAddNewReactions() && !navigationPrivateMessage) {
            message.addReaction(StringTools.getEmojiForBoolean(successful));
        }
    }

    public void addReactionListener(Message message) {
        reactionMessageID = message.getId();
        CommandContainer.getInstance().addReactionListener(this);
        if (countdown == null) countdown = new Countdown(Settings.TIME_OUT_TIME, Countdown.TimePeriod.MILISECONDS, () -> onTimeOut(message));
    }

    private void addForwarder(Message message, TextChannel forwardChannel, User forwardUser) {
        forwardChannelID = forwardChannel.getId();
        forwardUserID = forwardUser.getId();
        CommandContainer.getInstance().addMessageForwardListener(this);
        if (countdown == null) countdown = new Countdown(Settings.TIME_OUT_TIME, Countdown.TimePeriod.MILISECONDS, () -> onTimeOut(message));
    }

    private void addNavigation(Message message, TextChannel forwardChannel, User forwardUser) {
        forwardChannelID = forwardChannel.getId();
        forwardUserID = forwardUser.getId();
        reactionMessageID = message.getId();
        CommandContainer.getInstance().addReactionListener(this);
        CommandContainer.getInstance().addMessageForwardListener(this);
        if (countdown == null) countdown = new Countdown(Settings.TIME_OUT_TIME, Countdown.TimePeriod.MILISECONDS, () -> onTimeOut(message));
    }

    private void onTimeOut(Message message) {
        if (CommandContainer.getInstance().reactionListenerContains(this)) {
            if (this instanceof OnNavigationListener) {
                try {
                    ((OnNavigationListener) this).onNavigationTimeOut(message);
                    if (commandProperties.deleteOnTimeOut()) deleteNavigationMessage();
                    else removeNavigation();
                } catch (Throwable throwable) {
                    ExceptionHandler.handleException(throwable, locale, message.getServerTextChannel().get());
                }
            } else if (this instanceof OnReactionAddListener) {
                try {
                    if (commandProperties.deleteOnTimeOut()) deleteReactionMessage();
                    else removeReactionListener();
                    ((OnReactionAddListener) this).onReactionTimeOut(message);
                } catch (Throwable throwable) {
                    ExceptionHandler.handleException(throwable, locale, message.getServerTextChannel().get());
                }
            }
        }

        else if (CommandContainer.getInstance().forwarderContains(this)) {
            removeMessageForwarder();
            if (this instanceof OnForwardedRecievedListener) {
                try {
                    ((OnForwardedRecievedListener) this).onForwardedTimeOut();
                } catch (Throwable throwable) {
                    ExceptionHandler.handleException(throwable, locale, message.getServerTextChannel().get());
                }
            }
        }
    }

    public void deleteNavigationMessage() {
        removeNavigation();
        try {
            if (starterMessage.getChannel().canYouManageMessages() && navigationMessage.getChannel() == starterMessage.getChannel())
                starterMessage.getChannel().bulkDelete(navigationMessage, starterMessage).get();
            else navigationMessage.delete().get();
        } catch (InterruptedException | ExecutionException e) {
            //Ignore
        }
    }

    public void deleteReactionMessage() throws ExecutionException, InterruptedException {
        Message reactionMessage = ((OnReactionAddListener) this).getReactionMessage();
        removeReactionListener(reactionMessage);
        try {
            if (starterMessage.getChannel().canYouManageMessages())
                starterMessage.getChannel().bulkDelete(reactionMessage, starterMessage).get();
            else if (reactionMessage != null) reactionMessage.delete().get();
        } catch (InterruptedException | ExecutionException e) {
            //Ignore
            reactionMessage.delete();
        }
    }

    public void removeMessageForwarder() {
        CommandContainer.getInstance().removeForwarder(this);
    }

    public void removeReactionListener() {
        Message message = ((OnReactionAddListener) this).getReactionMessage();
        removeReactionListener(message);
    }

    public void removeReactionListener(Message message) {
        CommandContainer.getInstance().removeReactionListener(this);
        if (message != null && message.getChannel().canYouRemoveReactionsOfOthers()) message.removeAllReactions();
    }

    public void removeNavigation() {
        navigationActive = false;
        removeMessageForwarder();
        removeReactionListener(navigationMessage);
    }

    private void updateThreadName() {
        String name = Thread.currentThread().getName();
        if (name.contains(":")) name = name.split(":")[0];

        Thread.currentThread().setName(name + ":" + getTrigger());
    }

    public void stopCountdown() {
        if (countdown != null) countdown.stop();
    }


    public String getString(String key, String... args) throws IOException {
        String text = TextManager.getString(locale,"commands",commandProperties.trigger()+"_"+key, args);
        if (prefix != null) text = text.replace("%PREFIX", prefix);
        return text;
    }

    public String getString(String key, int option, String... args) throws IOException {
        String text = TextManager.getString(locale,"commands",commandProperties.trigger()+"_"+key, option, args);
        if (prefix != null) text = text.replace("%PREFIX", prefix);
        return text;
    }

    public String getString(String key, boolean secondOption, String... args) throws IOException {
        String text = TextManager.getString(locale,"commands",commandProperties.trigger()+"_"+key, secondOption, args);
        if (prefix != null) text = text.replace("%PREFIX", prefix);
        return text;
    }

    public boolean checkWriteInChannelWithLog(ServerTextChannel channel) {
        if (channel.canYouWrite() && channel.canYouEmbedLinks()) return true;
        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel", "#"+channel.getName()));
        return false;
    }

    public boolean checkManageChannelWithLog(ServerChannel channel) {
        if (PermissionCheck.botHasChannelPermission(channel, PermissionType.MANAGE_CHANNELS)) return true;
        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel_permission", (channel.asTextChannel().isPresent() ? "#" : "") +channel.getName()));
        return false;
    }

    public boolean checkRoleWithLog(Role role) {
        if (PermissionCheck.canYouManageRole(role)) return true;
        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role", false, "@"+role.getName()));
        return false;
    }

    public boolean checkRolesWithLog(List<Role> roles, User requester) {
        ArrayList<Role> unmanagableRoles = new ArrayList<>();

        for(Role role: roles) {
            if (!PermissionCheck.canYouManageRole(role)) unmanagableRoles.add(role);
        }

        if (unmanagableRoles.size() == 0) {
            ArrayList<Role> forbiddenRoles = new ArrayList<>();

            if (requester == null) requester = DiscordApiCollection.getInstance().getYourself();
            for(Role role: roles) {
                if (!PermissionCheck.canManageRole(requester, role)) forbiddenRoles.add(role);
            }

            if (forbiddenRoles.size() == 0) return true;
            try {
                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role_user", forbiddenRoles.size() != 1, MentionTools.getMentionedStringOfRoles(getLocale(), forbiddenRoles).getString().replace("**", "")));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
        try {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role", unmanagableRoles.size() != 1, MentionTools.getMentionedStringOfRoles(getLocale(), unmanagableRoles).getString().replace("**", "")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    //Just getters and setters, nothing important
    public Server getServer() { return starterMessage.getServer().get(); }
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public String getCategory() { return category; }
    public void setLocale(Locale locale) { this.locale = locale; }
    public Locale getLocale() {
        if (locale == null) ExceptionHandler.showErrorLog("Locale is null!");
        return locale;
    }
    public long getReactionMessageID() { return reactionMessageID; }
    public long getForwardChannelID() { return forwardChannelID; }
    public long getForwardUserID() { return forwardUserID; }
    public Message getStarterMessage() { return starterMessage; }
    public boolean isNavigationPrivateMessage() { return navigationPrivateMessage; }
    public void setReactionUserID(long reactionUserID) { this.reactionUserID = reactionUserID; }
    public long getReactionUserID() { return reactionUserID; }
    public void setState(int state) {
        this.options = null;
        this.page = 0;
        this.state = state;
    }
    public Message getNavigationMessage() { return navigationMessage; }


    public String getTrigger() { return commandProperties.trigger(); }
    public String[] getAliases() { return commandProperties.aliases(); }
    public String getThumbnail() { return commandProperties.thumbnail(); }
    public String getEmoji() { return commandProperties.emoji(); }
    public boolean isNsfw() { return commandProperties.nsfw(); }
    public boolean isExecutable() { return commandProperties.executable(); }
    public boolean requiresEmbeds() { return commandProperties.requiresEmbeds(); }
    public int getUserPermissions() { return commandProperties.userPermissions(); }
    public int getCooldownTime() { return commandProperties.cooldownTime(); }
    public int getBotPermissions() {
        int perm = commandProperties.botPermissions();
        if (this instanceof OnReactionAddListener || this instanceof OnNavigationListener || this instanceof OnReactionAddStaticListener) {
            perm |= Permission.ADD_REACTIONS | Permission.READ_MESSAGE_HISTORY;
        }
        if (this instanceof OnReactionAddStaticListener) {
            perm |= Permission.READ_MESSAGE_HISTORY;
        }
        return perm;
    }
    public void blockLoading() { loadingBlock = true; }

    public String[] getOptions() { return options; }
    public void setOptions(String[] options) {
        this.options = options;
        if (options != null) this.pageMax = Math.max(0, options.length - 1) / 10;
    }
    public void setLog(LogStatus logStatus, String string) {
        this.log = string;
        this.logStatus = logStatus;
    }

    public Thread getThread() {
        return thread;
    }

    public static String getTrigger(Class<? extends Command> c) {
        try {
            return CommandManager.createCommandByClass(c).getTrigger();
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return "???";
    }

}