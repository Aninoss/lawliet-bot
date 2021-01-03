package commands;

import commands.cooldownchecker.CooldownManager;
import commands.cooldownchecker.CooldownUserData;
import commands.listeners.OnForwardedRecievedListener;
import commands.listeners.OnNavigationListener;
import commands.listeners.OnReactionAddListener;
import commands.runnables.informationcategory.HelpCommand;
import commands.runnables.informationcategory.PingCommand;
import commands.runnables.utilitycategory.TriggerDeleteCommand;
import commands.runningchecker.RunningCheckerManager;
import constants.*;
import core.*;
import core.cache.PatreonCache;
import core.cache.ServerPatreonBoostCache;
import core.schedule.MainScheduler;
import core.utils.*;
import mysql.modules.commandmanagement.DBCommandManagement;
import mysql.modules.commandusages.DBCommandUsages;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;
import mysql.modules.whitelistedchannels.DBWhiteListedChannels;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandManager {

    private final static int SEC_UNTIL_REMOVAL = 20;

    public static void manage(MessageCreateEvent event, Command command, String followedString, Instant startTime) throws IOException, ExecutionException, InterruptedException, SQLException {
        if (botCanPost(event, command) &&
                isWhiteListed(event, command) &&
                checkCooldown(event, command) &&
                botCanUseEmbeds(event, command) &&
                canRunOnServer(event, command) &&
                isNSFWCompliant(event, command) &&
                checkTurnedOn(event, command) &&
                checkPermissions(event, command) &&
                checkPatreon(event, command) &&
                checkReleased(event, command) &&
                checkRunningCommands(event, command)
        ) {
            if (Bot.isPublicVersion())
                DBCommandUsages.getInstance().getBean(command.getTrigger()).increase();

            cleanPreviousActivities(event.getServer().get(), event.getMessageAuthor().asUser().get());
            manageSlowCommandLoadingReaction(command);
            if (command.isPatreonRequired() && (command.getUserPermissions() & Permission.MANAGE_SERVER) != 0) {
                ServerPatreonBoostCache.getInstance().setTrue(event.getServer().get().getId());
            }

            try {
                sendOverwrittenSignals(event);
                checkTriggerDelete(event);

                if (command instanceof PingCommand) command.getAttachments().put("starting_time", startTime);
                if (command instanceof OnNavigationListener)
                    command.onNavigationMessageSuper(event, followedString, true);
                else
                    command.onReceivedSuper(event, followedString);

                if (Bot.isPublicVersion())
                    maybeSendInvite(event, command.getLocale());
            } catch (Throwable e) {
                ExceptionUtil.handleCommandException(e, command, event.getServerTextChannel().get());
            } finally {
                command.getCompletedListeners().forEach(Runnable::run);
            }
            command.removeLoadingReaction();
        }
    }

    private static void checkTriggerDelete(MessageCreateEvent event) throws ExecutionException {
        ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
        if (serverBean.isCommandAuthorMessageRemove() &&
                ServerPatreonBoostCache.getInstance().get(event.getServer().get().getId()) &&
                PermissionCheckRuntime.getInstance().botHasPermission(serverBean.getLocale(), TriggerDeleteCommand.class, event.getServerTextChannel().get(), Permission.MANAGE_MESSAGES)
        ) {
            event.getMessage().delete(); //no log
        }
    }

    private static void maybeSendInvite(MessageCreateEvent event, Locale locale) {
        User author = event.getMessage().getUserAuthor().get();

        if (new Random().nextInt(200) == 0 &&
                !event.getServer().get().canManage(author) &&
                !event.getChannel().canManageMessages(author) &&
                event.getChannel().canYouEmbedLinks()
        ) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setThumbnail(DiscordApiManager.getInstance().getYourself().getAvatar())
                    .setDescription(TextManager.getString(locale, TextManager.GENERAL, "invite", ExternalLinks.BOT_INVITE_REMINDER_URL));

            event.getChannel().sendMessage(eb).exceptionally(ExceptionLogger.get());
        }
    }

    private static boolean checkRunningCommands(MessageCreateEvent event, Command command) throws ExecutionException, InterruptedException {
        if (command instanceof PingCommand) command.getAttachments().put(0, Instant.now());
        if (RunningCheckerManager.getInstance().canUserRunCommand(
                command,
                event.getMessage().getUserAuthor().get().getId(),
                event.getApi().getCurrentShard(),
                command.getMaxCalculationTimeSec()
        )) {
            return true;
        }

        if (CooldownManager.getInstance().getCooldownData(event.getMessageAuthor().getId()).canPostCooldownMessage()) {
            String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "alreadyused_desc");

            if (event.getChannel().canYouEmbedLinks()) {
                EmbedBuilder eb = EmbedFactory.getEmbedError()
                        .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "alreadyused_title"))
                        .setDescription(desc);
                sendError(event, command.getLocale(), eb);
            } else if (event.getChannel().canYouWrite()) {
                sendErrorNoEmbed(event, command.getLocale(), desc);
            }
        }

        return false;
    }

    private static boolean checkCooldown(MessageCreateEvent event, Command command) throws ExecutionException, InterruptedException {
        if (PatreonCache.getInstance().getUserTier(event.getMessageAuthor().asUser().get().getId()) >= 3) return true;
        CooldownUserData cooldownUserData = CooldownManager.getInstance().getCooldownData(event.getMessageAuthor().getId());

        Optional<Integer> waitingSec = cooldownUserData.getWaitingSec(Settings.COOLDOWN_TIME_SEC);
        if (waitingSec.isEmpty()) {
            return true;
        }

        if (cooldownUserData.canPostCooldownMessage()) {
            String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "cooldown_description", waitingSec.get() != 1, String.valueOf(waitingSec.get()));

            if (event.getChannel().canYouEmbedLinks()) {
                EmbedBuilder eb = EmbedFactory.getEmbedError()
                        .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "cooldown_title"))
                        .setDescription(desc);
                sendError(event, command.getLocale(), eb);
            } else if (event.getChannel().canYouWrite()) {
                sendErrorNoEmbed(event, command.getLocale(), desc);
            }

            Thread.sleep(5000);
        }

        return false;
    }

    private static boolean checkReleased(MessageCreateEvent event, Command command) throws ExecutionException, InterruptedException {
        LocalDate releaseDate = command.getReleaseDate().orElse(LocalDate.now());
        if (!releaseDate.isAfter(LocalDate.now()) || PatreonCache.getInstance().getUserTier(event.getMessageAuthor().asUser().get().getId()) > 1) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_beta_description", ExternalLinks.PATREON_PAGE);
        String waitTime = TextManager.getString(command.getLocale(), TextManager.GENERAL, "next", TimeUtil.getRemainingTimeString(command.getLocale(), Instant.now(), TimeUtil.localDateToInstant(releaseDate), false));

        if (event.getChannel().canYouEmbedLinks()) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setColor(Settings.PATREON_COLOR)
                    .setAuthor(TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_beta_title"), ExternalLinks.PATREON_PAGE, "https://c5.patreon.com/external/favicon/favicon-32x32.png?v=69kMELnXkB")
                    .setDescription(desc);
            EmbedUtil.addLog(eb, LogStatus.TIME, waitTime);
            sendError(event, command.getLocale(), eb);
        } else if (event.getChannel().canYouWrite()) {
            sendErrorNoEmbed(event, command.getLocale(), desc + "\n\n`" + waitTime + "`");
        }

        return false;
    }

    private static boolean checkPatreon(MessageCreateEvent event, Command command) throws ExecutionException, InterruptedException {
        if (!command.isPatreonRequired() || PatreonCache.getInstance().getUserTier(event.getMessageAuthor().asUser().get().getId()) > 1) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_description", ExternalLinks.PATREON_PAGE);

        if (event.getChannel().canYouEmbedLinks()) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setColor(Settings.PATREON_COLOR)
                    .setAuthor(TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_title"), ExternalLinks.PATREON_PAGE, "https://c5.patreon.com/external/favicon/favicon-32x32.png?v=69kMELnXkB")
                    .setDescription(desc);
            sendError(event, command.getLocale(), eb);
        } else if (event.getChannel().canYouWrite()) {
            sendErrorNoEmbed(event, command.getLocale(), desc);
        }

        return false;
    }

    private static boolean checkPermissions(MessageCreateEvent event, Command command) throws ExecutionException, InterruptedException {
        EmbedBuilder errEmbed = PermissionUtil.getUserAndBotPermissionMissingEmbed(command.getLocale(), event.getServer().get(), event.getServerTextChannel().get(), event.getMessage().getUserAuthor().get(), command.getUserPermissions(), command.getBotPermissions());
        if (errEmbed == null || command instanceof HelpCommand) {
            return true;
        }

        if (event.getChannel().canYouWrite() && event.getChannel().canYouEmbedLinks())
            sendError(event, command.getLocale(), errEmbed);
        return false;
    }

    private static boolean checkTurnedOn(MessageCreateEvent event, Command command) throws ExecutionException, InterruptedException {
        Server server = event.getServer().get();
        User user = event.getMessage().getUserAuthor().get();

        if (PermissionUtil.hasAdminPermissions(server, user) ||
                DBCommandManagement.getInstance().getBean(server.getId()).commandIsTurnedOn(command)
        ) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "turnedoff_description");

        if (event.getChannel().canYouEmbedLinks()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "turnedoff_title", command.getPrefix()))
                    .setDescription(desc);
            sendError(event, command.getLocale(), eb);
        } else if (event.getChannel().canYouWrite()) {
            sendErrorNoEmbed(event, command.getLocale(), desc);
        }
        return false;
    }

    private static boolean canRunOnServer(MessageCreateEvent event, Command command) {
        return command.canRunOnServer(event.getServer().get().getId(), event.getMessage().getUserAuthor().get().getId());
    }

    private static boolean botCanUseEmbeds(MessageCreateEvent event, Command command) throws ExecutionException, InterruptedException {
        if (event.getChannel().canYouEmbedLinks() || !command.requiresEmbeds()) {
            return true;
        }

        sendErrorNoEmbed(event, command.getLocale(), TextManager.getString(command.getLocale(), TextManager.GENERAL, "no_embed"));
        return false;
    }

    private static boolean isNSFWCompliant(MessageCreateEvent event, Command command) throws ExecutionException, InterruptedException {
        if (!command.isNsfw() || event.getServerTextChannel().get().isNsfw()) {
            return true;
        }

        EmbedBuilder eb = EmbedFactory.getNSFWBlockEmbed(command.getLocale());
        sendError(event, command.getLocale(), eb);
        return false;
    }

    private static void sendErrorNoEmbed(MessageCreateEvent event, Locale locale, String text) throws ExecutionException, InterruptedException {
        if (event.getChannel().canYouSee() && event.getChannel().canYouWrite() && event.getChannel().canYouEmbedLinks()) {
            Message message = event.getChannel().sendMessage(TextManager.getString(locale, TextManager.GENERAL, "command_block", text, event.getMessage().getUserAuthor().get().getMentionTag())).get();
            autoRemoveMessageAfterCountdown(event, message);
        }
    }

    private static void sendError(MessageCreateEvent event, Locale locale, EmbedBuilder eb) throws ExecutionException, InterruptedException {
        if (event.getChannel().canYouSee() && event.getChannel().canYouWrite() && event.getChannel().canYouEmbedLinks()) {
            eb.setFooter(TextManager.getString(locale, TextManager.GENERAL, "deleteTime", String.valueOf(SEC_UNTIL_REMOVAL)));
            Message message = event.getChannel().sendMessage(event.getMessage().getUserAuthor().get().getMentionTag(), eb).get();
            autoRemoveMessageAfterCountdown(event, message);
        }
    }

    private static void autoRemoveMessageAfterCountdown(MessageCreateEvent event, Message message) {
        MainScheduler.getInstance().schedule(SEC_UNTIL_REMOVAL, ChronoUnit.SECONDS, "command_manager_error_countdown", () -> {
            if (event.getChannel().canYouManageMessages())
                event.getChannel().bulkDelete(message, event.getMessage());
            else
                message.delete();
        });
    }

    private static boolean isWhiteListed(MessageCreateEvent event, Command command) throws ExecutionException, InterruptedException {
        if (event.getServer().get().canManage(event.getMessage().getUserAuthor().get()) || DBWhiteListedChannels.getInstance().getBean(event.getServer().get().getId()).isWhiteListed(event.getServerTextChannel().get().getId())) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "whitelist_description");

        if (event.getChannel().canYouEmbedLinks()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "whitelist_title", command.getPrefix()))
                    .setDescription(desc);
            sendError(event, command.getLocale(), eb);
        } else if (event.getChannel().canYouWrite()) {
            sendErrorNoEmbed(event, command.getLocale(), desc);
        }
        return false;
    }

    private static boolean botCanPost(MessageCreateEvent event, Command command) {
        if (event.getChannel().canYouWrite() || command instanceof HelpCommand) {
            return true;
        }

        if (event.getChannel().canYouAddNewReactions()) {
            if (event.getChannel().canYouUseExternalEmojis())
                event.addReactionsToMessage(DiscordUtil.createCustomEmojiFromTag(Emojis.NO));
            else
                event.addReactionsToMessage("❌");
            event.addReactionsToMessage("✍️");
        }

        if (PermissionUtil.hasAdminPermissions(event.getServer().get(), event.getMessageAuthor().asUser().get()))
            event.getMessage().getUserAuthor().get().sendMessage(TextManager.getString(command.getLocale(), TextManager.GENERAL, "no_writing_permissions", StringUtil.escapeMarkdown(event.getServerTextChannel().get().getName())));

        return false;
    }

    private static void sendOverwrittenSignals(MessageCreateEvent event) {
        ArrayList<Command> list = CommandContainer.getInstance().getMessageForwardInstances();
        for (int i=list.size() - 1; i >= 0; i--) {
            Command command = list.get(i);
            if (command != null &&
                    (event.getChannel().getId() == command.getForwardChannelID() || command.getForwardChannelID() == -1) &&
                    (event.getMessage().getUserAuthor().get().getId() == command.getForwardUserID() || command.getForwardUserID() == -1)
            ) {
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
            if (command != null) {
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
        }

        //Count Reaction Listeners
        list = CommandContainer.getInstance().getReactionInstances();
        for (Command command : list) {
            if (command != null) {
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
        }

        while (openedMessages.size() >= 3) {
            long removeMessageId = openedMessages.get(0);
            openedMessages.remove(0);

            //Remove Forwarded Listeners
            list = CommandContainer.getInstance().getMessageForwardInstances();
            for (Command command : list) {
                if (command != null) {
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
            }

            //Remove Reaction Listeners
            list = CommandContainer.getInstance().getReactionInstances();
            for (Command command : list) {
                if (command != null) {
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
    }

    private static void manageSlowCommandLoadingReaction(Command command) {
        AtomicBoolean commandIsActive = new AtomicBoolean(true);
        command.addCompletedListener(() -> commandIsActive.set(false));

        MainScheduler.getInstance().schedule(3, ChronoUnit.SECONDS, "command_manager_add_loading_reaction", () -> {
            if (commandIsActive.get()) {
                command.addLoadingReaction();
            }
        });

        Thread commandThread = Thread.currentThread();
        MainScheduler.getInstance().schedule(command.getMaxCalculationTimeSec(), ChronoUnit.SECONDS, "command_manager_timeout", () -> {
            if (command.hasTimeOut() && commandIsActive.get()) {
                commandThread.interrupt();
            }
        });
    }

    public static Optional<Command> createCommandByTrigger(String trigger, Locale locale, String prefix) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        Class<? extends Command> clazz = CommandContainer.getInstance().getCommandMap().get(trigger);
        if (clazz == null) return Optional.empty();
        return Optional.of(createCommandByClass(clazz, locale, prefix));
    }

    public static Command createCommandByClassName(String className, Locale locale, String prefix) throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException {
        return createCommandByClass((Class<? extends Command>) Class.forName(className), locale, prefix);
    }

    public static Command createCommandByClass(Class<? extends Command> clazz, Locale locale, String prefix) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        for(Constructor<?> s : clazz.getConstructors()) {
            if (s.getParameterCount() == 2)
                return (Command) s.newInstance(locale, prefix);
        }
        throw new RuntimeException("Invalid class");
    }

}
