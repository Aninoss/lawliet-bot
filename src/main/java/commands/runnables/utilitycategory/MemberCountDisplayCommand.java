package commands.runnables.utilitycategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicVoiceChannel;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.MemberCountDisplay;
import mysql.modules.membercountdisplays.DBMemberCountDisplays;
import mysql.modules.membercountdisplays.MemberCountData;
import mysql.modules.membercountdisplays.MemberCountDisplaySlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@CommandProperties(
        trigger = "mcdisplays",
        userGuildPermissions = Permission.MANAGE_SERVER,
        botGuildPermissions = Permission.VOICE_CONNECT,
        emoji = "️🧮️",
        executableWithoutArgs = true,
        aliases = { "membercountdisplays", "memberscountdisplays", "memberdisplays", "mdisplays", "countdisplays", "displays", "mcdisplay" }
)
public class MemberCountDisplayCommand extends NavigationAbstract {

    public static int MAX_NAME_MASK_LENGTH = 50;

    private MemberCountData memberCountBean;
    private AtomicVoiceChannel currentVC = null;
    private String currentName = null;

    public MemberCountDisplayCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        memberCountBean = DBMemberCountDisplays.getInstance().retrieve(event.getGuild().getIdLong());
        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input, int state) {
        if (state == 1) {
            List<VoiceChannel> vcList = MentionUtil.getVoiceChannels(event.getMessage(), input).getList();
            if (vcList.isEmpty()) {
                String checkString = input.toLowerCase();
                if (!modules.MemberCountDisplay.replaceVariables(checkString, "", "", "", "").equals(checkString)) {
                    if (input.length() <= MAX_NAME_MASK_LENGTH) {
                        currentName = input;
                        setLog(LogStatus.SUCCESS, getString("nameset"));
                        return MessageInputResponse.SUCCESS;
                    } else {
                        setLog(LogStatus.FAILURE, getString("nametoolarge", StringUtil.numToString(MAX_NAME_MASK_LENGTH)));
                        return MessageInputResponse.FAILED;
                    }
                }

                setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
            } else {
                VoiceChannel channel = vcList.get(0);
                String err = MemberCountDisplay.checkChannel(getLocale(), channel);
                if (err == null) {
                    currentVC = new AtomicVoiceChannel(channel);
                    setLog(LogStatus.SUCCESS, getString("vcset"));
                    return MessageInputResponse.SUCCESS;
                } else {
                    setLog(LogStatus.FAILURE, err);
                    return MessageInputResponse.FAILED;
                }
            }
            return MessageInputResponse.FAILED;
        }

        return null;
    }

    @Override
    public boolean controllerButton(ButtonInteractionEvent event, int i, int state) {
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
                        if (!memberCountBean.getMemberCountBeanSlots().isEmpty()) {
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
                        String err = MemberCountDisplay.initialize(getLocale(), voiceChannel);
                        if (err != null) {
                            setLog(LogStatus.FAILURE, err);
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
                    if (memberCountBean.getMemberCountBeanSlots().isEmpty()) {
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
            case 0 -> {
                setComponents(getString("state0_options").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_mdisplays"), highlightVariables(new ListGen<MemberCountDisplaySlot>()
                                .getList(memberCountBean.getMemberCountBeanSlots().values(), getLocale(), bean -> {
                                    if (bean.getVoiceChannel().isPresent()) {
                                        return getString("state0_displays", new AtomicVoiceChannel(bean.getVoiceChannel().get()).getPrefixedNameInField(getLocale()), StringUtil.escapeMarkdown(bean.getMask()));
                                    } else {
                                        return getString("state0_displays", TextManager.getString(getLocale(), TextManager.GENERAL, "notfound", StringUtil.numToHex(bean.getVoiceChannelId())), StringUtil.escapeMarkdown(bean.getMask()));
                                    }
                                })), false);
            }
            case 1 -> {
                if (currentName != null && currentVC != null) {
                    setComponents(getString("state1_options"));
                }
                String currentVoiceChannel = Optional.ofNullable(currentVC).map(m -> m.getPrefixedNameInField(getLocale())).orElse("**" + notSet + "**");
                String currentNameMask = "**" + highlightVariables(Optional.ofNullable(currentName).map(StringUtil::escapeMarkdown).orElse(notSet)) + "**";
                return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"))
                        .addField(getString("state1_status_title"), getString("state1_status_desc", currentVoiceChannel, currentNameMask), false);
            }
            case 2 -> {
                ArrayList<MemberCountDisplaySlot> channelNames = new ArrayList<>(memberCountBean.getMemberCountBeanSlots().values());
                String[] roleStrings = new String[channelNames.size()];
                for (int i = 0; i < roleStrings.length; i++) {
                    roleStrings[i] = channelNames.get(i).getMask();
                }
                setComponents(roleStrings);
                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));
            }
            default -> {
                return null;
            }
        }
    }

    private String highlightVariables(String str) {
        return modules.MemberCountDisplay.replaceVariables(str, "`%MEMBERS`", "`%USERS`", "`%BOTS`", "`%BOOSTS`");
    }

}
