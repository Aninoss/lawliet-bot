package modules;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.runnables.utilitycategory.MemberCountDisplayCommand;
import core.PermissionCheckRuntime;
import core.RatelimitUpdater;
import core.utils.StringUtil;
import mysql.modules.membercountdisplays.DBMemberCountDisplays;
import mysql.modules.membercountdisplays.MemberCountDisplaySlot;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.requests.RestAction;

public class MemberCountDisplay {

    private static final MemberCountDisplay ourInstance = new MemberCountDisplay();

    public static MemberCountDisplay getInstance() {
        return ourInstance;
    }

    private final RatelimitUpdater ratelimitUpdater = new RatelimitUpdater(5, ChronoUnit.MINUTES);
    private final Cache<Long, String> voiceNameCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(20))
            .build();

    private MemberCountDisplay() {
    }

    public void manage(Locale locale, Guild guild) {
        ArrayList<MemberCountDisplaySlot> displays = new ArrayList<>(DBMemberCountDisplays.getInstance().retrieve(guild.getIdLong()).getMemberCountBeanSlots().values());
        for (MemberCountDisplaySlot display : displays) {
            display.getVoiceChannel().ifPresent(voiceChannel -> {
                if (PermissionCheckRuntime.getInstance().botHasPermission(locale, MemberCountDisplayCommand.class, voiceChannel, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL)) {
                    String newVoiceName = generateNewVCName(guild, display.getMask());

                    if (!getCurrentVoiceName(voiceChannel).equals(newVoiceName)) {
                        rename(voiceChannel, newVoiceName);
                        voiceNameCache.put(voiceChannel.getIdLong(), newVoiceName);
                    }
                }
            });
        }
    }

    private String getCurrentVoiceName(VoiceChannel voiceChannel) {
        if (voiceNameCache.asMap().containsKey(voiceChannel.getIdLong())) {
            return voiceNameCache.getIfPresent(voiceChannel.getIdLong());
        }
        return voiceChannel.getName();
    }

    private void rename(VoiceChannel voiceChannel, String newVCName) {
        RestAction<Void> restAction = voiceChannel.getManager()
                .setName(newVCName);

        ratelimitUpdater.update(
                voiceChannel.getIdLong(),
                restAction
        );
    }

    public static String generateNewVCName(Guild guild, String name) {
        long members = guild.getMemberCount();
        long botMembers = guild.getMembers().stream().filter(m -> m.getUser().isBot()).count();
        int boosts = guild.getBoostCount();

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
