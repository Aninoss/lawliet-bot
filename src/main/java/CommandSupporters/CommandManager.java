package CommandSupporters;

import CommandListeners.onForwardedRecievedListener;
import CommandListeners.onNavigationListener;
import CommandListeners.onReactionAddListener;
import CommandListeners.onRecievedListener;
import General.*;
import General.Cooldown.Cooldown;
import General.RunningCommands.RunningCommandManager;
import MySQL.DBBot;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Locale;

public class CommandManager {
    public static void manage(MessageCreateEvent event, Command command, String followedString) throws Throwable {
        Locale locale = command.getLocale();
        if (!command.isPrivate() || event.getMessage().getAuthor().isBotOwner()) {
            if (!command.isNsfw() || event.getServerTextChannel().get().isNsfw()) {
                if (event.getChannel().canYouEmbedLinks() || command.getTrigger().equalsIgnoreCase("help")) {
                    EmbedBuilder errEmbed = PermissionCheck.userAndBothavePermissions(command.getLocale(), event.getServer().get(), event.getChannel(), event.getMessage().getUserAuthor().get(), command.getUserPermissions(), command.getBotPermissions());
                    if (errEmbed == null || command.getTrigger().equalsIgnoreCase("help")) {
                        if (Cooldown.getInstance().canPost(event.getMessageAuthor().asUser().get())) {
                            //Add command usage to database
                            if (!Bot.isDebug()) {
                                new Thread(() -> {
                                    try {
                                        DBBot.addCommandUsage(command.getTrigger());
                                    } catch (Throwable throwable) {
                                        throwable.printStackTrace();
                                    }
                                }).start();
                            }

                            if (event.getServer().isPresent()) cleanPreviousActivities(event.getServer().get(), event.getMessageAuthor().asUser().get());

                            if (RunningCommandManager.getInstance().canUserRunCommand(event.getMessage().getUserAuthor().get(), command.getTrigger())) {
                                try {
                                    if (command instanceof onRecievedListener)
                                        command.onRecievedSuper(event, followedString);
                                    if (command instanceof onNavigationListener)
                                        command.onNavigationMessageSuper(event, followedString, true);
                                } catch (Throwable e) {
                                    ExceptionHandler.handleException(e, locale, event.getServerTextChannel().get());
                                }
                                RunningCommandManager.getInstance().remove(event.getMessage().getUserAuthor().get(), command.getTrigger());
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
                                } catch (Throwable e) {
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
        } else {
            EmbedBuilder eb = new EmbedBuilder()
                    .setColor(Color.RED)
                    .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "botowner_only_title"))
                    .setDescription(TextManager.getString(command.getLocale(), TextManager.GENERAL, "botowner_only_description"));
            event.getChannel().sendMessage(eb);
            event.getMessage().addReaction("❌");
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

    public static Command createCommandByTrigger(String trigger) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class clazz = CommandContainer.getInstance().getCommands().get(trigger);
        if (clazz == null) return null;
        return createCommandByClass(clazz);
    }

    public static Command createCommandByTrigger(String trigger, Locale locale) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class clazz = CommandContainer.getInstance().getCommands().get(trigger);
        if (clazz == null) return null;
        return createCommandByClass(clazz, locale);
    }

    public static Command createCommandByTrigger(String trigger, Locale locale, String prefix) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class clazz = CommandContainer.getInstance().getCommands().get(trigger);
        if (clazz == null) return null;
        return createCommandByClass(clazz, locale, prefix);
    }


    public static Command createCommandByClassName(String className) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return (Command) Class.forName(className).getDeclaredConstructor().newInstance();
    }

    public static Command createCommandByClassName(String className, Locale locale) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Command command = createCommandByClassName(className);
        command.setLocale(locale);

        return command;
    }

    public static Command createCommandByClassName(String className, Locale locale, String prefix) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Command command = createCommandByClassName(className, locale);
        command.setPrefix(prefix);

        return command;
    }


    public static Command createCommandByClass(Class clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return (Command) clazz.getDeclaredConstructor().newInstance();
    }

    public static Command createCommandByClass(Class clazz, Locale locale) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Command command = createCommandByClass(clazz);
        command.setLocale(locale);

        return command;
    }

    public static Command createCommandByClass(Class clazz, Locale locale, String prefix) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Command command = createCommandByClass(clazz, locale);
        command.setPrefix(prefix);

        return command;
    }
}
