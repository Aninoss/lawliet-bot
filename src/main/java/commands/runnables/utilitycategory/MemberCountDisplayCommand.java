package commands.runnables.utilitycategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.MemberCacheController;
import core.TextManager;
import core.atomicassets.AtomicVoiceChannel;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.MemberCountDisplay;
import mysql.modules.membercountdisplays.DBMemberCountDisplays;
import mysql.modules.membercountdisplays.MemberCountData;
import mysql.modules.membercountdisplays.MemberCountDisplaySlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.managers.ChannelManager;

@CommandProperties(
        trigger = "mcdisplays",
        userGuildPermissions = Permission.MANAGE_SERVER,
        botGuildPermissions = Permission.VOICE_CONNECT,
        emoji = "️🧮️",
        executableWithoutArgs = true,
        aliases = { "membercountdisplays", "memberscountdisplays", "memberdisplays", "mdisplays", "countdisplays", "displays", "mcdisplay" }
)
public class MemberCountDisplayCommand extends NavigationAbstract {

    private MemberCountData memberCountBean;
    private AtomicVoiceChannel currentVC = null;
    private String currentName = null;

    public MemberCountDisplayCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(CommandEvent event, String args) {
        memberCountBean = DBMemberCountDisplays.getInstance().retrieve(event.getGuild().getIdLong());
        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(GuildMessageReceivedEvent event, String input, int state) {
        if (state == 1) {
            List<VoiceChannel> vcList = MentionUtil.getVoiceChannels(event.getMessage(), input).getList();
            if (vcList.size() == 0) {
                String checkString = input.toLowerCase();
                if (!modules.MemberCountDisplay.replaceVariables(checkString, "", "", "", "").equals(checkString)) {
                    if (input.length() <= 50) {
                        currentName = input;
                        setLog(LogStatus.SUCCESS, getString("nameset"));
                        return MessageInputResponse.SUCCESS;
                    } else {
                        setLog(LogStatus.FAILURE, getString("nametoolarge", "50"));
                        return MessageInputResponse.FAILED;
                    }
                }

                setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
            } else {
                VoiceChannel channel = vcList.get(0);
                if (checkChannel(channel)) {
                    currentVC = new AtomicVoiceChannel(channel);
                    setLog(LogStatus.SUCCESS, getString("vcset"));
                    return MessageInputResponse.SUCCESS;
                }
            }
            return MessageInputResponse.FAILED;
        }

        return null;
    }

    @Override
    public boolean controllerButton(ButtonClickEvent event, int i, int state) {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        deregisterListenersWithComponentMessage();
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
                        if (!checkChannel(voiceChannel)) {
                            return true;
                        }

                        MemberCacheController.getInstance().loadMembersFull(event.getGuild()).join();
                        ChannelManager manager = voiceChannel.getManager();
                        try {
                            for (PermissionOverride permissionOverride : voiceChannel.getPermissionOverrides()) {
                                manager = manager.putPermissionOverride(
                                        permissionOverride.getPermissionHolder(),
                                        permissionOverride.getAllowedRaw() & ~Permission.VOICE_CONNECT.getRawValue(),
                                        permissionOverride.getDeniedRaw() | Permission.VOICE_CONNECT.getRawValue()
                                );
                            }
                        } catch (InsufficientPermissionException | ErrorResponseException e) {
                            //Ignore
                            setLog(LogStatus.FAILURE, getString("nopermissions"));
                            return true;
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

                        Member self = event.getGuild().getSelfMember();
                        long permissionBotOverride = Permission.MANAGE_CHANNEL.getRawValue() | Permission.VOICE_CONNECT.getRawValue();
                        PermissionOverride permissionBot = voiceChannel.getPermissionOverride(self);
                        manager = manager.putPermissionOverride(
                                self,
                                (permissionBot != null ? permissionBot.getAllowedRaw() : 0) | permissionBotOverride,
                                permissionBot != null ? permissionBot.getDeniedRaw() & ~permissionBotOverride : 0
                        );

                        try {
                            manager.complete();
                        } catch (ErrorResponseException e) {
                            //Ignore
                            setLog(LogStatus.FAILURE, getString("nopermissions"));
                            return true;
                        }

                        memberCountBean.getMemberCountBeanSlots().put(voiceChannel.getIdLong(), new MemberCountDisplaySlot(event.getGuild().getIdLong(), voiceChannel.getIdLong(), currentName));
                        MemberCountDisplay.manage(getLocale(), event.getGuild());

                        setLog(LogStatus.SUCCESS, getString("displayadd"));
                        setState(0);
                        return true;
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
                    if (memberCountBean.getMemberCountBeanSlots().size() == 0) {
                        setState(0);
                    }
                    return true;
                }

            default:
                return false;
        }
    }

    @Override
    public EmbedBuilder draw(Member member, int state) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");

        switch (state) {
            case 0:
                setComponents(getString("state0_options").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_mdisplays"), highlightVariables(new ListGen<MemberCountDisplaySlot>()
                                .getList(memberCountBean.getMemberCountBeanSlots().values(), getLocale(), bean -> {
                                    if (bean.getVoiceChannel().isPresent()) {
                                        return getString("state0_displays", StringUtil.escapeMarkdown(bean.getVoiceChannel().get().getAsMention()), StringUtil.escapeMarkdown(bean.getMask()));
                                    } else {
                                        return getString("state0_displays", TextManager.getString(getLocale(), TextManager.GENERAL, "notfound", StringUtil.numToHex(bean.getVoiceChannelId())), StringUtil.escapeMarkdown(bean.getMask()));
                                    }
                                })), false);

            case 1:
                if (currentName != null && currentVC != null) setComponents(getString("state1_options"));
                return EmbedFactory.getEmbedDefault(this, getString("state1_description", StringUtil.escapeMarkdown(Optional.ofNullable(currentVC).flatMap(AtomicVoiceChannel::get).map(GuildChannel::getAsMention).orElse(notSet)), highlightVariables(StringUtil.escapeMarkdown(Optional.ofNullable(currentName).orElse(notSet)))), getString("state1_title"));

            case 2:
                ArrayList<MemberCountDisplaySlot> channelNames = new ArrayList<>(memberCountBean.getMemberCountBeanSlots().values());
                String[] roleStrings = new String[channelNames.size()];
                for (int i = 0; i < roleStrings.length; i++) {
                    roleStrings[i] = channelNames.get(i).getMask();
                }
                setComponents(roleStrings);
                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));

            default:
                return null;
        }
    }

    private boolean checkChannel(VoiceChannel channel) {
        String channelMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(getLocale(), channel, Permission.MANAGE_CHANNEL, Permission.MANAGE_PERMISSIONS, Permission.VOICE_CONNECT);
        if (channelMissingPerms != null) {
            setLog(LogStatus.FAILURE, channelMissingPerms);
            return false;
        }
        if (memberCountBean.getMemberCountBeanSlots().containsKey(channel.getIdLong())) {
            setLog(LogStatus.FAILURE, getString("alreadyexists"));
            return false;
        }

        return true;
    }

    private String highlightVariables(String str) {
        return modules.MemberCountDisplay.replaceVariables(str, "`%MEMBERS`", "`%USERS`", "`%BOTS`", "`%BOOSTS`");
    }

}
