package CommandSupporters;

import CommandListeners.*;
import CommandSupporters.CommandLogger.CommandLogger;
import CommandSupporters.CommandLogger.CommandUsage;
import Commands.InformationCategory.HelpCommand;
import Constants.Permission;
import Constants.Settings;
import Core.*;
import CommandSupporters.Cooldown.Cooldown;
import CommandSupporters.RunningCommands.RunningCommandManager;
import Core.Utils.BotUtil;
import MySQL.Modules.CommandManagement.DBCommandManagement;
import MySQL.Modules.CommandUsages.DBCommandUsages;
import MySQL.Modules.WhiteListedChannels.DBWhiteListedChannels;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class CommandManager {

    final static Logger LOGGER = LoggerFactory.getLogger(CommandManager.class);
    final static String EMOJI_NO_EMBED = "❌ ";

    public static void manage(MessageCreateEvent event, Command command, String followedString) throws IOException, ExecutionException, InterruptedException, SQLException {
        if (botCanPost(event, command) &&
                isWhiteListed(event) &&
                botCanUseEmbeds(event, command) &&
                isNSFWCompliant(event, command) &&
                checkTurnedOn(event, command) &&
                checkPermissions(event, command) &&
                checkCooldown(event, command) &&
                checkPatreon(event, command) &&
                checkRunningCommands(event, command)
        ) {
            DBCommandUsages.getInstance().getBean(command.getTrigger()).increase();
            CommandUsers.getInstance().addUsage(event.getMessageAuthor().getId());
            cleanPreviousActivities(event.getServer().get(), event.getMessageAuthor().asUser().get());
            manageSlowCommandLoadingReaction(command, event.getMessage());
            if (command.isPatreonRequired() && (command.getUserPermissions() & Permission.MANAGE_SERVER) != 0) {
                ServerPatreonBoost.getInstance().setTrue(event.getServer().get().getId());
            }

            try {
                sendOverwrittenSignals(event);

                if (command instanceof OnNavigationListener)
                    command.onNavigationMessageSuper(event, followedString, true);
                else
                    command.onRecievedSuper(event, followedString);

                CommandLogger.getInstance().add(event.getServer().get().getId(), new CommandUsage(event.getMessageContent(), CommandUsage.Result.SUCCESS));
            } catch (Throwable e) {
                ExceptionHandler.handleException(e, command.getLocale(), event.getServerTextChannel().get());
            }
            command.removeLoadingReaction();
        }
    }

    private static boolean checkRunningCommands(MessageCreateEvent event, Command command) throws ExecutionException, InterruptedException, SQLException {
        if (BotUtil.getUserDonationStatus(event.getMessageAuthor().asUser().get()) >= 2) return true;

        if (RunningCommandManager.getInstance().canUserRunCommand(event.getMessage().getUserAuthor().get().getId(), event.getApi().getCurrentShard(), command.getMaxCalculationTimeSec())) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "alreadyused_desc");

        if (event.getChannel().canYouEmbedLinks()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "alreadyused_title"))
                    .setDescription(desc);
            event.getChannel().sendMessage(eb).get();
        } else if (event.getChannel().canYouWrite()) {
            event.getChannel().sendMessage(EMOJI_NO_EMBED + desc).get();
        }

        addErrorEmoji(event);
        return false;
    }

    private static boolean checkCooldown(MessageCreateEvent event, Command command) throws ExecutionException, InterruptedException, SQLException {
        if (BotUtil.getUserDonationStatus(event.getMessageAuthor().asUser().get()) >= 2) return true;

        Optional<Integer> waitingSec = Cooldown.getInstance().getWaitingSec(event.getMessageAuthor().asUser().get().getId(), Settings.COOLDOWN_TIME_SEC);
        if (!waitingSec.isPresent()) {
            return true;
        }

        User user = event.getMessageAuthor().asUser().get();
        if (Cooldown.getInstance().isFree(user.getId())) {
            String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "cooldown_description", waitingSec.get() != 1, user.getMentionTag(), String.valueOf(waitingSec.get()));

            if (event.getChannel().canYouEmbedLinks()) {
                EmbedBuilder eb = EmbedFactory.getEmbedError()
                        .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "cooldown_title"))
                        .setDescription(desc);
                event.getChannel().sendMessage(eb).get();
            } else if (event.getChannel().canYouWrite()) {
                event.getChannel().sendMessage(EMOJI_NO_EMBED + desc).get();
            }
            addErrorEmoji(event);
        }

        return false;
    }

    private static boolean checkPatreon(MessageCreateEvent event, Command command) throws SQLException, ExecutionException, InterruptedException {
        if (!command.isPatreonRequired() || BotUtil.getUserDonationStatus(event.getMessageAuthor().asUser().get()) > 0) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_description", Settings.PATREON_PAGE);

        if (event.getChannel().canYouEmbedLinks()) {
            EmbedBuilder eb = EmbedFactory.getEmbed()
                    .setColor(Settings.PATREON_COLOR)
                    .setAuthor(TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_title"), Settings.PATREON_PAGE, "https://c5.patreon.com/external/favicon/favicon-32x32.png?v=69kMELnXkB")
                    .setDescription(desc);
            event.getChannel().sendMessage(eb).get();
        } else if (event.getChannel().canYouWrite()) {
            event.getChannel().sendMessage(EMOJI_NO_EMBED + desc);
        }

        addErrorEmoji(event);
        return false;
    }

    private static boolean checkPermissions(MessageCreateEvent event, Command command) throws ExecutionException, InterruptedException {
        EmbedBuilder errEmbed = PermissionCheck.getUserAndBotPermissionMissingEmbed(command.getLocale(), event.getServer().get(), event.getServerTextChannel().get(), event.getMessage().getUserAuthor().get(), command.getUserPermissions(), command.getBotPermissions());
        if (errEmbed == null || command instanceof HelpCommand) {
            return true;
        }

        if (event.getChannel().canYouEmbedLinks()) event.getChannel().sendMessage(errEmbed).get();
        addErrorEmoji(event);
        return false;
    }

    private static boolean checkTurnedOn(MessageCreateEvent event, Command command) throws ExecutionException, InterruptedException {
        Server server = event.getServer().get();
        User user = event.getMessage().getUserAuthor().get();

        if (PermissionCheck.hasAdminPermissions(server, user) ||
                DBCommandManagement.getInstance().getBean(server.getId()).commandIsTurnedOn(command)
        ) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "turnedoff_description");

        if (event.getChannel().canYouEmbedLinks()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "turnedoff_title"))
                    .setDescription(desc);
            event.getChannel().sendMessage(eb).get();
        } else if (event.getChannel().canYouWrite()) {
            event.getChannel().sendMessage(EMOJI_NO_EMBED + desc).get();
        }

        addErrorEmoji(event);
        return false;
    }

    private static boolean botCanUseEmbeds(MessageCreateEvent event, Command command) {
        if (event.getChannel().canYouEmbedLinks() || !command.requiresEmbeds()) {
            return true;
        }

        event.getChannel().sendMessage("**" + TextManager.getString(command.getLocale(), TextManager.GENERAL, "missing_permissions_title") + "**\n" + TextManager.getString(command.getLocale(), TextManager.GENERAL, "no_embed"));
        addErrorEmoji(event);
        return false;
    }

    private static boolean isNSFWCompliant(MessageCreateEvent event, Command command) throws IOException {
        if (!command.isNsfw() || event.getServerTextChannel().get().isNsfw()) {
            return true;
        }

        event.getChannel().sendMessage(EmbedFactory.getNSFWBlockEmbed(command.getLocale()));
        addErrorEmoji(event);
        return false;
    }

    private static boolean isWhiteListed(MessageCreateEvent event) throws ExecutionException {
        return event.getServer().get().canManage(event.getMessage().getUserAuthor().get()) || DBWhiteListedChannels.getInstance().getBean(event.getServer().get().getId()).isWhiteListed(event.getServerTextChannel().get().getId());
    }

    private static void addErrorEmoji(MessageCreateEvent event) {
        event.getMessage().addReaction("❌");
    }

    private static boolean botCanPost(MessageCreateEvent event, Command command) {
        if (event.getChannel().canYouWrite() || command instanceof HelpCommand) {
            return true;
        }

        if (event.getChannel().canYouAddNewReactions()) {
            event.addReactionsToMessage("❌");
            //event.getMessage().getUserAuthor().get().sendMessage(TextManager.getString(command.getLocale(), TextManager.GENERAL, "no_writing_permissions", event.getServerTextChannel().get().getName()));
        }

        return false;
    }

    private static void sendOverwrittenSignals(MessageCreateEvent event) {
        ArrayList<Command> list = CommandContainer.getInstance().getMessageForwardInstances();
        for (int i=list.size() - 1; i >= 0; i--) {
            Command command = list.get(i);
            if ((event.getChannel().getId() == command.getForwardChannelID() || command.getForwardChannelID() == -1) && (event.getMessage().getUserAuthor().get().getId() == command.getForwardUserID() || command.getForwardUserID() == -1)) {
                if (command instanceof OnForwardedRecievedListener) ((OnForwardedRecievedListener)command).onNewActivityOverwrite();
                else if (command instanceof OnNavigationListener) ((OnNavigationListener)command).onNewActivityOverwrite();
                break;
            }
        }
    }

    private static void cleanPreviousActivities(Server server, User user) {
        ArrayList<Long> openedMessages = new ArrayList<>();

        //Count Forwarded Listeners
        ArrayList<Command> list = CommandContainer.getInstance().getMessageForwardInstances();
        for (Command command : list) {
            Message message = null;
            long activityUserId = command.getReactionUserID();

            if (command instanceof OnForwardedRecievedListener)
                message = ((OnForwardedRecievedListener) command).getForwardedMessage();
            else if (command instanceof OnNavigationListener) message = command.getNavigationMessage();

            if (message != null && message.getServer().isPresent() && message.getServer().get().getId() == server.getId() && activityUserId == user.getId()) {
                long messageID = message.getId();
                if (!openedMessages.contains(messageID)) openedMessages.add(messageID);
            }
        }

        //Count Reaction Listeners
        list = CommandContainer.getInstance().getReactionInstances();
        for (Command command : list) {
            Message message = null;
            long activityUserId = command.getReactionUserID();

            if (command instanceof OnReactionAddListener)
                message = ((OnReactionAddListener) command).getReactionMessage();
            else if (command instanceof OnNavigationListener) message = command.getNavigationMessage();

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
            for (Command command : list) {
                Message message = null;

                if (command instanceof OnForwardedRecievedListener)
                    message = ((OnForwardedRecievedListener) command).getForwardedMessage();
                else if (command instanceof OnNavigationListener) message = command.getNavigationMessage();

                if (message != null && removeMessageId == message.getId()) {
                    if (command instanceof OnNavigationListener) command.removeNavigation();
                    else command.removeReactionListener(message);
                    break;
                }
            }

            //Remove Reaction Listeners
            list = CommandContainer.getInstance().getReactionInstances();
            for (Command command : list) {
                Message message = null;

                if (command instanceof OnReactionAddListener)
                    message = ((OnReactionAddListener) command).getReactionMessage();
                else if (command instanceof OnNavigationListener) message = command.getNavigationMessage();

                if (message != null && removeMessageId == message.getId()) {
                    if (command instanceof OnNavigationListener) command.removeNavigation();
                    else command.removeMessageForwarder();
                    break;
                }
            }
        }
    }

    private static void manageSlowCommandLoadingReaction(Command command, Message userMessage) {
        final Thread commandThread = Thread.currentThread();
        Thread t = new CustomThread(() -> {
            try {
                Thread.sleep(1000);
                if (commandThread.isAlive()) {
                    command.addLoadingReaction();
                    for (int i = 0; i < command.getMaxCalculationTimeSec(); i++) {
                        if (!commandThread.isAlive()) return;
                        Thread.sleep(1000);
                    }

                    //commandThread.interrupt();
                }
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted", e);
            }
        }, "command_slow_loading_reaction_countdown", 1);
        t.start();
    }

    public static Command createCommandByTrigger(String trigger, Locale locale, String prefix) throws IllegalAccessException, InstantiationException {
        Class<? extends Command> clazz = CommandContainer.getInstance().getCommands().get(trigger);
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


    public static Command createCommandByClass(Class<? extends Command> clazz) throws IllegalAccessException, InstantiationException {
        return clazz.newInstance();
    }

    public static Command createCommandByClass(Class<? extends Command> clazz, Locale locale) throws IllegalAccessException, InstantiationException {
        Command command = createCommandByClass(clazz);
        command.setLocale(locale);

        return command;
    }

    public static Command createCommandByClass(Class<? extends Command> clazz, Locale locale, String prefix) throws IllegalAccessException, InstantiationException {
        Command command = createCommandByClass(clazz, locale);
        command.setPrefix(prefix);

        return command;
    }

    public static CommandProperties getCommandProperties(Class<? extends Command> command) {
        return (CommandProperties) command.getAnnotation(CommandProperties.class);
    }

}
