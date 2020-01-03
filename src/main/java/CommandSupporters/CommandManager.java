package CommandSupporters;

import CommandListeners.*;
import Constants.Category;
import General.*;
import General.Cooldown.Cooldown;
import General.RunningCommands.RunningCommandManager;
import MySQL.DBBot;
import MySQL.DBServer;
import org.javacord.api.entity.channel.ChannelCategory;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CommandManager {
    public static void manage(MessageCreateEvent event, Command command, String followedString) throws IOException, ExecutionException, InterruptedException, SQLException {
        Locale locale = command.getLocale();
        String commandTrigger = command.getTrigger();
        if (event.getChannel().canYouWrite() || ((commandTrigger.equalsIgnoreCase("help") || commandTrigger.equalsIgnoreCase("commands")) && Tools.canSendPrivateMessage(event.getMessage().getUserAuthor().get()))) {
            if (event.getServer().get().canManage(event.getMessage().getUserAuthor().get()) || DBServer.isChannelWhitelisted(event.getServerTextChannel().get())) {
                if (!command.isPrivate() || event.getMessage().getAuthor().isBotOwner()) {
                    if (!command.isNsfw() || event.getServerTextChannel().get().isNsfw()) {
                        if (event.getChannel().canYouEmbedLinks() || command.getTrigger().equalsIgnoreCase("help")) {
                            EmbedBuilder errEmbed = PermissionCheck.userAndBothavePermissions(command.getLocale(), event.getServer().get(), event.getServerTextChannel().get(), event.getMessage().getUserAuthor().get(), command.getUserPermissions(), command.getBotPermissions());
                            if (errEmbed == null || command.getTrigger().equalsIgnoreCase("help")) {
                                if (Cooldown.getInstance().canPost(event.getMessageAuthor().asUser().get())) {
                                    //Add command usage to database
                                    if (!Bot.isDebug()) {
                                        Thread t = new Thread(() -> {
                                            try {
                                                DBBot.addCommandUsage(command.getTrigger());
                                            } catch (SQLException e) {
                                                e.printStackTrace();
                                            }
                                        });
                                        t.setName("command_usage_add");
                                        t.start();
                                    }

                                    //Only for the "Free Hentai" Server
                                    if (event.getServer().get().getId() == 660212849817288704L && !event.getMessageAuthor().isServerAdmin()) {
                                        boolean ok = false;

                                        ServerTextChannel channel = event.getServerTextChannel().get();
                                        ChannelCategory channelCategory = channel.getCategory().get();

                                        if (channelCategory.getId() == 660260159830097942L) ok = true;
                                        if (channelCategory.getId() == 660815066345635891L &&
                                                (command.getCategory().equalsIgnoreCase(Category.FISHERY) || command.getCategory().equalsIgnoreCase(Category.CASINO))
                                        ) ok = true;
                                        if (channel.getId() == 660262919090470931L &&
                                                (command.getCategory().equalsIgnoreCase(Category.INTERACTIONS) || command.getCategory().equalsIgnoreCase(Category.EMOTES))
                                        ) ok = true;
                                        if (channelCategory.getId() == 660266507774722049L && command.getCategory() == Category.NSFW) ok = true;

                                        if (!ok) {
                                            Message message = event.getChannel().sendMessage("**❌ You aren't allowed to run this command here!**").get();
                                            Thread t = new Thread(() -> {
                                                try {
                                                    Thread.sleep(5000);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                                event.getChannel().deleteMessages(message, event.getMessage());
                                            });
                                            t.start();
                                            return;
                                        }
                                    }

                                    if (event.getServer().isPresent())
                                        cleanPreviousActivities(event.getServer().get(), event.getMessageAuthor().asUser().get());

                                    if (RunningCommandManager.getInstance().canUserRunCommand(event.getMessage().getUserAuthor().get(), command.getTrigger())) {
                                        manageSlowCommandLoadingReaction(command, event.getMessage());
                                        CommandContainer.getInstance().updateLastCommandUsage();

                                        try {
                                            sendOverwrittenMessages(event);
                                            if (command instanceof onRecievedListener)
                                                command.onRecievedSuper(event, followedString);
                                            if (command instanceof onNavigationListener)
                                                command.onNavigationMessageSuper(event, followedString, true);
                                        } catch (Throwable e) {
                                            ExceptionHandler.handleException(e, locale, event.getServerTextChannel().get());
                                        }

                                        RunningCommandManager.getInstance().remove(event.getMessage().getUserAuthor().get(), command.getTrigger());
                                        command.removeLoadingReaction();
                                    } else {
                                        EmbedBuilder eb = EmbedFactory.getEmbedError()
                                                .setTitle(TextManager.getString(locale, TextManager.GENERAL, "alreadyused_title"))
                                                .setDescription(TextManager.getString(locale, TextManager.GENERAL, "alreadyused_desc"));
                                        event.getChannel().sendMessage(eb).get();
                                    }

                                } else {
                                    User user = event.getMessageAuthor().asUser().get();
                                    if (!Cooldown.getInstance().isBotIsSending(user)) {
                                        Cooldown.getInstance().setBotIsSending(user, true);

                                        try {
                                            EmbedBuilder eb = EmbedFactory.getEmbedError()
                                                    .setTitle(TextManager.getString(locale, TextManager.GENERAL, "cooldown_title"))
                                                    .setDescription(TextManager.getString(locale, TextManager.GENERAL, "cooldown_description", user.getMentionTag(), String.valueOf(Cooldown.COOLDOWN_TIME_IN_SECONDS)));
                                            event.getChannel().sendMessage(eb).get();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                        Cooldown.getInstance().setBotIsSending(user, false);
                                    }
                                }
                            } else {
                                event.getChannel().sendMessage(errEmbed);
                            }
                        } else {
                            event.getChannel().sendMessage("**" + TextManager.getString(locale, TextManager.GENERAL, "missing_permissions_title") + "**\n" + TextManager.getString(locale, TextManager.GENERAL, "no_embed"));
                            event.getMessage().addReaction("❌");
                        }
                    } else {
                        event.getChannel().sendMessage(EmbedFactory.getNSFWBlockEmbed(command.getLocale()));
                        event.getMessage().addReaction("❌");
                    }
                }
            }
        } else {
            if (event.getChannel().canYouAddNewReactions()) {
                event.addReactionsToMessage("✏");
                event.addReactionsToMessage("❌");
                User user = event.getMessage().getUserAuthor().get();
                if (Tools.canSendPrivateMessage(user))
                    user.sendMessage(TextManager.getString(locale, TextManager.GENERAL, "no_writing_permissions", event.getServerTextChannel().get().getName())).get();
            }
        }
    }

    private static void sendOverwrittenMessages(MessageCreateEvent event) {
        ArrayList<Command> list = CommandContainer.getInstance().getMessageForwardInstances();
        for (int i=list.size()-1; i >= 0; i--) {
            Command command = list.get(i);
            if ((event.getChannel().getId() == command.getForwardChannelID() || command.getForwardChannelID() == -1) && (event.getMessage().getUserAuthor().get().getId() == command.getForwardUserID() || command.getForwardUserID() == -1)) {
                if (command instanceof onForwardedRecievedListener) ((onForwardedRecievedListener)command).onNewActivityOverwrite();
                else if (command instanceof onNavigationListener) ((onNavigationListener)command).onNewActivityOverwrite();
                break;
            }
        }
    }

    private static void cleanPreviousActivities(Server server, User user) {
        ArrayList<Long> openedMessages = new ArrayList<>();

        //Count Forwarded Listeners
        ArrayList<Command> list = CommandContainer.getInstance().getMessageForwardInstances();
        for (int i = 0; i < list.size(); i++) {
            Command command = list.get(i);
            Message message = null;
            long activityUserId = command.getReactionUserID();

            if (command instanceof onForwardedRecievedListener) message = ((onForwardedRecievedListener) command).getForwardedMessage();
            else if (command instanceof onNavigationListener) message = command.getNavigationMessage();

            if (message != null && message.getServer().isPresent() && message.getServer().get().getId() == server.getId() && activityUserId == user.getId()) {
                long messageID = message.getId();
                if (!openedMessages.contains(messageID)) openedMessages.add(messageID);
            }
        }

        //Count Reaction Listeners
        list = CommandContainer.getInstance().getReactionInstances();
        for (int i = 0; i < list.size(); i++) {
            Command command = list.get(i);
            Message message = null;
            long activityUserId = command.getReactionUserID();

            if (command instanceof onReactionAddListener) message = ((onReactionAddListener) command).getReactionMessage();
            else if (command instanceof onNavigationListener) message = command.getNavigationMessage();

            if (message != null && message.getServer().isPresent() && message.getServer().get().getId() == server.getId() && activityUserId == user.getId()) {
                long messageID = message.getId();
                if (!openedMessages.contains(messageID)) openedMessages.add(messageID);
            }
        }

        while (openedMessages.size() >= 3) {
            long removeMessageId = openedMessages.get(0);
            openedMessages.remove(0);

            //Remove Forwarded Listeners
            list = CommandContainer.getInstance().getMessageForwardInstances();
            for (int i = 0; i < list.size(); i++) {
                Command command = list.get(i);
                Message message = null;

                if (command instanceof onForwardedRecievedListener) message = ((onForwardedRecievedListener) command).getForwardedMessage();
                else if (command instanceof onNavigationListener) message = command.getNavigationMessage();

                if (message != null && removeMessageId == message.getId()) {
                    if (command instanceof onNavigationListener) command.removeNavigation();
                    else command.removeReactionListener(message);
                    break;
                }
            }

            //Remove Reaction Listeners
            list = CommandContainer.getInstance().getReactionInstances();
            for (int i = 0; i < list.size(); i++) {
                Command command = list.get(i);
                Message message = null;

                if (command instanceof onReactionAddListener) message = ((onReactionAddListener) command).getReactionMessage();
                else if (command instanceof onNavigationListener) message = command.getNavigationMessage();

                if (message != null && removeMessageId == message.getId()) {
                    if (command instanceof onNavigationListener) command.removeNavigation();
                    else command.removeMessageForwarder();
                    break;
                }
            }
        }
    }

    private static void manageSlowCommandLoadingReaction(Command command, Message userMessage) {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(1000);

                if (RunningCommandManager.getInstance().find(userMessage.getUserAuthor().get(), command.getTrigger()) != null) {
                    command.addLoadingReaction();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t.setName("command_slow_loading_reaction_countdown");
        t.start();
    }

    public static Command createCommandByTrigger(String trigger) throws IllegalAccessException, InstantiationException {
        Class clazz = CommandContainer.getInstance().getCommands().get(trigger);
        if (clazz == null) return null;
        return createCommandByClass(clazz);
    }

    public static Command createCommandByTrigger(String trigger, Locale locale) throws IllegalAccessException, InstantiationException {
        Class clazz = CommandContainer.getInstance().getCommands().get(trigger);
        if (clazz == null) return null;
        return createCommandByClass(clazz, locale);
    }

    public static Command createCommandByTrigger(String trigger, Locale locale, String prefix) throws IllegalAccessException, InstantiationException {
        Class clazz = CommandContainer.getInstance().getCommands().get(trigger);
        if (clazz == null) return null;
        return createCommandByClass(clazz, locale, prefix);
    }


    public static Command createCommandByClassName(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return (Command) Class.forName(className).newInstance();
    }

    public static Command createCommandByClassName(String className, Locale locale) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Command command = createCommandByClassName(className);
        command.setLocale(locale);

        return command;
    }

    public static Command createCommandByClassName(String className, Locale locale, String prefix) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Command command = createCommandByClassName(className, locale);
        command.setPrefix(prefix);

        return command;
    }


    public static Command createCommandByClass(Class clazz) throws IllegalAccessException, InstantiationException {
        return (Command) clazz.newInstance();
    }

    public static Command createCommandByClass(Class clazz, Locale locale) throws IllegalAccessException, InstantiationException {
        Command command = createCommandByClass(clazz);
        command.setLocale(locale);

        return command;
    }

    public static Command createCommandByClass(Class clazz, Locale locale, String prefix) throws IllegalAccessException, InstantiationException {
        Command command = createCommandByClass(clazz, locale);
        command.setPrefix(prefix);

        return command;
    }

    public static CommandProperties getCommandProperties(Class command) {
        return (CommandProperties) command.getAnnotation(CommandProperties.class);
    }

}
