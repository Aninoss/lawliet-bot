package modules;

import commands.runnables.utilitycategory.MemberCountDisplayCommand;
import constants.Permission;
import core.CustomThread;
import core.PermissionCheckRuntime;
import core.utils.StringUtil;
import mysql.modules.membercountdisplays.DBMemberCountDisplays;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MemberCountDisplay {

    private final static Logger LOGGER = LoggerFactory.getLogger(MemberCountDisplay.class);

    public static void manage(Locale locale, Server server) throws ExecutionException {
        ArrayList<mysql.modules.membercountdisplays.MemberCountDisplay> displays = new ArrayList<>(DBMemberCountDisplays.getInstance().getBean(server.getId()).getMemberCountBeanSlots().values());
        for (mysql.modules.membercountdisplays.MemberCountDisplay display : displays) {
            new CustomThread(() -> {
                synchronized (server) {
                    display.getVoiceChannel().ifPresent(voiceChannel -> {
                        try {
                            if (PermissionCheckRuntime.getInstance().botHasPermission(locale, MemberCountDisplayCommand.class, voiceChannel, Permission.MANAGE_CHANNEL | Permission.CONNECT)) {
                                String newVCName = getNewVCName(server, locale, display.getMask());
                                if (!newVCName.equals(voiceChannel.getName())) {
                                    voiceChannel.createUpdater()
                                            .setName(newVCName)
                                            .update()
                                            .get(10, TimeUnit.SECONDS);
                                }
                            }
                        } catch (Throwable e) {
                            LOGGER.error("Error in mc display", e);
                        }
                    });
                }
            }, "mc_display", 1).start();
        }
    }

    public static String getNewVCName(Server server, Locale locale, String name) {
        long members = server.getMemberCount();
        long botMembers = server.getMembers().stream().filter(User::isBot).count();
        int boosts = server.getBoostCount();

        return replaceVariables(
                name,
                StringUtil.numToString(members),
                StringUtil.numToString(members - botMembers),
                StringUtil.numToString(botMembers),
                StringUtil.numToString(boosts)
        );
    }

    public static String replaceVariables(String string, String arg1, String arg2, String arg3, String arg4) {
        return string.replaceAll("(?i)" + Pattern.quote("%members"), Matcher.quoteReplacement(arg1))
                .replaceAll("(?i)" + Pattern.quote("%users"), Matcher.quoteReplacement(arg2))
                .replaceAll("(?i)" + Pattern.quote("%bots"), Matcher.quoteReplacement(arg3))
                .replaceAll("(?i)" + Pattern.quote("%boosts"), Matcher.quoteReplacement(arg4));
    }

}
