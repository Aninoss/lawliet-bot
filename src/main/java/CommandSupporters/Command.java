package CommandSupporters;

import CommandListeners.*;
import CommandListeners.CommandProperties;
import Constants.*;
import General.*;
import General.EmojiConnection.EmojiConnection;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
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
    private int state = 0;
    private Thread thread;

    private enum LoadingStatus { OFF, ONGOING, FINISHED }

    public Command() {
        commandProperties = this.getClass().getAnnotation(CommandProperties.class);
        category = CategoryCalculator.getCategoryByCommand(this.getClass());
        thread = Thread.currentThread();
    }

    public void onRecievedSuper(MessageCreateEvent event, String followedString) {
        updateThreadName();

        starterMessage = event.getMessage();
        lastUserMessage = event.getMessage();
        if (commandProperties.withLoadingBar()) addLoadingReaction();

        boolean successful = false;
        try {
            successful = ((onRecievedListener) this).onRecieved(event, followedString);
        } catch (Throwable e) {
            ExceptionHandler.handleException(e, locale, event.getServerTextChannel().get());
            return;
        } finally {
            removeLoadingReaction();
            setResultReaction(successful);
        }

        if ((this instanceof onReactionAddListener)) {
            Message reactionMessage = ((onReactionAddListener) this).getReactionMessage();
            if (reactionMessage != null) {
                reactionUserID = starterMessage.getUserAuthor().get().getId();
                addReactionListener(reactionMessage);
            }
        }

        if ((this instanceof onForwardedRecievedListener)) {
            Message forwardedMessage = ((onForwardedRecievedListener) this).getForwardedMessage();
            if (forwardedMessage != null) {
                reactionUserID = starterMessage.getUserAuthor().get().getId();
                addForwarder(forwardedMessage, starterMessage.getServerTextChannel().get(), starterMessage.getUserAuthor().get());
            }
        }

        if (this instanceof onNavigationListener) {
            addNavigation(navigationMessage, event.getChannel(), event.getMessage().getUserAuthor().get());
        }
    }

    public void onReactionAddSuper(SingleReactionEvent event) {
        updateThreadName();

        if (countdown != null) countdown.reset();

        try {
            ((onReactionAddListener) this).onReactionAdd(event);
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
            success = ((onForwardedRecievedListener) this).onForwardedRecieved(event);
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
            if(!channel.canYouWrite() || !channel.canYouAddNewReactions() || !channel.canYouEmbedLinks()) navigationPrivateMessage = true;
        }
        lastUserMessage = event.getMessage();
        if (commandProperties.withLoadingBar()) addLoadingReaction();

        try {
            navigationActive = true;
            success = ((onNavigationListener) this).controllerMessage(event, followedString, state, firstTime);
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
                for (int i = -1; i < ((onNavigationListener) this).getMaxReactionNumber(); i++) {
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

        resetNavigation();
        if (countdown != null) countdown.reset();

        int index = -2;
        if ((event.getEmoji().isUnicodeEmoji() && event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(Settings.BACK_EMOJI)) || (event.getEmoji().isCustomEmoji() && event.getEmoji().asCustomEmoji().get().getMentionTag().equalsIgnoreCase(DiscordApiCollection.getInstance().getBackEmojiCustom().getMentionTag())))
            index = -1;
        else {
            for(int i = 0; i < ((onNavigationListener) this).getMaxReactionNumber(); i++) {
                if (event.getEmoji().isUnicodeEmoji() && event.getEmoji().asUnicodeEmoji().get().equals(LetterEmojis.LETTERS[i])) {
                    index = i;
                    break;
                }
            }
        }
        boolean changed;
        try {
            changed = ((onNavigationListener) this).controllerReaction(event, index, state);
            if (changed) drawSuper(event.getApi(), event.getChannel());
        } catch (Throwable throwable) {
            ExceptionHandler.handleException(throwable, locale, event.getChannel());
        }
    }

    public void drawSuper(DiscordApi api, TextChannel channel) throws Throwable {
        EmbedBuilder eb = ((onNavigationListener) this).draw(api, state)
                .setTimestampToNow();

        if (options != null && options.length > 0) {
            String str = EmojiConnection.getOptionsString(channel, false, options);
            eb.addField(Tools.getEmptyCharacter(), Tools.getEmptyCharacter());
            eb.addField(TextManager.getString(locale, TextManager.GENERAL, "options"), str);
        }

        EmbedFactory.addLog(eb, logStatus, log);

        if (navigationMessage == null) {
            if (navigationPrivateMessage) {
                if (channel.canYouAddNewReactions()) starterMessage.addReaction("\u2709").get();
                navigationMessage = starterMessage.getUserAuthor().get().sendMessage(eb).get();
            }
            else navigationMessage = channel.sendMessage(eb).get();
        }
        else {
            if (navigationMessage.getCurrentCachedInstance().isPresent())
                navigationMessage.edit(eb);
        }
    }

    private void resetNavigation() {
        log = "";
        logStatus = null;
        options = null;
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
                try {
                    message = message.getLatestInstance().get();
                    try {
                        if (message.getChannel().canYouUseExternalEmojis())
                            message.removeOwnReactionByEmoji(DiscordApiCollection.getInstance().getHomeEmojiById(407189379749117981L)).get();
                        else message.removeOwnReactionByEmoji("⏳").get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    //Ignore
                }
            }
        }
    }

    private void setResultReaction(boolean successful) {
        setResultReaction(lastUserMessage, successful);
    }

    private void setResultReaction(Message message, boolean successful) {
        if (message.getChannel().canYouAddNewReactions() && !navigationPrivateMessage) {
            message.addReaction(Tools.getEmojiForBoolean(successful));
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
            if (this instanceof onNavigationListener) {
                try {
                    ((onNavigationListener) this).onNavigationTimeOut(message);
                    if (commandProperties.deleteOnTimeOut()) deleteNavigationMessage();
                    else removeNavigation();
                } catch (Throwable throwable) {
                    ExceptionHandler.handleException(throwable, locale, message.getServerTextChannel().get());
                }
            } else if (this instanceof onReactionAddListener) {
                try {
                    if (commandProperties.deleteOnTimeOut()) deleteReactionMessage();
                    else removeReactionListener();
                    ((onReactionAddListener) this).onReactionTimeOut(message);
                } catch (Throwable throwable) {
                    ExceptionHandler.handleException(throwable, locale, message.getServerTextChannel().get());
                }
            }
        }

        else if (CommandContainer.getInstance().forwarderContains(this)) {
            removeMessageForwarder();
            if (this instanceof onForwardedRecievedListener) {
                try {
                    ((onForwardedRecievedListener) this).onForwardedTimeOut();
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
        Message reactionMessage = ((onReactionAddListener) this).getReactionMessage();
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
        Message message = ((onReactionAddListener) this).getReactionMessage();
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
        if (Tools.canManageChannel(channel, DiscordApiCollection.getInstance().getYourself())) return true;
        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel_permission", "#"+channel.getName()));
        return false;
    }

    public boolean checkRoleWithLog(Role role) {
        if (Tools.canManageRole(role)) return true;
        try {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role", false, "@"+role.getName()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean checkRolesWithLog(ArrayList<Role> roles, User requester) {
        ArrayList<Role> unmanagableRoles = new ArrayList<>();

        for(Role role: roles) {
            if (!Tools.canManageRole(role)) unmanagableRoles.add(role);
        }

        if (unmanagableRoles.size() == 0) {
            ArrayList<Role> forbiddenRoles = new ArrayList<>();

            if (requester == null) requester = DiscordApiCollection.getInstance().getYourself();
            for(Role role: roles) {
                if (!Tools.canManageRole(requester, role)) forbiddenRoles.add(role);
            }

            if (forbiddenRoles.size() == 0) return true;
            try {
                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role_user", forbiddenRoles.size() != 1, Tools.getMentionedStringOfRoles(getLocale(), forbiddenRoles).getString().replace("**", "")));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
        try {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role", unmanagableRoles.size() != 1, Tools.getMentionedStringOfRoles(getLocale(), unmanagableRoles).getString().replace("**", "")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    //Just getters and setters, nothing important
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public String getCategory() { return category; }
    public void setLocale(Locale locale) { this.locale = locale; }
    public Locale getLocale() { return locale; }
    public long getReactionMessageID() { return reactionMessageID; }
    public long getForwardChannelID() { return forwardChannelID; }
    public long getForwardUserID() { return forwardUserID; }
    public Message getStarterMessage() { return starterMessage; }
    public boolean isNavigationPrivateMessage() { return navigationPrivateMessage; }
    public void setReactionUserID(long reactionUserID) { this.reactionUserID = reactionUserID; }
    public long getReactionUserID() { return reactionUserID; }
    public void setState(int state) { this.state = state; }
    public Message getNavigationMessage() { return navigationMessage; }


    public String getTrigger() { return commandProperties.trigger(); }
    public String[] getAliases() { return commandProperties.aliases(); }
    public String getThumbnail() { return commandProperties.thumbnail(); }
    public String getEmoji() { return commandProperties.emoji(); }
    public boolean isNsfw() { return commandProperties.nsfw(); }
    public boolean isPrivate() { return commandProperties.privateUse(); }
    public boolean isExecutable() { return commandProperties.executable(); }
    public int getUserPermissions() { return commandProperties.userPermissions(); }
    public int getBotPermissions() {
        int perm = commandProperties.botPermissions();
        if (this instanceof onReactionAddListener || this instanceof onNavigationListener || this instanceof onReactionAddStatic) {
            perm |= Permission.ADD_NEW_REACTIONS;
        }
        if (this instanceof onReactionAddStatic) {
            perm |= Permission.READ_MESSAGE_HISTORY_OF_TEXT_CHANNEL;
        }
        return perm;
    }
    public void blockLoading() { loadingBlock = true; }

    public String[] getOptions() { return options; }
    public void setOptions(String[] options) { this.options = options; }
    public void setLog(LogStatus logStatus, String string) {
        this.log = string;
        this.logStatus = logStatus;
    }

    public Thread getThread() {
        return thread;
    }

}