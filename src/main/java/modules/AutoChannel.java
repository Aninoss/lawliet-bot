package modules;

import commands.Command;
import commands.runnables.configurationcategory.AutoChannelCommand;
import core.PermissionCheckRuntime;
import core.utils.BotPermissionUtil;
import mysql.hibernate.entity.guild.AutoChannelEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoChannel {

    public static String resolveVariables(String string, String arg1, String arg2, String arg3) {
        return string.replaceAll("(?i)" + Pattern.quote("%vcname"), Matcher.quoteReplacement(arg1))
                .replaceAll("(?i)" + Pattern.quote("%index"), Matcher.quoteReplacement(arg2))
                .replaceAll("(?i)" + Pattern.quote("%creator"), Matcher.quoteReplacement(arg3));
    }

    public static void processCreate(VoiceChannel voiceChannel, Member member, GuildEntity guildEntity) {
        cleanGuild(voiceChannel.getGuild(), guildEntity);
        if (!voiceChannel.getMembers().contains(member)) {
            return;
        }

        Guild guild = voiceChannel.getGuild();
        AutoChannelEntity autoChannelEntity = guildEntity.getAutoChannel();
        if (!autoChannelEntity.getActive() || !autoChannelEntity.getParentChannelIds().contains(voiceChannel.getIdLong())) {
            return;
        }

        Locale locale = guildEntity.getLocale();
        if (!PermissionCheckRuntime.botHasPermission(locale, AutoChannelCommand.class, guild, Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL, Permission.VOICE_MOVE_OTHERS) ||
                !PermissionCheckRuntime.botHasPermission(locale, AutoChannelCommand.class, voiceChannel.getParentCategory(), Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL, Permission.VOICE_MOVE_OTHERS) ||
                !PermissionCheckRuntime.botHasPermission(locale, AutoChannelCommand.class, voiceChannel, Permission.VOICE_CONNECT, Permission.VOICE_MOVE_OTHERS)
        ) {
            return;
        }

        int n = 1;
        for (int i = 0; i < 50; i++) {
            if (!guild.getVoiceChannelsByName(getNewVoiceName(autoChannelEntity.getNameMask(), voiceChannel, member, n), true).isEmpty()) {
                n++;
            } else {
                break;
            }
        }
        if (!voiceChannel.getMembers().contains(member)) {
            return;
        }

        ChannelAction<VoiceChannel> channelAction = createNewVoice(autoChannelEntity, voiceChannel, member, n);
        try {
            VoiceChannel newVoiceChannel = channelAction.complete();
            processCreatedVoice(autoChannelEntity, locale, newVoiceChannel, voiceChannel.getIdLong(), member);
        } catch (Throwable e) {
            VoiceChannel newVoiceChannel = channelAction.setName("???").complete();
            processCreatedVoice(autoChannelEntity, locale, newVoiceChannel, voiceChannel.getIdLong(), member);
        }
    }

    public static void processRemove(VoiceChannel voiceChannel, GuildEntity guildEntity) {
        AutoChannelEntity autoChannelEntity = guildEntity.getAutoChannel();
        if (autoChannelEntity.getChildChannelIdsToParentChannelId().containsKey(voiceChannel.getIdLong()) &&
                PermissionCheckRuntime.botHasPermission(guildEntity.getLocale(), AutoChannelCommand.class, voiceChannel, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL) &&
                voiceChannel.getMembers().isEmpty()
        ) {
            voiceChannel.delete()
                    .reason(Command.getCommandLanguage(AutoChannelCommand.class, guildEntity.getLocale()).getTitle())
                    .queue();
        }
    }

    public static void cleanGuild(Guild guild, GuildEntity guildEntity) {
        boolean changes = false;

        AutoChannelEntity autoChannelEntity = guildEntity.getAutoChannel();
        for (long channelId : new ArrayList<>(autoChannelEntity.getChildChannelIdsToParentChannelId().keySet())) {
            VoiceChannel voiceChannel = guild.getVoiceChannelById(channelId);
            if (voiceChannel == null || voiceChannel.getMembers().isEmpty()) {
                if (!changes) {
                    changes = true;
                    autoChannelEntity.beginTransaction();
                }
                autoChannelEntity.getChildChannelIdsToParentChannelId().remove(channelId);
            }
            if (voiceChannel != null && voiceChannel.getMembers().isEmpty()) {
                voiceChannel.delete()
                        .reason(Command.getCommandLanguage(AutoChannelCommand.class, guildEntity.getLocale()).getTitle())
                        .queue();
            }
        }

        if (changes) {
            autoChannelEntity.commitTransaction();
        }
    }

    private static void processCreatedVoice(AutoChannelEntity autoChannelEntity, Locale locale, VoiceChannel voiceChannel,
                                            long parentChannelId, Member member
    ) {
        if (member.getVoiceState() == null || !member.getVoiceState().inAudioChannel()) {
            return;
        }

        try {
            member.getGuild().moveVoiceMember(member, voiceChannel).complete();
        } catch (Throwable e) {
            voiceChannel.delete()
                    .reason(Command.getCommandLanguage(AutoChannelCommand.class, locale).getTitle())
                    .queue();
            return;
        }

        autoChannelEntity.beginTransaction();
        autoChannelEntity.getChildChannelIdsToParentChannelId().put(voiceChannel.getIdLong(), parentChannelId);
        autoChannelEntity.commitTransaction();
    }

    private static ChannelAction<VoiceChannel> createNewVoice(AutoChannelEntity autoChannelEntity, VoiceChannel parentVoice, Member member, int n) {
        ChannelAction<VoiceChannel> channelAction;
        if (parentVoice.getParentCategory() != null) {
            channelAction = parentVoice.getParentCategory().createVoiceChannel(getNewVoiceName(autoChannelEntity.getNameMask(), parentVoice, member, n));
        } else {
            channelAction = parentVoice.getGuild().createVoiceChannel(getNewVoiceName(autoChannelEntity.getNameMask(), parentVoice, member, n));
        }
        channelAction = BotPermissionUtil.clearPermissionOverrides(channelAction)
                .setBitrate(parentVoice.getBitrate())
                .setUserlimit(parentVoice.getUserLimit());

        if (autoChannelEntity.getBeginLocked()) {
            channelAction = channelAction.setUserlimit(1);
        }

        channelAction = BotPermissionUtil.copyPermissions(parentVoice, channelAction);
        channelAction = BotPermissionUtil.addPermission(parentVoice, channelAction, parentVoice.getGuild().getSelfMember(), true,
                Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.VOICE_CONNECT);
        return BotPermissionUtil.addPermission(parentVoice, channelAction, member, true, Permission.MANAGE_CHANNEL, Permission.VOICE_MOVE_OTHERS);
    }

    private static String getNewVoiceName(String nameMask, VoiceChannel parentVoice, Member member, int n) {
        nameMask = AutoChannel.resolveVariables(nameMask, parentVoice.getName(), String.valueOf(n), member.getEffectiveName());
        nameMask = nameMask.substring(0, Math.min(100, nameMask.length()));
        return nameMask;
    }

}
