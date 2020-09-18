package modules;

import commands.runnables.managementcategory.MemberCountDisplayCommand;
import constants.Permission;
import core.PermissionCheckRuntime;
import core.utils.StringUtil;
import mysql.modules.membercountdisplays.DBMemberCountDisplays;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MemberCountDisplay {

    private final static Logger LOGGER = LoggerFactory.getLogger(MemberCountDisplay.class);

    public static void manage(Locale locale, Server server) throws ExecutionException {
        ArrayList<mysql.modules.membercountdisplays.MemberCountDisplay> displays = new ArrayList<>(DBMemberCountDisplays.getInstance().getBean(server.getId()).getMemberCountBeanSlots().values());
        for (mysql.modules.membercountdisplays.MemberCountDisplay display : displays) {
            try {
                synchronized (server) {
                    Optional<ServerVoiceChannel> vcOpt = display.getVoiceChannel();
                    if (vcOpt.isPresent()) {
                        ServerVoiceChannel voiceChannel = vcOpt.get();
                        if (PermissionCheckRuntime.getInstance().botHasPermission(locale, MemberCountDisplayCommand.class, voiceChannel, Permission.MANAGE_CHANNEL | Permission.CONNECT)) {
                            String newVCName = getNewVCName(server, locale, display.getMask());
                            if (!newVCName.equals(voiceChannel.getName())) {
                                voiceChannel.createUpdater()
                                        .setName(newVCName)
                                        .update().get();
                            }
                        }
                    }
                }
            } catch (Throwable throwable) {
                LOGGER.error("Error in mcdisplay", throwable);
            }
        }
    }

    public static String getNewVCName(Server server, Locale locale, String name) {
        long members = server.getMemberCount();
        long botMembers = server.getMembers().stream().filter(User::isBot).count();
        int boosts = server.getBoostCount();

        return replaceVariables(name,
                StringUtil.numToString(locale, members),
                StringUtil.numToString(locale, members - botMembers),
                StringUtil.numToString(locale, botMembers),
                StringUtil.numToString(locale, boosts)
        );
    }

    public static String replaceVariables(String string, String arg1, String arg2, String arg3, String arg4) {
        return string.replaceAll("(?i)" + Pattern.quote("%members"), Matcher.quoteReplacement(arg1))
                .replaceAll("(?i)" + Pattern.quote("%users"), Matcher.quoteReplacement(arg2))
                .replaceAll("(?i)" + Pattern.quote("%bots"), Matcher.quoteReplacement(arg3))
                .replaceAll("(?i)" + Pattern.quote("%boosts"), Matcher.quoteReplacement(arg4));
    }
}
