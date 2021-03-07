package events.discordevents.guildvoicejoin;

import commands.Command;
import commands.runnables.utilitycategory.AutoChannelCommand;
import core.PermissionCheckRuntime;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildVoiceJoinAbstract;
import modules.AutoChannel;
import mysql.modules.autochannel.AutoChannelBean;
import mysql.modules.autochannel.DBAutoChannel;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildBean;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;

@DiscordEvent
public class GuildVoiceChannelMemberJoinAutoChannel extends GuildVoiceJoinAbstract {

    @Override
    public boolean onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if (!event.getChannelJoined().getMembers().contains(event.getMember()))
            return true;

        Guild guild = event.getGuild();
        AutoChannelBean autoChannelBean = DBAutoChannel.getInstance().retrieve(guild.getIdLong());
        if (autoChannelBean.isActive() && event.getChannelJoined().getIdLong() == autoChannelBean.getParentChannelId().orElse(0L)) {
            GuildBean guildBean = DBGuild.getInstance().retrieve(guild.getIdLong());
            if (PermissionCheckRuntime.getInstance().botHasPermission(guildBean.getLocale(), AutoChannelCommand.class, event.getGuild(), Permission.MANAGE_CHANNEL, Permission.VOICE_MOVE_OTHERS, Permission.VOICE_CONNECT) &&
                    (event.getChannelJoined().getParent() != null || PermissionCheckRuntime.getInstance().botHasPermission(guildBean.getLocale(), AutoChannelCommand.class, event.getChannelJoined().getParent(), Permission.VOICE_MOVE_OTHERS, Permission.VOICE_CONNECT))
            ) {
                int n = 1;
                for (int i = 0; i < 50; i++) {
                    if (!event.getGuild().getVoiceChannelsByName(getNewVoiceName(autoChannelBean, event.getChannelJoined(), event.getMember(), n), true).isEmpty())
                        n++;
                    else break;
                }

                if (!event.getChannelJoined().getMembers().contains(event.getMember()))
                    return true;

                ChannelAction<VoiceChannel> channelAction = createNewVoice(autoChannelBean, event.getChannelJoined(), event.getMember(), n);
                channelAction.queue(voiceChannel -> processCreatedVoice(autoChannelBean, voiceChannel, event.getMember()),
                        e -> channelAction.setName("???")
                                .queue(voiceChannel -> processCreatedVoice(autoChannelBean, voiceChannel, event.getMember()))
                );
            }
        }

        return true;
    }

    private void processCreatedVoice(AutoChannelBean autoChannelBean, VoiceChannel voiceChannel, Member member) {
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

    private ChannelAction<VoiceChannel> createNewVoice(AutoChannelBean autoChannelBean, VoiceChannel parentVoice, Member member, int n) {
        ChannelAction<VoiceChannel> channelAction;
        if (parentVoice.getParent() != null) {
            channelAction = parentVoice.getParent().createVoiceChannel(getNewVoiceName(autoChannelBean, parentVoice, member, n));
        } else {
            channelAction = parentVoice.getGuild().createVoiceChannel(getNewVoiceName(autoChannelBean, parentVoice, member, n));
        }
        channelAction = channelAction.setBitrate(parentVoice.getBitrate())
                .setUserlimit(parentVoice.getUserLimit());

        if (autoChannelBean.isLocked())
            channelAction = channelAction.setUserlimit(1);

        channelAction = addOriginalPermissions(parentVoice, channelAction);
        channelAction = addBotPermissions(parentVoice, channelAction);
        return addCreatorPermissions(parentVoice, channelAction, member);
    }

    private ChannelAction<VoiceChannel> addOriginalPermissions(VoiceChannel parentVoice, ChannelAction<VoiceChannel> channelAction) {
        for(PermissionOverride permissionOverride : parentVoice.getPermissionOverrides()) {
            if (permissionOverride.getPermissionHolder() != null) {
                channelAction = channelAction.addPermissionOverride(permissionOverride.getPermissionHolder(), permissionOverride.getAllowed(), permissionOverride.getDenied());
            }
        }
        return channelAction;
    }

    private ChannelAction<VoiceChannel> addBotPermissions(VoiceChannel parentVoice, ChannelAction<VoiceChannel> channelAction) {
        PermissionOverride botPermission = parentVoice.getPermissionOverride(parentVoice.getGuild().getSelfMember());
        long allowRaw = botPermission != null ? botPermission.getAllowedRaw() : 0L;
        long denyRaw = botPermission != null ? botPermission.getDeniedRaw() : 0L;

        return channelAction.addPermissionOverride(
                parentVoice.getGuild().getSelfMember(),
                allowRaw | Permission.MANAGE_CHANNEL.getRawValue() | Permission.VOICE_CONNECT.getRawValue(),
                denyRaw
        );
    }

    private ChannelAction<VoiceChannel> addCreatorPermissions(VoiceChannel parentVoice, ChannelAction<VoiceChannel> channelAction, Member member) {
        PermissionOverride botPermission = parentVoice.getPermissionOverride(member);
        long allowRaw = botPermission != null ? botPermission.getAllowedRaw() : 0L;
        long denyRaw = botPermission != null ? botPermission.getDeniedRaw() : 0L;

        return channelAction.addPermissionOverride(member, allowRaw | Permission.MANAGE_CHANNEL.getRawValue(), denyRaw);
    }

    private String getNewVoiceName(AutoChannelBean autoChannelBean, VoiceChannel parentVoice, Member member, int n) {
        String name = autoChannelBean.getNameMask();
        name = AutoChannel.resolveVariables(name, parentVoice.getName(), String.valueOf(n), member.getEffectiveName());
        name = name.substring(0, Math.min(100, name.length()));
        return name;
    }

}
