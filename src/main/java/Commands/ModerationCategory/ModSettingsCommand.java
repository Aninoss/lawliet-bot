package Commands.ModerationCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onNavigationListener;
import CommandSupporters.Command;
import CommandSupporters.CommandManager;
import Constants.*;
import General.*;
import General.Mention.MentionFinder;
import General.Warnings.UserWarnings;
import MySQL.DBServerOld;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "mod",
        botPermissions = Permission.KICK_USER | Permission.BAN_USER,
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "\u2699\uFE0F️",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat/128/settings-3-icon.png",
        executable = true,
        aliases = {"modsettings"}
)
public class ModSettingsCommand extends Command implements onNavigationListener  {
    
    private ModerationStatus moderationStatus;
    private int autoKickTemp, autoBanTemp;
    private static final String EMOJI_AUTOMOD = "\uD83D\uDC77",
            TN_AUTOMOD = "http://icons.iconarchive.com/icons/webalys/kameleon.pics/128/Road-Worker-1-icon.png";

    public ModSettingsCommand() {
        super();
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state, boolean firstTime) throws Throwable {
        if (firstTime) {
            moderationStatus = DBServerOld.getModerationFromServer(event.getServer().get());
            return Response.TRUE;
        }

        switch (state) {
            case 1:
                ArrayList<ServerTextChannel> channelsList = MentionFinder.getTextChannels(event.getMessage(), inputString).getList();
                if (channelsList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    ServerTextChannel channel = channelsList.get(0);
                    if (checkWriteInChannelWithLog(channel)) {
                        moderationStatus.setChannel(channel);
                        DBServerOld.saveModeration(moderationStatus);
                        setLog(LogStatus.SUCCESS, getString("channelset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        return Response.FALSE;
                    }
                }

            case 2:
                if (Tools.stringIsInt(inputString)) {
                    int value = Integer.parseInt(inputString);
                    if (value >= 1) {
                        autoKickTemp = value;
                        setState(4);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL,"too_small2", "1"));
                        return Response.FALSE;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL,"no_digit"));
                    return Response.FALSE;
                }

            case 3:
                if (Tools.stringIsInt(inputString)) {
                    int value = Integer.parseInt(inputString);
                    if (value >= 1) {
                        autoBanTemp = value;
                        setState(5);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL,"too_small2", "1"));
                        return Response.FALSE;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL,"no_digit"));
                    return Response.FALSE;
                }

            case 4:
                if (Tools.stringIsInt(inputString)) {
                    int value = Integer.parseInt(inputString);
                    if (value >= 1) {
                        moderationStatus.setAutoKick(autoKickTemp);
                        moderationStatus.setAutoKickDays(value);
                        DBServerOld.saveModeration(moderationStatus);
                        setLog(LogStatus.SUCCESS, getString("autokickset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL,"too_small2", "1"));
                        return Response.FALSE;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL,"no_digit"));
                    return Response.FALSE;
                }

            case 5:
                if (Tools.stringIsInt(inputString)) {
                    int value = Integer.parseInt(inputString);
                    if (value >= 1) {
                        moderationStatus.setAutoBan(autoBanTemp);
                        moderationStatus.setAutoBanDays(value);
                        DBServerOld.saveModeration(moderationStatus);
                        setLog(LogStatus.SUCCESS, getString("autobanset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL,"too_small2", "1"));
                        return Response.FALSE;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL,"no_digit"));
                    return Response.FALSE;
                }
        }

        return null;
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        deleteNavigationMessage();
                        return false;

                    case 0:
                        setState(1);
                        return true;

                    case 1:
                        moderationStatus.switchQuestion();
                        DBServerOld.saveModeration(moderationStatus);
                        setLog(LogStatus.SUCCESS, getString("setquestion", moderationStatus.isQuestion()));
                        return true;

                    case 2:
                        setState(2);
                        return true;

                    case 3:
                        setState(3);
                        return true;
                }
                return false;

            case 1:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        moderationStatus.setChannel(null);
                        DBServerOld.saveModeration(moderationStatus);
                        setLog(LogStatus.SUCCESS, getString("channelreset"));
                        setState(0);
                        return true;
                }
                return false;

            case 2:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        moderationStatus.setAutoKick(0);
                        DBServerOld.saveModeration(moderationStatus);
                        setLog(LogStatus.SUCCESS, getString("autokickset"));
                        setState(0);
                        return true;
                }
                return false;

            case 3:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        moderationStatus.setAutoBan(0);
                        DBServerOld.saveModeration(moderationStatus);
                        setLog(LogStatus.SUCCESS, getString("autobanset"));
                        setState(0);
                        return true;
                }
                return false;

            case 4:
                switch (i) {
                    case -1:
                        setState(2);
                        return true;

                    case 0:
                        moderationStatus.setAutoKick(autoKickTemp);
                        moderationStatus.setAutoKickDays(0);
                        DBServerOld.saveModeration(moderationStatus);
                        setLog(LogStatus.SUCCESS, getString("autokickset"));
                        setState(0);
                        return true;
                }
                return false;

            case 5:
                switch (i) {
                    case -1:
                        setState(3);
                        return true;

                    case 0:
                        moderationStatus.setAutoBan(autoBanTemp);
                        moderationStatus.setAutoBanDays(0);
                        DBServerOld.saveModeration(moderationStatus);
                        setLog(LogStatus.SUCCESS, getString("autobanset"));
                        setState(0);
                        return true;
                }
                return false;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        switch (state) {
            case 0:
                String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description"))
                        .addField(getString("state0_mchannel"), Tools.getStringIfNotNull(moderationStatus.getChannel().orElse(null), notSet), true)
                        .addField(getString("state0_mquestion"), Tools.getOnOffForBoolean(getLocale(), moderationStatus.isQuestion()), true)
                        .addField(getString("state0_mautomod"), getString("state0_mautomod_desc", getAutoModString(moderationStatus.getAutoKick(), moderationStatus.getAutoKickDays()), getAutoModString(moderationStatus.getAutoBan(), moderationStatus.getAutoBanDays())), false);

            case 1:
                setOptions(new String[]{getString("state1_options")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description"), getString("state1_title"));

            case 2:
                setOptions(new String[]{getString("state2_options")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state2_description"), getString("state2_title"));

            case 3:
                setOptions(new String[]{getString("state3_options")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state3_description"), getString("state3_title"));

            case 4:
                setOptions(new String[]{getString("state4_options")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state4_description", autoKickTemp != 1, Tools.numToString(autoKickTemp)), getString("state4_title"));

            case 5:
                setOptions(new String[]{getString("state4_options")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state4_description", autoBanTemp != 1, Tools.numToString(autoBanTemp)), getString("state5_title"));
        }
        return null;
    }

    private String getAutoModString(int value, int days) throws IOException {
        if (value <= 0) return Tools.getOnOffForBoolean(getLocale(), false);
        return getString("state0_mautomod_templ", value > 1, Tools.numToString(value), days > 0 ? getString("days", days > 1, Tools.numToString(days)) : getString("total"));
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 4;
    }

    public static void insertWarning(Locale locale, Server server, User user, User requestor, String reason) throws SQLException {
        DBServerOld.insertWarning(server, user, requestor, reason);

        ModerationStatus moderationStatus = DBServerOld.getModerationFromServer(server);
        UserWarnings userWarnings = DBServerOld.getWarningsForUser(server, user);

        int autoKickDays = moderationStatus.getAutoKickDays();
        int autoBanDays = moderationStatus.getAutoBanDays();

        boolean autoKick = moderationStatus.getAutoKick() > 0 && (autoKickDays > 0 ? userWarnings.amountLatestDays(autoKickDays) : userWarnings.amountTotal()) >= moderationStatus.getAutoKick();
        boolean autoBan = moderationStatus.getAutoBan() > 0 && (autoBanDays > 0 ? userWarnings.amountLatestDays(autoBanDays) : userWarnings.amountTotal()) >= moderationStatus.getAutoBan();

        if (autoBan && PermissionCheckRuntime.getInstance().botHasPermission(locale, "mod", server, Permission.BAN_USER)) {
            try {
                server.banUser(user, 7, TextManager.getString(locale, TextManager.COMMANDS, "mod_autoban")).get();

                EmbedBuilder eb = EmbedFactory.getEmbed()
                        .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, TextManager.COMMANDS, "mod_autoban"))
                        .setDescription(TextManager.getString(locale, TextManager.COMMANDS, "mod_autoban_template", user.getDisplayName(server)))
                        .setThumbnail(TN_AUTOMOD);

                postLog(CommandManager.createCommandByClass(ModSettingsCommand.class, locale), eb, moderationStatus);
            } catch (IllegalAccessException | InstantiationException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        else if (autoKick && PermissionCheckRuntime.getInstance().botHasPermission(locale, "mod", server, Permission.KICK_USER)) {
            try {
                server.kickUser(user, TextManager.getString(locale, TextManager.COMMANDS, "mod_autokick")).get();

                EmbedBuilder eb = EmbedFactory.getEmbed()
                        .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, TextManager.COMMANDS, "mod_autokick"))
                        .setDescription(TextManager.getString(locale, TextManager.COMMANDS, "mod_autokick_template", user.getDisplayName(server)))
                        .setThumbnail(TN_AUTOMOD);

                postLog(CommandManager.createCommandByClass(ModSettingsCommand.class, locale), eb, moderationStatus);
            } catch (InterruptedException | ExecutionException | IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    public static void postLog(Command command, EmbedBuilder eb, Server server) throws SQLException {
        postLog(command, eb, DBServerOld.getModerationFromServer(server));
    }

    public static void postLog(Command command, EmbedBuilder eb, ModerationStatus moderationStatus) {
        moderationStatus.getChannel().ifPresent(serverTextChannel -> {

            if (PermissionCheckRuntime.getInstance().botHasPermission(command.getLocale(), command.getTrigger(), serverTextChannel, Permission.WRITE_IN_TEXT_CHANNEL | Permission.EMBED_LINKS_IN_TEXT_CHANNELS)) {
                try {
                    serverTextChannel.sendMessage(eb).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

        });
    }

}
