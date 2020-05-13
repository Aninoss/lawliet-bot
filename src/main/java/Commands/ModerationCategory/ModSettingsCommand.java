package Commands.ModerationCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnNavigationListener;
import CommandSupporters.Command;
import CommandSupporters.CommandManager;
import Constants.*;
import Core.*;
import Core.Mention.MentionUtil;
import Core.Utils.StringUtil;
import MySQL.Modules.Moderation.DBModeration;
import MySQL.Modules.Moderation.ModerationBean;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Warning.DBServerWarnings;
import MySQL.Modules.Warning.ServerWarningsBean;
import MySQL.Modules.Warning.ServerWarningsSlot;
import javafx.util.Pair;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "mod",
        botPermissions = Permission.KICK_MEMBERS | Permission.BAN_MEMBERS,
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "\u2699\uFE0F️",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat/128/settings-3-icon.png",
        executable = true,
        aliases = {"modsettings"}
)
public class ModSettingsCommand extends Command implements OnNavigationListener {

    final static Logger LOGGER = LoggerFactory.getLogger(ModSettingsCommand.class);
    private ModerationBean moderationBean;
    private int autoKickTemp, autoBanTemp;
    private static final String EMOJI_AUTOMOD = "\uD83D\uDC77",
            TN_AUTOMOD = "http://icons.iconarchive.com/icons/webalys/kameleon.pics/128/Road-Worker-1-icon.png";

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        moderationBean = DBModeration.getInstance().getBean(event.getServer().get().getId());
        return true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable {
        switch (state) {
            case 1:
                ArrayList<ServerTextChannel> channelsList = MentionUtil.getTextChannels(event.getMessage(), inputString).getList();
                if (channelsList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    ServerTextChannel channel = channelsList.get(0);
                    if (checkWriteInChannelWithLog(channel)) {
                        moderationBean.setAnnouncementChannelId(channel.getId());
                        setLog(LogStatus.SUCCESS, getString("channelset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        return Response.FALSE;
                    }
                }

            case 2:
                if (StringUtil.stringIsInt(inputString)) {
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
                if (StringUtil.stringIsInt(inputString)) {
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
                if (StringUtil.stringIsInt(inputString)) {
                    int value = Integer.parseInt(inputString);
                    if (value >= 1) {
                        moderationBean.setAutoKick(autoKickTemp, value);
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
                if (StringUtil.stringIsInt(inputString)) {
                    int value = Integer.parseInt(inputString);
                    if (value >= 1) {
                        moderationBean.setAutoBan(autoBanTemp, value);
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
                        removeNavigationWithMessage();
                        return false;

                    case 0:
                        setState(1);
                        return true;

                    case 1:
                        moderationBean.toggleQuestion();
                        setLog(LogStatus.SUCCESS, getString("setquestion", moderationBean.isQuestion()));
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
                        moderationBean.setAnnouncementChannelId(null);
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
                        moderationBean.setAutoKick(0, 0);
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
                        moderationBean.setAutoBan(0, 0);
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
                        moderationBean.setAutoKick(autoKickTemp, 0);
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
                        moderationBean.setAutoKick(autoBanTemp, 0);
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
                        .addField(getString("state0_mchannel"), moderationBean.getAnnouncementChannel().map(Mentionable::getMentionTag).orElse(notSet), true)
                        .addField(getString("state0_mquestion"), StringUtil.getOnOffForBoolean(getLocale(), moderationBean.isQuestion()), true)
                        .addField(getString("state0_mautomod"), getString("state0_mautomod_desc", getAutoModString(moderationBean.getAutoKick(), moderationBean.getAutoKickDays()), getAutoModString(moderationBean.getAutoBan(), moderationBean.getAutoBanDays())), false);

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
                return EmbedFactory.getCommandEmbedStandard(this, getString("state4_description", autoKickTemp != 1, StringUtil.numToString(autoKickTemp)), getString("state4_title"));

            case 5:
                setOptions(new String[]{getString("state4_options")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state4_description", autoBanTemp != 1, StringUtil.numToString(autoBanTemp)), getString("state5_title"));
        }
        return null;
    }

    private String getAutoModString(int value, int days) throws IOException {
        if (value <= 0) return StringUtil.getOnOffForBoolean(getLocale(), false);
        return getString("state0_mautomod_templ", value > 1, StringUtil.numToString(value), days > 0 ? getString("days", days > 1, StringUtil.numToString(days)) : getString("total"));
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 4;
    }

    public static void insertWarning(Locale locale, Server server, User user, User requestor, String reason) throws ExecutionException, InterruptedException {
        ServerWarningsBean serverWarningsBean = DBServerWarnings.getInstance().getBean(new Pair<>(server.getId(), user.getId()));
        serverWarningsBean.getWarnings().add(new ServerWarningsSlot(
                DBServer.getInstance().getBean(server.getId()),
                user.getId(),
                Instant.now(),
                requestor.getId(),
                reason == null || reason.isEmpty() ? null : reason)
        );

        ModerationBean moderationBean = DBModeration.getInstance().getBean(server.getId());

        int autoKickDays = moderationBean.getAutoKickDays();
        int autoBanDays = moderationBean.getAutoBanDays();

        boolean autoKick = moderationBean.getAutoKick() > 0 && (autoKickDays > 0 ? serverWarningsBean.getAmountLatest(autoKickDays, ChronoUnit.DAYS).size() : serverWarningsBean.getWarnings().size()) >= moderationBean.getAutoKick();
        boolean autoBan = moderationBean.getAutoBan() > 0 && (autoBanDays > 0 ? serverWarningsBean.getAmountLatest(autoBanDays, ChronoUnit.DAYS).size() : serverWarningsBean.getWarnings().size()) >= moderationBean.getAutoBan();

        if (autoBan && PermissionCheckRuntime.getInstance().botHasPermission(locale, ModSettingsCommand.class, server, Permission.BAN_MEMBERS) && server.canYouBanUser(user)) {
            try {
                server.banUser(user, 7, TextManager.getString(locale, TextManager.COMMANDS, "mod_autoban")).get();

                EmbedBuilder eb = EmbedFactory.getEmbed()
                        .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, TextManager.COMMANDS, "mod_autoban"))
                        .setDescription(TextManager.getString(locale, TextManager.COMMANDS, "mod_autoban_template", user.getDisplayName(server)))
                        .setThumbnail(TN_AUTOMOD);

                postLog(CommandManager.createCommandByClass(ModSettingsCommand.class, locale), eb, moderationBean);
            } catch (IllegalAccessException | InstantiationException | ExecutionException e) {
                LOGGER.error("Could not ban user", e);
            }
        }

        else if (autoKick && PermissionCheckRuntime.getInstance().botHasPermission(locale, ModSettingsCommand.class, server, Permission.KICK_MEMBERS) && server.canYouKickUser(user)) {
            try {
                server.kickUser(user, TextManager.getString(locale, TextManager.COMMANDS, "mod_autokick")).get();

                EmbedBuilder eb = EmbedFactory.getEmbed()
                        .setTitle(EMOJI_AUTOMOD + " " + TextManager.getString(locale, TextManager.COMMANDS, "mod_autokick"))
                        .setDescription(TextManager.getString(locale, TextManager.COMMANDS, "mod_autokick_template", user.getDisplayName(server)))
                        .setThumbnail(TN_AUTOMOD);

                postLog(CommandManager.createCommandByClass(ModSettingsCommand.class, locale), eb, moderationBean);
            } catch (ExecutionException | IllegalAccessException | InstantiationException e) {
                LOGGER.error("Could not kick user", e);
            }
        }
    }

    public static void postLog(Command command, EmbedBuilder eb, Server server) throws ExecutionException {
        postLog(command, eb, DBModeration.getInstance().getBean(server.getId()));
    }

    public static void postLog(Command command, EmbedBuilder eb, ModerationBean moderationBean) {
        moderationBean.getAnnouncementChannel().ifPresent(serverTextChannel -> {
            if (PermissionCheckRuntime.getInstance().botHasPermission(command.getLocale(), command.getClass(), serverTextChannel, Permission.SEND_MESSAGES | Permission.EMBED_LINKS)) {
                try {
                    serverTextChannel.sendMessage(eb).get();
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.error("Could not post warning", e);
                }
            }
        });
    }

}
