package modules;

import commands.runnables.utilitycategory.MemberCountDisplayCommand;
import constants.Permission;
import core.DiscordApiManager;
import core.PermissionCheckRuntime;
import core.QuickUpdater;
import core.utils.StringUtil;
import mysql.modules.membercountdisplays.DBMemberCountDisplays;
import mysql.modules.membercountdisplays.MemberCountDisplaySlot;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MemberCountDisplay {

    private final static Logger LOGGER = LoggerFactory.getLogger(MemberCountDisplay.class);

    private static final MemberCountDisplay ourInstance = new MemberCountDisplay();
    public static MemberCountDisplay getInstance() { return ourInstance; }
    private MemberCountDisplay() { }

    public void manage(Locale locale, Server server) {
        ArrayList<MemberCountDisplaySlot> displays = new ArrayList<>(DBMemberCountDisplays.getInstance().getBean(server.getId()).getMemberCountBeanSlots().values());
        for (MemberCountDisplaySlot display : displays) {
            display.getVoiceChannel().ifPresent(voiceChannel -> {
                if (voiceChannel.getId() == 638158050972401664L)
                    LOGGER.info("MCDisplay register"); //TODO

                QuickUpdater.getInstance().update(
                        "member_count_displays",
                        voiceChannel.getId(),
                        () -> {
                            Server s = DiscordApiManager.getInstance().getLocalServerById(server.getId()).get();
                            ServerVoiceChannel vc = s.getVoiceChannelById(voiceChannel.getId()).get();

                            if (PermissionCheckRuntime.getInstance().botHasPermission(locale, MemberCountDisplayCommand.class, vc, Permission.MANAGE_CHANNEL | Permission.CONNECT)) {
                                String newVCName = generateNewVCName(s, display.getMask());
                                if (vc.getId() == 638158050972401664L)
                                    LOGGER.info("MCDisplay: " + newVCName); //TODO

                                if (!newVCName.equals(vc.getName())) {
                                    if (vc.getId() == 638158050972401664L)
                                        LOGGER.info("MCDisplay exec"); //TODO

                                    return vc.createUpdater()
                                            .setName(newVCName)
                                            .update()
                                            .thenRun(() -> {
                                                if (vc.getId() == 638158050972401664L)
                                                    LOGGER.info("MCDisplay end"); //TODO
                                            });
                                } else {
                                    if (vc.getId() == 638158050972401664L)
                                        LOGGER.info("MCDisplay not exec"); //TODO
                                }
                            }
                            return null;
                        }
                );
            });
        }
    }

    public static String generateNewVCName(Server server, String name) {
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
