package commands;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import commands.cooldownchecker.CoolDownManager;
import commands.cooldownchecker.CoolDownUserData;
import commands.listeners.OnButtonListener;
import commands.listeners.OnMessageInputListener;
import commands.listeners.OnReactionListener;
import commands.listeners.OnSelectionMenuListener;
import commands.runnables.informationcategory.HelpCommand;
import commands.runnables.informationcategory.PingCommand;
import commands.runningchecker.RunningCheckerManager;
import constants.Emojis;
import constants.ExternalLinks;
import constants.Settings;
import core.*;
import core.cache.PatreonCache;
import core.cache.ServerPatreonBoostCache;
import core.components.ActionRows;
import core.schedule.MainScheduler;
import core.utils.*;
import mysql.modules.commandmanagement.DBCommandManagement;
import mysql.modules.whitelistedchannels.DBWhiteListedChannels;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.TimeFormat;

public class CommandManager {

    private final static int SEC_UNTIL_REMOVAL = 20;
    private final static Random random = new Random();

    public static void manage(CommandEvent event, Command command, String args, Instant startTime) {
        manage(event, command, args, startTime, true);
    }

    public static void manage(CommandEvent event, Command command, String args, Instant startTime, boolean freshCommand) {
        if (checkCoolDown(event, command) &&
                checkRunningCommands(event, command)
        ) {
            process(event, command, args, startTime, freshCommand);
        }

        command.getCompletedListeners().forEach(runnable -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                MainLogger.get().error("Error on completed listener", e);
            }
        });
    }

    private static void process(CommandEvent event, Command command, String args, Instant startTime, boolean freshCommand) {
        if (botCanPost(event, command) &&
                isWhiteListed(event, command) &&
                botCanUseEmbeds(event, command) &&
                canRunOnGuild(event, command) &&
                isNSFWCompliant(event, command) &&
                checkTurnedOn(event, command) &&
                checkPermissions(event, command) &&
                checkPatreon(event, command) &&
                checkReleased(event, command)
        ) {
            if (command.getCommandProperties().patreonRequired() &&
                    (Arrays.stream(command.getCommandProperties().userGuildPermissions()).anyMatch(p -> p == Permission.MANAGE_SERVER))
            ) {
                ServerPatreonBoostCache.setTrue(event.getGuild().getIdLong());
            }

            try {
                cleanPreviousListeners(command, event.getMember());
                sendOverwrittenSignals(command, event.getMember());

                if (command instanceof PingCommand) {
                    command.getAttachments().put("starting_time", startTime);
                }

                boolean success = command.processTrigger(event, args, freshCommand);
                if (success && Program.publicVersion()) {
                    maybeSendBotInvite(event, command.getLocale());
                }
            } catch (Throwable e) {
                ExceptionUtil.handleCommandException(e, command);
            } finally {
                CommandContainer.cleanUp();
            }
        }
    }

    private static void maybeSendBotInvite(CommandEvent event, Locale locale) {
        if (random.nextInt(180) == 0 &&
                !BotPermissionUtil.can(event.getMember(), Permission.MANAGE_SERVER) &&
                !BotPermissionUtil.can(event.getMember(), Permission.MESSAGE_MANAGE) &&
                (BotPermissionUtil.canWriteEmbed(event.getChannel()) || event.isSlashCommandEvent())
        ) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setThumbnail(ShardManager.getSelf().getAvatarUrl())
                    .setDescription(TextManager.getString(locale, TextManager.GENERAL, "invite"));

            Button button = Button.of(ButtonStyle.LINK, ExternalLinks.BOT_INVITE_REMINDER_URL, TextManager.getString(locale, TextManager.GENERAL, "invite_button"));
            event.getChannel().sendMessageEmbeds(eb.build())
                    .setActionRows(ActionRows.of(button))
                    .queue();
        }
    }

    private static boolean checkRunningCommands(CommandEvent event, Command command) {
        if (command instanceof PingCommand) command.getAttachments().put("start_instant", Instant.now());
        if (RunningCheckerManager.canUserRunCommand(
                command,
                event.getGuild().getIdLong(),
                event.getMember().getIdLong(),
                event.getJDA().getShardInfo().getShardId(),
                command.getCommandProperties().maxCalculationTimeSec()
        )) {
            return true;
        }

        if (CoolDownManager.getCoolDownData(event.getMember().getIdLong()).canPostCoolDownMessage()) {
            String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "alreadyused_desc");

            if (BotPermissionUtil.canWriteEmbed(event.getChannel()) || event.isSlashCommandEvent()) {
                EmbedBuilder eb = EmbedFactory.getEmbedError()
                        .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "alreadyused_title"))
                        .setDescription(desc);
                sendError(event, command.getLocale(), eb, true);
            } else if (BotPermissionUtil.canWrite(event.getChannel())) {
                sendErrorNoEmbed(event, command.getLocale(), desc, true);
            }
        }

        return false;
    }

    private static boolean checkCoolDown(CommandEvent event, Command command) {
        if (PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), true) || PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong())) {
            return true;
        }
        CoolDownUserData cooldownUserData = CoolDownManager.getCoolDownData(event.getMember().getIdLong());

        Optional<Integer> waitingSec = cooldownUserData.getWaitingSec(Settings.COOLDOWN_TIME_SEC);
        if (waitingSec.isEmpty()) {
            return true;
        }

        if (cooldownUserData.canPostCoolDownMessage()) {
            String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "cooldown_description", waitingSec.get() != 1, String.valueOf(waitingSec.get()));

            if (BotPermissionUtil.canWriteEmbed(event.getChannel()) || event.isSlashCommandEvent()) {
                EmbedBuilder eb = EmbedFactory.getEmbedError()
                        .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "cooldown_title"))
                        .setDescription(desc);
                sendError(event, command.getLocale(), eb, false);
            } else if (BotPermissionUtil.canWrite(event.getChannel())) {
                sendErrorNoEmbed(event, command.getLocale(), desc, false);
            }
        }

        return false;
    }

    private static boolean checkReleased(CommandEvent event, Command command) {
        LocalDate releaseDate = command.getReleaseDate().orElse(LocalDate.now());
        if (!releaseDate.isAfter(LocalDate.now()) ||
                PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), true) ||
                PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong())
        ) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_beta_description");
        String waitTime = TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_beta_releaseday", TimeFormat.DATE_TIME_SHORT.atInstant(TimeUtil.localDateToInstant(releaseDate)).toString());

        if (BotPermissionUtil.canWriteEmbed(event.getChannel()) || event.isSlashCommandEvent()) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setColor(Settings.PREMIUM_COLOR)
                    .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_beta_title"))
                    .setDescription(desc);
            eb.addField(Emojis.ZERO_WIDTH_SPACE, waitTime, false);
            sendError(event, command.getLocale(), eb, false, EmbedFactory.getPatreonBlockButtons(command.getLocale()));
        } else if (BotPermissionUtil.canWrite(event.getChannel())) {
            sendErrorNoEmbed(event, command.getLocale(), desc + "\n\n" + waitTime, false, EmbedFactory.getPatreonBlockButtons(command.getLocale()));
        }

        return false;
    }

    private static boolean checkPatreon(CommandEvent event, Command command) {
        if (!command.getCommandProperties().patreonRequired() ||
                PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), true) ||
                PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong())
        ) {
            return true;
        }

        if (BotPermissionUtil.canWriteEmbed(event.getChannel()) || event.isSlashCommandEvent()) {
            sendError(event, command.getLocale(), EmbedFactory.getPatreonBlockEmbed(command.getLocale()), false, EmbedFactory.getPatreonBlockButtons(command.getLocale()));
        } else if (BotPermissionUtil.canWrite(event.getChannel())) {
            sendErrorNoEmbed(event, command.getLocale(), TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_description_noembed"), false, EmbedFactory.getPatreonBlockButtons(command.getLocale()));
        }

        return false;
    }

    private static boolean checkPermissions(CommandEvent event, Command command) {
        EmbedBuilder errEmbed = BotPermissionUtil.getUserAndBotPermissionMissingEmbed(
                command.getLocale(),
                event.getChannel(),
                event.getMember(),
                command.getAdjustedUserGuildPermissions(),
                command.getAdjustedUserChannelPermissions(),
                command.getAdjustedBotGuildPermissions(),
                command.getAdjustedBotChannelPermissions()
        );
        if (errEmbed == null) {
            return true;
        }

        sendError(event, command.getLocale(), errEmbed, true);
        return false;
    }

    private static boolean checkTurnedOn(CommandEvent event, Command command) {
        if (BotPermissionUtil.can(event.getMember(), Permission.ADMINISTRATOR) ||
                DBCommandManagement.getInstance().retrieve(event.getGuild().getIdLong()).commandIsTurnedOn(command)
        ) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "turnedoff_description");
        if (BotPermissionUtil.canWriteEmbed(event.getChannel()) || event.isSlashCommandEvent()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "turnedoff_title", command.getPrefix()))
                    .setDescription(desc);
            sendError(event, command.getLocale(), eb, true);
        } else if (BotPermissionUtil.canWrite(event.getChannel())) {
            sendErrorNoEmbed(event, command.getLocale(), desc, true);
        }
        return false;
    }

    private static boolean canRunOnGuild(CommandEvent event, Command command) {
        return command.canRunOnGuild(event.getGuild().getIdLong(), event.getMember().getIdLong());
    }

    private static boolean botCanUseEmbeds(CommandEvent event, Command command) {
        if (BotPermissionUtil.canWriteEmbed(event.getChannel()) || !command.getCommandProperties().requiresEmbeds() || event.isSlashCommandEvent()) {
            return true;
        }

        sendErrorNoEmbed(event, command.getLocale(), TextManager.getString(command.getLocale(), TextManager.GENERAL, "no_embed"), true);
        sendHelpDm(event.getMember(), command);
        return false;
    }

    private static boolean isNSFWCompliant(CommandEvent event, Command command) {
        if (!command.getCommandProperties().nsfw() || event.getChannel().isNSFW()) {
            return true;
        }

        EmbedBuilder eb = EmbedFactory.getNSFWBlockEmbed(command.getLocale());
        sendError(event, command.getLocale(), eb, true, EmbedFactory.getNSFWBlockButton(command.getLocale()));
        return false;
    }

    private static void sendErrorNoEmbed(CommandEvent event, Locale locale, String text, boolean autoDelete, Button... buttons) {
        if (BotPermissionUtil.canWrite(event.getChannel()) || event.isSlashCommandEvent()) {
            RestAction<Message> messageAction = event.replyMessage(TextManager.getString(locale, TextManager.GENERAL, "command_block", text), ActionRows.of(buttons));
            if (autoDelete) {
                messageAction.queue(message -> autoRemoveMessageAfterCountdown(event, message));
            } else {
                messageAction.queue();
            }
        }
    }

    private static void sendError(CommandEvent event, Locale locale, EmbedBuilder eb, boolean autoDelete, Button... buttons) {
        if (BotPermissionUtil.canWriteEmbed(event.getChannel()) || event.isSlashCommandEvent()) {
            if (autoDelete) {
                eb.setFooter(TextManager.getString(locale, TextManager.GENERAL, "deleteTime", String.valueOf(SEC_UNTIL_REMOVAL)));
            }

            RestAction<Message> messageAction = event.replyMessageEmbeds(List.of(eb.build()), ActionRows.of(buttons));
            if (autoDelete) {
                messageAction.queue(message -> autoRemoveMessageAfterCountdown(event, message));
            } else {
                messageAction.queue();
            }
        }
    }

    private static void autoRemoveMessageAfterCountdown(CommandEvent event, Message message) {
        MainScheduler.schedule(SEC_UNTIL_REMOVAL, ChronoUnit.SECONDS, "command_manager_error_countdown", () -> {
            if (BotPermissionUtil.can(event.getChannel())) {
                ArrayList<Message> messageList = new ArrayList<>();
                if (message != null) {
                    messageList.add(message);
                }
                if (event.isGuildMessageReceivedEvent() && BotPermissionUtil.can(event.getChannel(), Permission.MESSAGE_MANAGE)) {
                    messageList.add(event.getGuildMessageReceivedEvent().getMessage());
                }
                if (messageList.size() >= 2) {
                    event.getChannel().deleteMessages(messageList).queue();
                } else if (messageList.size() >= 1) {
                    event.getChannel().deleteMessageById(messageList.get(0).getId()).queue();
                }
            }
        });
    }

    private static boolean isWhiteListed(CommandEvent event, Command command) {
        if (BotPermissionUtil.can(event.getMember(), Permission.ADMINISTRATOR) ||
                DBWhiteListedChannels.getInstance().retrieve(event.getGuild().getIdLong()).isWhiteListed(event.getChannel().getIdLong())
        ) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "whitelist_description");

        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "whitelist_title", command.getPrefix()))
                    .setDescription(desc);
            sendError(event, command.getLocale(), eb, true);
        } else if (BotPermissionUtil.canWrite(event.getChannel())) {
            sendErrorNoEmbed(event, command.getLocale(), desc, true);
        }
        return false;
    }

    private static boolean botCanPost(CommandEvent event, Command command) {
        if (BotPermissionUtil.canWrite(event.getChannel()) || event.isSlashCommandEvent()) {
            return true;
        }

        if (event.isGuildMessageReceivedEvent() &&
                BotPermissionUtil.canReadHistory(event.getChannel(), Permission.MESSAGE_ADD_REACTION)
        ) {
            Message message = event.getGuildMessageReceivedEvent().getMessage();
            RestActionQueue restActionQueue = new RestActionQueue();
            restActionQueue.attach(message.addReaction(Emojis.X));
            restActionQueue.attach(message.addReaction("✍️"))
                    .getCurrentRestAction()
                    .queue();
        }

        if (!sendHelpDm(event.getMember(), command)) {
            if (BotPermissionUtil.can(event.getMember(), Permission.ADMINISTRATOR)) {
                JDAUtil.sendPrivateMessage(
                        event.getMember(),
                        TextManager.getString(command.getLocale(), TextManager.GENERAL, "no_writing_permissions", StringUtil.escapeMarkdown(event.getChannel().getName()))
                ).queue();
            }
        }

        return false;
    }

    private static boolean sendHelpDm(Member member, Command command) {
        if (command instanceof HelpCommand) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(command, command.getString("dm", ExternalLinks.COMMANDS_WEBSITE))
                    .setTitle(null);
            Member lawliet = member.getGuild().getSelfMember();
            EmbedUtil.setMemberAuthor(eb, lawliet);
            JDAUtil.sendPrivateMessage(member, eb.build()).queue();
            return true;
        }
        return false;
    }

    private static void sendOverwrittenSignals(Command command, Member member) {
        sendOverwrittenSignals(command, member, OnReactionListener.class);
        sendOverwrittenSignals(command, member, OnMessageInputListener.class);
        sendOverwrittenSignals(command, member, OnButtonListener.class);
        sendOverwrittenSignals(command, member, OnSelectionMenuListener.class);
    }

    private static void sendOverwrittenSignals(Command command, Member member, Class<?> clazz) {
        if (clazz.isInstance(command)) {
            CommandContainer.getListeners(clazz).stream()
                    .filter(meta -> meta.getAuthorId() == member.getIdLong())
                    .forEach(CommandListenerMeta::override);
        }
    }

    private static void cleanPreviousListeners(Command command, Member member) {
        for (Class<?> clazz : CommandContainer.getListenerClasses()) {
            if (clazz.isInstance(command)) {
                ArrayList<CommandListenerMeta<?>> metaList = CommandContainer.getListeners(clazz).stream()
                        .filter(meta -> meta.getAuthorId() == member.getIdLong())
                        .sorted(Comparator.comparing(CommandListenerMeta::getCreationTime))
                        .collect(Collectors.toCollection(ArrayList::new));

                while (metaList.size() >= 2) {
                    CommandListenerMeta<?> meta = metaList.remove(0);
                    CommandContainer.deregisterListeners(meta.getCommand());
                    meta.timeOut();
                }
            }
        }
    }

    public static Optional<Command> createCommandByTrigger(String trigger, Locale locale, String prefix) {
        Class<? extends Command> clazz = CommandContainer.getCommandMap().get(trigger);
        if (clazz == null) return Optional.empty();
        return Optional.of(createCommandByClass(clazz, locale, prefix));
    }

    public static Command createCommandByClassName(String className, Locale locale, String prefix) {
        try {
            return createCommandByClass((Class<? extends Command>) Class.forName(className), locale, prefix);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Command createCommandByClass(Class<? extends Command> clazz, Locale locale, String prefix) {
        for (Constructor<?> s : clazz.getConstructors()) {
            if (s.getParameterCount() == 2) {
                try {
                    return (Command) s.newInstance(locale, prefix);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new RuntimeException("Invalid class");
    }

}
