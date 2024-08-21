package modules;

import commands.Command;
import commands.runnables.configurationcategory.AutoChannelCommand;
import core.PermissionCheckRuntime;
import core.utils.BotPermissionUtil;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.modules.autochannel.AutoChannelData;
import mysql.modules.autochannel.DBAutoChannel;
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
        if (!voiceChannel.getMembers().contains(member)) {
            return;
        }

        Guild guild = voiceChannel.getGuild();
        AutoChannelData autoChannelBean = DBAutoChannel.getInstance().retrieve(guild.getIdLong());

        if (autoChannelBean.isActive() && voiceChannel.getIdLong() == autoChannelBean.getParentChannelId().orElse(0L)) {
            Locale locale = guildEntity.getLocale();
            if (PermissionCheckRuntime.botHasPermission(locale, AutoChannelCommand.class, guild, Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL, Permission.VOICE_MOVE_OTHERS) &&
                    PermissionCheckRuntime.botHasPermission(locale, AutoChannelCommand.class, voiceChannel.getParentCategory(), Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL, Permission.VOICE_MOVE_OTHERS) &&
                    PermissionCheckRuntime.botHasPermission(locale, AutoChannelCommand.class, voiceChannel, Permission.VOICE_CONNECT, Permission.VOICE_MOVE_OTHERS)
            ) {
                int n = 1;
                for (int i = 0; i < 50; i++) {
                    if (!guild.getVoiceChannelsByName(getNewVoiceName(autoChannelBean, voiceChannel, member, n), true).isEmpty()) {
                        n++;
                    } else {
                        break;
                    }
                }

                if (!voiceChannel.getMembers().contains(member)) {
                    return;
                }

                ChannelAction<VoiceChannel> channelAction = createNewVoice(autoChannelBean, voiceChannel, member, n);
                channelAction.queue(
                        vc -> processCreatedVoice(autoChannelBean, locale, vc, member),
                        e -> channelAction.setName("???")
                                .queue(vc -> processCreatedVoice(autoChannelBean, locale, vc, member))
                );
            }
        }
    }

    public static void processRemove(VoiceChannel voiceChannel, GuildEntity guildEntity) {
        AutoChannelData autoChannelBean = DBAutoChannel.getInstance().retrieve(voiceChannel.getGuild().getIdLong());

        for (long childChannelId : new ArrayList<>(autoChannelBean.getChildChannelIds())) {
            if (voiceChannel.getIdLong() == childChannelId) {
                if (PermissionCheckRuntime.botHasPermission(guildEntity.getLocale(), AutoChannelCommand.class, voiceChannel, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL)) {
                    if (voiceChannel.getMembers().isEmpty()) {
                        voiceChannel.delete().queue();
                    }
                }
                break;
            }
        }
    }

    private static void processCreatedVoice(AutoChannelData autoChannelBean, Locale locale, VoiceChannel voiceChannel,
                                            Member member
    ) {
        if (member.getVoiceState() != null && member.getVoiceState().inAudioChannel()) {
            member.getGuild().moveVoiceMember(member, voiceChannel).queue(v -> {
                autoChannelBean.getChildChannelIds().add(voiceChannel.getIdLong());
                if (!voiceChannel.getMembers().contains(member)) {
                    voiceChannel.delete()
                            .reason(Command.getCommandLanguage(AutoChannelCommand.class, locale).getTitle())
                            .queue();
                    autoChannelBean.getChildChannelIds().remove(voiceChannel.getIdLong());
                }
            }, e -> {
                voiceChannel.delete()
                        .reason(Command.getCommandLanguage(AutoChannelCommand.class, locale).getTitle())
                        .queue();
            });
        }
    }

    private static ChannelAction<VoiceChannel> createNewVoice(AutoChannelData autoChannelBean, VoiceChannel parentVoice, Member member, int n) {
        ChannelAction<VoiceChannel> channelAction;
        if (parentVoice.getParentCategory() != null) {
            channelAction = parentVoice.getParentCategory().createVoiceChannel(getNewVoiceName(autoChannelBean, parentVoice, member, n));
        } else {
            channelAction = parentVoice.getGuild().createVoiceChannel(getNewVoiceName(autoChannelBean, parentVoice, member, n));
        }
        channelAction = BotPermissionUtil.clearPermissionOverrides(channelAction)
                .setBitrate(parentVoice.getBitrate())
                .setUserlimit(parentVoice.getUserLimit());

        if (autoChannelBean.isLocked()) {
            channelAction = channelAction.setUserlimit(1);
        }

        channelAction = BotPermissionUtil.copyPermissions(parentVoice, channelAction);
        channelAction = BotPermissionUtil.addPermission(parentVoice, channelAction, parentVoice.getGuild().getSelfMember(), true,
                Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.VOICE_CONNECT);
        return BotPermissionUtil.addPermission(parentVoice, channelAction, member, true, Permission.MANAGE_CHANNEL, Permission.VOICE_MOVE_OTHERS);
    }

    private static String getNewVoiceName(AutoChannelData autoChannelBean, VoiceChannel parentVoice, Member member, int n) {
        String name = autoChannelBean.getNameMask();
        name = AutoChannel.resolveVariables(name, parentVoice.getName(), String.valueOf(n), member.getEffectiveName());
        name = name.substring(0, Math.min(100, name.length()));
        return name;
    }

}
