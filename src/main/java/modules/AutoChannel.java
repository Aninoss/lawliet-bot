package modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import commands.Command;
import commands.runnables.utilitycategory.AutoChannelCommand;
import core.PermissionCheckRuntime;
import core.utils.BotPermissionUtil;
import mysql.modules.autochannel.AutoChannelBean;
import mysql.modules.autochannel.DBAutoChannel;
import mysql.modules.guild.GuildBean;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

public class AutoChannel {

    public static String resolveVariables(String string, String arg1, String arg2, String arg3) {
        return string.replaceAll("(?i)" + Pattern.quote("%vcname"), Matcher.quoteReplacement(arg1))
                .replaceAll("(?i)" + Pattern.quote("%index"), Matcher.quoteReplacement(arg2))
                .replaceAll("(?i)" + Pattern.quote("%creator"), Matcher.quoteReplacement(arg3));
    }

    public static void processCreate(VoiceChannel voiceChannel, Member member) {
        if (!voiceChannel.getMembers().contains(member)) {
            return;
        }

        Guild guild = voiceChannel.getGuild();
        AutoChannelBean autoChannelBean = DBAutoChannel.getInstance().retrieve(guild.getIdLong());
        if (autoChannelBean.isActive() && voiceChannel.getIdLong() == autoChannelBean.getParentChannelId().orElse(0L)) {
            GuildBean guildBean = autoChannelBean.getGuildBean();
            if (PermissionCheckRuntime.getInstance().botHasPermission(guildBean.getLocale(), AutoChannelCommand.class, guild, Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.VOICE_CONNECT) &&
                    (voiceChannel.getParent() == null || PermissionCheckRuntime.getInstance().botHasPermission(guildBean.getLocale(), AutoChannelCommand.class, voiceChannel.getParent(), Permission.MANAGE_CHANNEL, Permission.VOICE_CONNECT)) &&
                    PermissionCheckRuntime.getInstance().botHasPermission(guildBean.getLocale(), AutoChannelCommand.class, voiceChannel, Permission.VOICE_CONNECT, Permission.VOICE_MOVE_OTHERS)
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
                        vc -> processCreatedVoice(autoChannelBean, vc, member),
                        e -> channelAction.setName("???")
                                .queue(vc -> processCreatedVoice(autoChannelBean, vc, member))
                );
            }
        }
    }

    public static void processRemove(VoiceChannel voiceChannel) {
        AutoChannelBean autoChannelBean = DBAutoChannel.getInstance().retrieve(voiceChannel.getGuild().getIdLong());

        for (long childChannelId : new ArrayList<>(autoChannelBean.getChildChannelIds())) {
            if (voiceChannel.getIdLong() == childChannelId) {
                if (PermissionCheckRuntime.getInstance().botHasPermission(autoChannelBean.getGuildBean().getLocale(), AutoChannelCommand.class, voiceChannel, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL)) {
                    if (voiceChannel.getMembers().size() == 0) {
                        voiceChannel.delete().queue();
                    }
                }
                break;
            }
        }
    }

    private static void processCreatedVoice(AutoChannelBean autoChannelBean, VoiceChannel voiceChannel, Member member) {
        if (member.getVoiceState() != null && member.getVoiceState().inVoiceChannel()) {
            member.getGuild().moveVoiceMember(member, voiceChannel).queue(v -> {
                autoChannelBean.getChildChannelIds().add(voiceChannel.getIdLong());
                if (!voiceChannel.getMembers().contains(member)) {
                    voiceChannel.delete()
                            .reason(Command.getCommandLanguage(AutoChannelCommand.class, autoChannelBean.getGuildBean().getLocale()).getTitle())
                            .queue();
                    autoChannelBean.getChildChannelIds().remove(voiceChannel.getIdLong());
                }
            }, e -> {
                voiceChannel.delete()
                        .reason(Command.getCommandLanguage(AutoChannelCommand.class, autoChannelBean.getGuildBean().getLocale()).getTitle())
                        .queue();
            });
        }
    }

    private static ChannelAction<VoiceChannel> createNewVoice(AutoChannelBean autoChannelBean, VoiceChannel parentVoice, Member member, int n) {
        ChannelAction<VoiceChannel> channelAction;
        if (parentVoice.getParent() != null) {
            channelAction = parentVoice.getParent().createVoiceChannel(getNewVoiceName(autoChannelBean, parentVoice, member, n));
        } else {
            channelAction = parentVoice.getGuild().createVoiceChannel(getNewVoiceName(autoChannelBean, parentVoice, member, n));
        }
        channelAction = channelAction.clearPermissionOverrides()
                .setBitrate(parentVoice.getBitrate())
                .setUserlimit(parentVoice.getUserLimit());

        if (autoChannelBean.isLocked()) {
            channelAction = channelAction.setUserlimit(1);
        }

        channelAction = addOriginalPermissions(parentVoice, channelAction);
        channelAction = addBotPermissions(parentVoice, channelAction);
        return addCreatorPermissions(parentVoice, channelAction, member);
    }

    private static ChannelAction<VoiceChannel> addOriginalPermissions(VoiceChannel parentVoice, ChannelAction<VoiceChannel> channelAction) {
        for (PermissionOverride permissionOverride : parentVoice.getPermissionOverrides()) {
            if (permissionOverride.getPermissionHolder() != null) {
                List<Permission> newAllowed = permissionOverride.getAllowed().stream()
                        .filter(permission -> BotPermissionUtil.canInteract(parentVoice.getGuild(), permission))
                        .collect(Collectors.toList());

                List<Permission> newDenied = permissionOverride.getDenied().stream()
                        .filter(permission -> BotPermissionUtil.canInteract(parentVoice.getGuild(), permission))
                        .collect(Collectors.toList());

                channelAction = channelAction.addPermissionOverride(
                        permissionOverride.getPermissionHolder(),
                        newAllowed,
                        newDenied
                );
            }
        }
        return channelAction;
    }

    private static ChannelAction<VoiceChannel> addBotPermissions(VoiceChannel parentVoice, ChannelAction<VoiceChannel> channelAction) {
        long allowRaw = 0L;
        long denyRaw = 0L;

        PermissionOverride botPermission = parentVoice.getPermissionOverride(parentVoice.getGuild().getSelfMember());
        if (botPermission != null) {
            Collection<Permission> allow = botPermission.getAllowed().stream()
                    .filter(permission -> BotPermissionUtil.canInteract(parentVoice.getGuild(), permission))
                    .collect(Collectors.toList());

            Collection<Permission> deny = botPermission.getDenied().stream()
                    .filter(permission -> BotPermissionUtil.canInteract(parentVoice.getGuild(), permission))
                    .collect(Collectors.toList());

            allowRaw = Permission.getRaw(allow);
            denyRaw = Permission.getRaw(deny);
        }

        return channelAction.addPermissionOverride(
                parentVoice.getGuild().getSelfMember(),
                allowRaw | Permission.VIEW_CHANNEL.getRawValue() | Permission.MANAGE_CHANNEL.getRawValue() | Permission.VOICE_CONNECT.getRawValue(),
                denyRaw & Permission.VIEW_CHANNEL.getRawValue() & ~Permission.MANAGE_CHANNEL.getRawValue() & ~Permission.VOICE_CONNECT.getRawValue()
        );
    }

    private static ChannelAction<VoiceChannel> addCreatorPermissions(VoiceChannel parentVoice, ChannelAction<VoiceChannel> channelAction, Member member) {
        PermissionOverride botPermission = parentVoice.getPermissionOverride(member);
        long allowRaw = botPermission != null ? botPermission.getAllowedRaw() : 0L;
        long denyRaw = botPermission != null ? botPermission.getDeniedRaw() : 0L;

        return channelAction.addPermissionOverride(
                member,
                allowRaw | Permission.MANAGE_CHANNEL.getRawValue(),
                denyRaw & ~Permission.MANAGE_CHANNEL.getRawValue()
        );
    }

    private static String getNewVoiceName(AutoChannelBean autoChannelBean, VoiceChannel parentVoice, Member member, int n) {
        String name = autoChannelBean.getNameMask();
        name = AutoChannel.resolveVariables(name, parentVoice.getName(), String.valueOf(n), member.getEffectiveName());
        name = name.substring(0, Math.min(100, name.length()));
        return name;
    }

}
