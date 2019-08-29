package CommandSupporters;

import CommandListeners.*;
import Constants.*;
import General.*;
import General.EmojiConnection.EmojiConnection;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Command {
    protected String trigger, category, thumbnail, prefix, emoji;
    protected boolean nsfw, withLoadingBar, privateUse, executable, deleteOnTimeOut = false;
    protected int botPermissions = 0, userPermissions = 0, state = 0;
    protected Locale locale;
    protected String[] options;
    private LogStatus logStatus = null;
    private long reactionMessageID = -1, reactionUserID, forwardChannelID = -1, forwardUserID = -1, startTime;
    private CompletableFuture<Void> loadingBarReaction;
    private Message navigationMessage, authorMessage;
    private boolean successful, navigationActive, navigationPrivateMessage = false;;
    private String log;


    public Command() {
        category = CategoryCalculator.getCategoryByCommand(this);
    }

    public void onRecievedSuper(MessageCreateEvent event, String followedString) {
        successful = false;
        if (withLoadingBar) addLoadingReaction(event.getMessage());

        Runnable r = () -> setResultReaction(event.getMessage(), successful);
        ExceptionHandler.handleUncaughtException(locale, event.getServerTextChannel().get(), r);

        try {
            successful = ((onRecievedListener) this).onRecieved(event, followedString);
        } catch (Throwable throwable) {
            ExceptionHandler.handleException(throwable, locale, event.getServerTextChannel().get());
        }

        if ((this instanceof onReactionAddListener)) {
            Message reactionMessage;
            reactionMessage = ((onReactionAddListener) this).getReactionMessage();

            if (reactionMessage != null) {
                authorMessage = event.getMessage();
                reactionUserID = event.getMessage().getAuthor().getId();
                addReactionListener(reactionMessage);
            }
        }

        if ((this instanceof onForwardedRecievedListener)) {
            Message forwardedMessage = ((onForwardedRecievedListener) this).getForwardedMessage();

            if (forwardedMessage != null) {
                authorMessage = event.getMessage();
                addForwarder(authorMessage.getServerTextChannel().get(), authorMessage.getUserAuthor().get());
            }
        }

        if (this instanceof onNavigationListener) {
            deleteOnTimeOut = true;
            reactionUserID = event.getMessage().getAuthor().getId();
            addNavigation(navigationMessage, event.getChannel(),event.getMessage().getUserAuthor().get());
        }

        r.run();
    }

    public boolean onNavigationMessageSuper(MessageCreateEvent event, String followedString, boolean firstTime) {
        resetNavigation();
        successful = false;
        Response success = null;
        if (withLoadingBar) addLoadingReaction(event.getMessage());
        if (firstTime) {
            authorMessage = event.getMessage();
            reactionUserID = event.getMessage().getAuthor().getId();
            ServerTextChannel channel = event.getServerTextChannel().get();
            if(!channel.canYouWrite() || !channel.canYouAddNewReactions() || !channel.canYouEmbedLinks()) navigationPrivateMessage = true;
        }

        Runnable r = () -> setResultReaction(event.getMessage(), successful);
        ExceptionHandler.handleUncaughtException(locale, event.getServerTextChannel().get(), r);

        try {
            navigationActive = true;
            success = ((onNavigationListener) this).controllerMessage(event, followedString, firstTime);
            successful = success == Response.TRUE;
            if (success != null || navigationMessage == null) {
                startTime = System.currentTimeMillis();
                drawSuper(event.getApi(), event.getServerTextChannel().get());
            }
        } catch (Throwable throwable) {
            ExceptionHandler.handleException(throwable, locale, event.getServerTextChannel().get());
        }

        if (firstTime) {
            if (successful && navigationActive) {
                for (int i = -1; i < ((onNavigationListener) this).getMaxReactionNumber(); i++) {
                    if (i == -1) {
                        if (navigationMessage != null) {
                            if (navigationMessage.getChannel().canYouUseExternalEmojis())
                                navigationMessage.addReaction(Shortcuts.getBackEmojiCustom(event.getApi()));
                            else navigationMessage.addReaction(Shortcuts.getBackEmojiUnicode());
                        }
                    }
                    if (i >= 0 && navigationMessage != null) {
                        navigationMessage.addReaction(LetterEmojis.LETTERS[i]);
                    }
                }
                //if (navigationPrivateMessage) addNavigation(navigationMessage,event.getMessage().getUserAuthor().get().getPrivateChannel().get(), event.getMessage().getUserAuthor().get());
                //else
                if (navigationMessage != null) addNavigation(navigationMessage,navigationMessage.getChannel(), event.getMessage().getUserAuthor().get());
            }
        } else {
            if (success == Response.TRUE) event.getMessage().delete();
        }

        if (success != null) r.run();

        return success != null;
    }

    public void onNavigationReactionSuper(SingleReactionEvent event) {
        resetNavigation();
        startTime = System.currentTimeMillis();

        Runnable r = null;
        //if (event instanceof ReactionAddEvent) r = ((ReactionAddEvent)event)::removeReaction;
        ExceptionHandler.handleUncaughtException(locale, event.getChannel(), r);

        int index = -2;
        if ((event.getEmoji().isUnicodeEmoji() && event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(Shortcuts.getBackEmojiUnicode())) || (event.getEmoji().isCustomEmoji() && event.getEmoji().asCustomEmoji().get().getMentionTag().equalsIgnoreCase(Shortcuts.getBackEmojiCustom(event.getApi()).getMentionTag())))
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
            changed = ((onNavigationListener) this).controllerReaction(event, index);
            if (changed) drawSuper(event.getApi(), event.getChannel());
        } catch (Throwable throwable) {
            ExceptionHandler.handleException(throwable, locale, event.getChannel());
        }

        if (r != null) r.run();
    }

    public void drawSuper(DiscordApi api, TextChannel channel) throws Throwable {
        EmbedBuilder eb = ((onNavigationListener) this).draw(api)
                .setTimestampToNow();

        if (options != null && options.length > 0) {
            String str = EmojiConnection.getOptionsString(channel, false, options);
            eb.addField(Tools.getEmptyCharacter(), Tools.getEmptyCharacter());
            eb.addField(TextManager.getString(locale, TextManager.GENERAL, "options"), str);
        }

        EmbedFactory.addLog(eb, logStatus, log);

        if (navigationMessage == null) {
            if (navigationPrivateMessage) {
                if (channel.canYouAddNewReactions()) authorMessage.addReaction("\u2709").get();
                navigationMessage = authorMessage.getUserAuthor().get().sendMessage(eb).get();
            }
            else navigationMessage = channel.sendMessage(eb).get();
        }
        else {
            if (navigationMessage.getCurrentCachedInstance().isPresent())
                navigationMessage.edit(eb);
        }
    }

    public void deleteNavigationMessage() throws Throwable {
        removeNavigation();
        if (authorMessage.getChannel().canYouManageMessages() && navigationMessage.getChannel() == authorMessage.getChannel()) authorMessage.getChannel().bulkDelete(navigationMessage,authorMessage).get();
        else navigationMessage.delete().get();
    }

    public void deleteReactionMessage() throws Throwable {
        Message reactionMessage = null;
        if (this instanceof onReactionAddListener) reactionMessage = ((onReactionAddListener) this).getReactionMessage();
        removeReactionListener(reactionMessage);
        if (authorMessage.getChannel().canYouManageMessages()) authorMessage.getChannel().bulkDelete(reactionMessage, authorMessage).get();
        else if (reactionMessage != null) reactionMessage.delete().get();
    }

    public void deleteForwardedMessage() throws Throwable {
        Message forwardedMessage = ((onForwardedRecievedListener) this).getForwardedMessage();
        removeNavigation();
        if (authorMessage.getChannel().canYouManageMessages()) authorMessage.getChannel().bulkDelete(forwardedMessage,authorMessage).get();
        else if (forwardedMessage != null) forwardedMessage.delete().get();
    }

    public void onForwardedRecievedSuper(MessageCreateEvent event) {
        successful = false;
        startTime = System.currentTimeMillis();
        if (withLoadingBar) addLoadingReaction(event.getMessage());

        Runnable r = () -> setResultReaction(event.getMessage(), successful);
        ExceptionHandler.handleUncaughtException(locale, event.getServerTextChannel().get(), r);

        try {
            successful = ((onForwardedRecievedListener) this).onForwardedRecieved(event);
        } catch (Throwable throwable) {
            ExceptionHandler.handleException(throwable, locale, event.getServerTextChannel().get());
        }

        r.run();
    }

    public void onReactionAddSuper(SingleReactionEvent event) {
        startTime = System.currentTimeMillis();

        Runnable r = null;
        //if (event instanceof ReactionAddEvent) r = ((ReactionAddEvent)event)::removeReaction;
        ExceptionHandler.handleUncaughtException(locale, event.getServerTextChannel().get(), r);

        try {
            ((onReactionAddListener) this).onReactionAdd(event);
        } catch (Throwable throwable) {
            ExceptionHandler.handleException(throwable, locale, event.getServerTextChannel().get());
        }

        if (r != null) r.run();
    }

    public void navigationFinish() {
        removeNavigation();
    }

    public void setResultReaction(Message message, boolean successful) {
        removeLoadingReaction(message);
        if (message.getChannel().canYouAddNewReactions() && !navigationPrivateMessage) {
            message.addReaction(Tools.getEmojiForBoolean(successful));
        }
    }

    public void addReactionListener(Message message) {
        reactionMessageID = message.getId();
        CommandContainer.getInstance().addReactionListener(this);
        new Thread(() -> manageTimeOut(message)).start();
    }

    private void addForwarder(TextChannel forwardChannel, User forwardUser) {
        addForwarder(forwardChannel);
        forwardUserID = forwardUser.getId();
    }

    private void addForwarder(TextChannel forwardChannel) {
        if (forwardChannel != null) forwardChannelID = forwardChannel.getId();
        CommandContainer.getInstance().addMessageForwardListener(this);
        new Thread(() -> manageTimeOut(null)).start();
    }

    private void addNavigation(Message message, TextChannel forwardChannel, User forwardUser) {
        if (forwardChannel != null) forwardChannelID = forwardChannel.getId();
        if (forwardUser != null) forwardUserID = forwardUser.getId();
        reactionMessageID = message.getId();
        CommandContainer.getInstance().addReactionListener(this);
        CommandContainer.getInstance().addMessageForwardListener(this);
        new Thread(() -> manageTimeOut(message)).start();
    }

    private void manageTimeOut(Message message) {
        startTime = System.currentTimeMillis();
        while(System.currentTimeMillis() < (startTime + Settings.TIME_OUT_TIME)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (CommandContainer.getInstance().reactionListenerContains(this)) {
            CommandContainer.getInstance().removeReactionListener(this);
            if (message != null && message.getChannel().canYouRemoveReactionsOfOthers()) message.removeAllReactions();
            if (this instanceof onNavigationListener) {
                try {
                    ((onNavigationListener) this).onNavigationTimeOut(message);
                    if (deleteOnTimeOut) deleteNavigationMessage();
                    else removeNavigation();
                } catch (Throwable throwable) {
                    ExceptionHandler.handleException(throwable, locale, message.getServerTextChannel().get());
                }
            } else {
                try {
                    if (deleteOnTimeOut) deleteReactionMessage();
                    else removeReactionListener();
                } catch (Throwable throwable) {
                    ExceptionHandler.handleException(throwable, locale, message.getServerTextChannel().get());
                }
            }
            if (this instanceof onReactionAddListener) {
                try {
                    ((onReactionAddListener) this).onReactionTimeOut(message);
                } catch (Throwable throwable) {
                    ExceptionHandler.handleException(throwable, locale, message.getServerTextChannel().get());
                }
            }
        }
        if (CommandContainer.getInstance().forwarderContains(this)) {
            CommandContainer.getInstance().removeForwarder(this);
            if (this instanceof onForwardedRecievedListener) {
                try {
                    ((onForwardedRecievedListener) this).onForwardedTimeOut();
                } catch (Throwable throwable) {
                    ExceptionHandler.handleException(throwable, locale, message.getServerTextChannel().get());
                }
            }
        }
    }

    public void removeMessageForwarder() {
        CommandContainer.getInstance().removeForwarder(this);
    }

    public void removeReactionListener() {
        Message message = null;
        if (this instanceof onReactionAddListener)
            message = ((onReactionAddListener) this).getReactionMessage();

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

    public void addLoadingReaction(Message message) {
        try {
            if (message != null && (message.getCurrentCachedInstance().isPresent() || (message = message.getServerTextChannel().get().getMessageById(message.getId()).get()) != null)) {
                if (message.getChannel().canYouAddNewReactions()) {
                    if (message.getChannel().canYouUseExternalEmojis())
                        loadingBarReaction = message.addReaction(Shortcuts.getCustomEmojiByID(message.getApi(), 407189379749117981L));
                    else loadingBarReaction = message.addReaction("⏳");
                }
            }
        } catch (Throwable e) {
            //Ignore
        }
    }

    public void removeLoadingReaction(Message message) {
        if (loadingBarReaction != null) {
            try {
                loadingBarReaction.get();
                if (message.getChannel().canYouRemoveReactionsOfOthers() && message.getServer().isPresent()) message.removeAllReactions();
                else {
                    if (message.getChannel().canYouUseExternalEmojis()) message.removeOwnReactionByEmoji(Shortcuts.getCustomEmojiByID(message.getApi(), 407189379749117981L)).get();
                    else message.removeOwnReactionByEmoji("⏳").get();
                }
            } catch (InterruptedException | ExecutionException e) {
                //Ignore
            }
        }
    }

    public String getTrigger() {
        return trigger;
    }

    public String getCategory() {
        return category;
    }

    public boolean isNsfw() {
        return nsfw;
    }

    public long getReactionMessageID() {
        return reactionMessageID;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public long getForwardChannelID() {
        return forwardChannelID;
    }

    public long getForwardUserID() {
        return forwardUserID;
    }

    public boolean isPrivate() {
        return privateUse;
    }

    public int getUserPermissions() {
        return userPermissions;
    }

    public int getBotPermissions() {
        int perm = botPermissions;
        if (this instanceof onReactionAddListener || this instanceof onNavigationListener || this instanceof onReactionAddStatic) {
            perm = perm | Permission.ADD_NEW_REACTIONS;
        }
        return perm;
    }

    public String getString(String key, String... args) throws Throwable {
        String text = TextManager.getString(locale,"commands",trigger+"_"+key, args);
        if (prefix != null) text = text.replace("%PREFIX", prefix);
        return text;
    }

    public String getString(String key, int option, String... args) throws Throwable {
        String text = TextManager.getString(locale,"commands",trigger+"_"+key, option, args);
        if (prefix != null) text = text.replace("%PREFIX", prefix);
        return text;
    }

    public String getString(String key, boolean secondOption, String... args) throws Throwable {
        String text = TextManager.getString(locale,"commands",trigger+"_"+key, secondOption, args);
        if (prefix != null) text = text.replace("%PREFIX", prefix);
        return text;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getEmoji() {
        return emoji;
    }

    public void resetNavigation() {
        log = "";
        logStatus = null;
        options = null;
    }

    public void setLog(LogStatus logStatus, String string) {
        this.log = string;
        this.logStatus = logStatus;
    }

    public String getLog() {
        return log;
    }

    public String getPrefix() {
        return prefix;
    }

    public Message getAuthorMessage() {
        return authorMessage;
    }

    public boolean isExecutable() {
        return executable;
    }

    public void setWithLoadingBar(boolean withLoadingBar) {
        this.withLoadingBar = withLoadingBar;
    }

    public Message getNavigationMessage() {
        return navigationMessage;
    }

    public long getReactionUserID() {
        return reactionUserID;
    }

    public void setReactionUserID(long reactionUserID) {
        this.reactionUserID = reactionUserID;
    }

    public boolean isNavigationPrivateMessage() {
        return navigationPrivateMessage;
    }


}
