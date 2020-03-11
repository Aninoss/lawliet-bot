package Commands.Moderation;

import CommandListeners.CommandProperties;
import CommandListeners.onNavigationListener;
import CommandSupporters.Command;
import Constants.*;
import General.*;
import General.Mention.MentionFinder;
import General.Warnings.UserWarnings;
import MySQL.DBServer;
import MySQL.DBUser;
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
        botPermissions = Permission.USE_EXTERNAL_EMOJIS_IN_TEXT_CHANNEL | Permission.KICK_USER | Permission.BAN_USER,
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "\u2699\uFE0F️",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat/128/settings-3-icon.png",
        executable = true,
        aliases = {"modsettings"}
)
public class ModSettingsCommand extends Command implements onNavigationListener  {
    
    private ModerationStatus moderationStatus;
    private static final String EMOJI_AUTOMOD = "\uD83D\uDC77",
            TN_AUTOMOD = "http://icons.iconarchive.com/icons/webalys/kameleon.pics/128/Road-Worker-1-icon.png";

    public ModSettingsCommand() {
        super();
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state, boolean firstTime) throws Throwable {
        if (firstTime) {
            moderationStatus = DBServer.getModerationFromServer(event.getServer().get());
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
                        DBServer.saveModeration(moderationStatus);
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
                    if (value >= 0) {
                        moderationStatus.setAutoKick(value);
                        DBServer.saveModeration(moderationStatus);
                        setLog(LogStatus.SUCCESS, getString("autokickset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL,"too_small2", "0"));
                        return Response.FALSE;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL,"no_digit"));
                    return Response.FALSE;
                }

            case 3:
                if (Tools.stringIsInt(inputString)) {
                    int value = Integer.parseInt(inputString);
                    if (value >= 0) {
                        moderationStatus.setAutoBan(value);
                        DBServer.saveModeration(moderationStatus);
                        setLog(LogStatus.SUCCESS, getString("autobanset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL,"too_small2", "0"));
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
                        DBServer.saveModeration(moderationStatus);
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
                        DBServer.saveModeration(moderationStatus);
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
                        DBServer.saveModeration(moderationStatus);
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
                        DBServer.saveModeration(moderationStatus);
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
                        .addField(getString("state0_mchannel"), Tools.getStringIfNotNull(moderationStatus.getChannel(), notSet), true)
                        .addField(getString("state0_mquestion"), Tools.getOnOffForBoolean(getLocale(), moderationStatus.isQuestion()), true)
                        .addField(getString("state0_mautomod"), getString("state0_mautomod_desc", getAutoModString(moderationStatus.getAutoKick()), getAutoModString(moderationStatus.getAutoBan())), false);

            case 1:
                setOptions(new String[]{getString("state1_options")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description"), getString("state1_title"));

            case 2:
                setOptions(new String[]{getString("state2_options")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state2_description"), getString("state2_title"));

            case 3:
                setOptions(new String[]{getString("state3_options")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state3_description"), getString("state3_title"));
        }
        return null;
    }

    private String getAutoModString(int value) throws IOException {
        if (value <= 0) return Tools.getOnOffForBoolean(getLocale(), false);
        return getString("state0_mautomod_templ", Tools.numToString(value));
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 4;
    }

    public static void insertWarning(Locale locale, Server server, User user, User requestor, String reason) throws SQLException {
        DBServer.insertWarning(server, user, requestor, reason);

        ModerationStatus moderationStatus = DBServer.getModerationFromServer(server);
        UserWarnings userWarnings = DBServer.getWarningsForUser(server, user);

        boolean autoKick = moderationStatus.getAutoKick() > 0 && userWarnings.amountLatestDays(30) == moderationStatus.getAutoKick();
        boolean autoBan = moderationStatus.getAutoBan() > 0 && userWarnings.amountLatestDays(30) == moderationStatus.getAutoBan();

        if (autoBan && PermissionCheckRuntime.getInstance().botHasPermission(locale, "mod", server, Permission.BAN_USER)) {
            try {
                server.banUser(user, 7, TextManager.getString(locale, TextManager.COMMANDS, "mod_autoban")).get();
                moderationStatus.getChannel().ifPresent(serverTextChannel -> {
                    EmbedBuilder eb = EmbedFactory.getEmbed()
                            .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, TextManager.COMMANDS, "mod_autoban"))
                            .setDescription(TextManager.getString(locale, TextManager.COMMANDS, "mod_autoban_template", user.getDisplayName(server)))
                            .setThumbnail(TN_AUTOMOD);

                    try {
                        serverTextChannel.sendMessage(eb).get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                });
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        else if (autoKick && PermissionCheckRuntime.getInstance().botHasPermission(locale, "mod", server, Permission.KICK_USER)) {
            try {
                server.kickUser(user, TextManager.getString(locale, TextManager.COMMANDS, "mod_autokick")).get();
                moderationStatus.getChannel().ifPresent(serverTextChannel -> {
                    EmbedBuilder eb = EmbedFactory.getEmbed()
                            .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, TextManager.COMMANDS, "mod_autokick"))
                            .setDescription(TextManager.getString(locale, TextManager.COMMANDS, "mod_autokick_template", user.getDisplayName(server)))
                            .setThumbnail(TN_AUTOMOD);

                    try {
                        serverTextChannel.sendMessage(eb).get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                });
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
