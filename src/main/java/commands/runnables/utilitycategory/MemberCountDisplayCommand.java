package commands.runnables.utilitycategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import constants.Response;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicVoiceChannel;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.MemberCountDisplay;
import mysql.modules.membercountdisplays.DBMemberCountDisplays;
import mysql.modules.membercountdisplays.MemberCountBean;
import mysql.modules.membercountdisplays.MemberCountDisplaySlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;
import net.dv8tion.jda.api.managers.ChannelManager;

@CommandProperties(
        trigger = "mcdisplays",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "️🧮️",
        executableWithoutArgs = true,
        aliases = { "membercountdisplays", "memberscountdisplays", "memberdisplays", "mdisplays", "countdisplays", "displays", "mcdisplay" }
)
public class MemberCountDisplayCommand extends NavigationAbstract {

    private MemberCountBean memberCountBean;
    private AtomicVoiceChannel currentVC = null;
    private String currentName = null;

    public MemberCountDisplayCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        memberCountBean = DBMemberCountDisplays.getInstance().retrieve(event.getGuild().getIdLong());
        memberCountBean.getMemberCountBeanSlots().trim(vcId -> event.getGuild().getVoiceChannelById(vcId));
        registerNavigationListener(5);
        return true;
    }

    @Override
    public Response controllerMessage(GuildMessageReceivedEvent event, String input, int state) {
        if (state == 1) {
            List<VoiceChannel> vcList = MentionUtil.getVoiceChannels(event.getMessage(), input).getList();
            if (vcList.size() == 0) {
                String checkString = input.toLowerCase();
                if (!modules.MemberCountDisplay.replaceVariables(checkString, "", "", "", "").equals(checkString)) {
                    if (input.length() <= 50) {
                        currentName = input;
                        setLog(LogStatus.SUCCESS, getString("nameset"));
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, getString("nametoolarge", "50"));
                        return Response.FALSE;
                    }
                }

                setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                return Response.FALSE;
            } else {
                VoiceChannel channel = vcList.get(0);

                List<Permission> missingPermissions = BotPermissionUtil.getMissingPermissions(channel, event.getGuild().getSelfMember(), Permission.MANAGE_CHANNEL, Permission.MANAGE_PERMISSIONS, Permission.VOICE_CONNECT);
                if (missingPermissions.size() > 0) {
                    String permissionsList = new ListGen<Permission>().getList(missingPermissions, ListGen.SLOT_TYPE_BULLET, permission -> TextManager.getString(getLocale(), TextManager.PERMISSIONS, permission.getName()));
                    setLog(LogStatus.FAILURE, getString("missing_perms", permissionsList));
                    return Response.FALSE;
                }

                if (memberCountBean.getMemberCountBeanSlots().containsKey(channel.getIdLong())) {
                    setLog(LogStatus.FAILURE, getString("alreadyexists"));
                    return Response.FALSE;
                }

                currentVC = new AtomicVoiceChannel(channel);

                setLog(LogStatus.SUCCESS, getString("vcset"));
                return Response.TRUE;
            }
        }

        return null;
    }

    @Override
    public boolean controllerReaction(GenericGuildMessageReactionEvent event, int i, int state) {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        removeNavigationWithMessage();
                        return false;

                    case 0:
                        if (memberCountBean.getMemberCountBeanSlots().size() < 5) {
                            setState(1);
                            currentVC = null;
                            currentName = null;
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, getString("toomanydisplays"));
                            return true;
                        }

                    case 1:
                        if (memberCountBean.getMemberCountBeanSlots().size() > 0) {
                            setState(2);
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, getString("nothingtoremove"));
                            return true;
                        }

                    default:
                        return false;
                }

            case 1:
                if (i == -1) {
                    setState(0);
                    return true;
                }

                if (i == 0 && currentName != null && currentVC != null) {
                    Optional<VoiceChannel> vcOpt = currentVC.get();
                    if (vcOpt.isPresent()) {
                        VoiceChannel voiceChannel = vcOpt.get();
                        ChannelManager manager = voiceChannel.getManager();

                        for (PermissionOverride permissionOverride : voiceChannel.getPermissionOverrides()) {
                            manager = manager.putPermissionOverride(
                                    permissionOverride.getPermissionHolder(),
                                    permissionOverride.getAllowedRaw() & ~Permission.VOICE_CONNECT.getRawValue(),
                                    permissionOverride.getDeniedRaw() | Permission.VOICE_CONNECT.getRawValue()
                            );
                        }

                        Role publicRole = event.getGuild().getPublicRole();
                        PermissionOverride permissionOverride = voiceChannel.getPermissionOverride(publicRole);
                        if (permissionOverride == null) {
                            manager = manager.putPermissionOverride(
                                    publicRole,
                                    0,
                                    Permission.VOICE_CONNECT.getRawValue()
                            );
                        }

                        Member botMember = event.getGuild().getSelfMember();
                        long permissionBotOverride = Permission.MANAGE_CHANNEL.getRawValue() | Permission.MANAGE_PERMISSIONS.getRawValue() | Permission.VOICE_CONNECT.getRawValue();
                        PermissionOverride permissionBot = voiceChannel.getPermissionOverride(botMember);
                        manager = manager.putPermissionOverride(
                                botMember,
                                (permissionBot != null ? permissionBot.getAllowedRaw() : 0) | permissionBotOverride,
                                permissionBot != null ? permissionBot.getDeniedRaw() & ~permissionBotOverride : 0
                        );

                        try {
                            manager.complete();
                            memberCountBean.getMemberCountBeanSlots().put(currentVC.getId(), new MemberCountDisplaySlot(event.getGuild().getIdLong(), currentVC.getId(), currentName));
                            MemberCountDisplay.getInstance().manage(getLocale(), event.getGuild());

                            setLog(LogStatus.SUCCESS, getString("displayadd"));
                            setState(0);

                            return true;
                        } catch (Throwable e) {
                            //Ignore
                        }
                    }

                    setLog(LogStatus.FAILURE, getString("nopermissions"));
                    return true;
                }
                return false;

            case 2:
                if (i == -1) {
                    setState(0);
                    return true;
                } else if (i < memberCountBean.getMemberCountBeanSlots().size()) {
                    memberCountBean.getMemberCountBeanSlots().remove(new ArrayList<>(memberCountBean.getMemberCountBeanSlots().keySet()).get(i));

                    setLog(LogStatus.SUCCESS, getString("displayremove"));
                    setState(0);
                    return true;
                }

            default:
                return false;
        }
    }

    @Override
    public EmbedBuilder draw(int state) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");

        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_mdisplays"), highlightVariables(new ListGen<MemberCountDisplaySlot>()
                                .getList(memberCountBean.getMemberCountBeanSlots().values(), getLocale(), bean -> {
                                    if (bean.getVoiceChannel().isPresent()) {
                                        return getString("state0_displays", StringUtil.escapeMarkdown(bean.getVoiceChannel().get().getName()), StringUtil.escapeMarkdown(bean.getMask()));
                                    } else {
                                        return getString("state0_displays", "???", StringUtil.escapeMarkdown(bean.getMask()));
                                    }
                                })), false);

            case 1:
                if (currentName != null && currentVC != null) setOptions(new String[] { getString("state1_options") });
                return EmbedFactory.getEmbedDefault(this, getString("state1_description", StringUtil.escapeMarkdown(Optional.ofNullable(currentVC).flatMap(AtomicVoiceChannel::get).map(GuildChannel::getName).orElse(notSet)), highlightVariables(StringUtil.escapeMarkdown(Optional.ofNullable(currentName).orElse(notSet)))), getString("state1_title"));

            case 2:
                ArrayList<MemberCountDisplaySlot> channelNames = new ArrayList<>(memberCountBean.getMemberCountBeanSlots().values());
                String[] roleStrings = new String[channelNames.size()];
                for (int i = 0; i < roleStrings.length; i++) {
                    roleStrings[i] = channelNames.get(i).getMask();
                }
                setOptions(roleStrings);
                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));

            default:
                return null;
        }
    }

    private String highlightVariables(String str) {
        return modules.MemberCountDisplay.replaceVariables(str, "`%MEMBERS`", "`%USERS`", "`%BOTS`", "`%BOOSTS`");
    }

}
