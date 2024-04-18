package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.GuildChannelsStateProcessor;
import commands.stateprocessor.StringStateProcessor;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicVoiceChannel;
import core.utils.StringUtil;
import modules.MemberCountDisplay;
import mysql.hibernate.entity.BotLogEntity;
import mysql.modules.membercountdisplays.DBMemberCountDisplays;
import mysql.modules.membercountdisplays.MemberCountData;
import mysql.modules.membercountdisplays.MemberCountDisplaySlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@CommandProperties(
        trigger = "mcdisplays",
        userGuildPermissions = Permission.MANAGE_SERVER,
        botGuildPermissions = Permission.VOICE_CONNECT,
        emoji = "️🧮️",
        executableWithoutArgs = true,
        aliases = {"membercountdisplays", "memberscountdisplays", "memberdisplays", "mdisplays", "countdisplays", "displays", "mcdisplay"}
)
public class MemberCountDisplayCommand extends NavigationAbstract {

    public static int MAX_NAME_MASK_LENGTH = 50;

    public static final int STATE_ADD = 1,
            STATE_DISCONNECT = 2,
            STATE_SET_CHANNEL = 3,
            STATE_SET_NAME_MASK = 4;

    private MemberCountData memberCountData;
    private AtomicVoiceChannel currentChannel = null;
    private String currentNameMask = null;

    public MemberCountDisplayCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        memberCountData = DBMemberCountDisplays.getInstance().retrieve(event.getGuild().getIdLong());
        registerNavigationListener(event.getMember(), List.of(
                new GuildChannelsStateProcessor(this, STATE_SET_CHANNEL, STATE_ADD, getString("dashboard_vc"))
                        .setMinMax(1, 1)
                        .setChannelTypes(List.of(ChannelType.VOICE))
                        .setCheckPermissions(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL, Permission.MANAGE_PERMISSIONS)
                        .setSingleGetter(() -> currentChannel != null ? currentChannel.getIdLong() : null)
                        .setSingleSetter(channelId -> currentChannel = new AtomicVoiceChannel(event.getGuild().getIdLong(), channelId)),
                new StringStateProcessor(this, STATE_SET_NAME_MASK, STATE_ADD, getString("dashboard_mask"))
                        .setMax(MAX_NAME_MASK_LENGTH)
                        .setDescription(getString("state4_desc"))
                        .setClearButton(false)
                        .setGetter(() -> currentNameMask)
                        .setSetter(input -> currentNameMask = input)
        ));
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                deregisterListenersWithComponentMessage();
                return false;

            case 0:
                if (memberCountData.getMemberCountDisplaySlots().size() < 5) {
                    currentChannel = null;
                    currentNameMask = getString("state1_example");
                    setState(STATE_ADD);
                    return true;
                } else {
                    setLog(LogStatus.FAILURE, getString("toomanydisplays"));
                    return true;
                }

            case 1:
                if (!memberCountData.getMemberCountDisplaySlots().isEmpty()) {
                    setState(STATE_DISCONNECT);
                    return true;
                } else {
                    setLog(LogStatus.FAILURE, getString("nothingtoremove"));
                    return true;
                }

            default:
                return false;
        }
    }

    @ControllerButton(state = STATE_ADD)
    public boolean onButtonAdd(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                setState(DEFAULT_STATE);
                return true;
            }
            case 0 -> {
                setState(STATE_SET_CHANNEL);
                return true;
            }
            case 1 -> {
                setState(STATE_SET_NAME_MASK);
                return true;
            }
            case 2 -> {
                String err = MemberCountDisplay.initialize(getLocale(), currentChannel, currentNameMask);
                if (err != null) {
                    setLog(LogStatus.FAILURE, err);
                    return true;
                }

                memberCountData.getMemberCountDisplaySlots().put(currentChannel.getIdLong(), new MemberCountDisplaySlot(event.getGuild().getIdLong(), currentChannel.getIdLong(), currentNameMask));
                MemberCountDisplay.manage(getLocale(), event.getGuild());

                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.MEMBER_COUNT_DISPLAYS_ADD, event.getMember(), currentChannel.getIdLong());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("displayadd"));
                setState(DEFAULT_STATE);
                return true;
            }
        }
        return false;
    }

    @ControllerButton(state = STATE_DISCONNECT)
    public boolean onButtonDisconnect(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        } else if (i < memberCountData.getMemberCountDisplaySlots().size()) {
            MemberCountDisplaySlot slot = memberCountData.getMemberCountDisplaySlots().remove(new ArrayList<>(memberCountData.getMemberCountDisplaySlots().keySet()).get(i));

            if (slot != null) {
                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.MEMBER_COUNT_DISPLAYS_DISCONNECT, event.getMember(), slot.getVoiceChannelId());
                getEntityManager().getTransaction().commit();
            }

            setLog(LogStatus.SUCCESS, getString("displayremove"));
            if (memberCountData.getMemberCountDisplaySlots().isEmpty()) {
                setState(DEFAULT_STATE);
            }
            return true;
        }
        return false;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) {
        setComponents(getString("state0_options").split("\n"));
        String listString = new ListGen<MemberCountDisplaySlot>()
                .getList(memberCountData.getMemberCountDisplaySlots().values(), getLocale(), data -> {
                    return getString("state0_displays", new AtomicVoiceChannel(member.getGuild().getIdLong(), data.getVoiceChannelId()).getPrefixedNameInField(getLocale()), StringUtil.escapeMarkdown(data.getMask()));
                });

        return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                .addField(getString("state0_mdisplays"), highlightVariables(listString), false);
    }

    @Draw(state = STATE_ADD)
    public EmbedBuilder drawAdd(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        setComponents(getString("state1_options").split("\n"), Set.of(2));
        return EmbedFactory.getEmbedDefault(this, null)
                .addField(getString("dashboard_vc"), currentChannel != null ? currentChannel.getPrefixedNameInField(getLocale()) : notSet, true)
                .addField(getString("dashboard_mask"), highlightVariables(currentNameMask), true);
    }

    @Draw(state = STATE_DISCONNECT)
    public EmbedBuilder drawDisconnect(Member member) {
        ArrayList<MemberCountDisplaySlot> channelNames = new ArrayList<>(memberCountData.getMemberCountDisplaySlots().values());
        String[] roleStrings = new String[channelNames.size()];
        for (int i = 0; i < roleStrings.length; i++) {
            roleStrings[i] = channelNames.get(i).getMask();
        }
        setComponents(roleStrings);
        return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));
    }

    private String highlightVariables(String str) {
        return modules.MemberCountDisplay.replaceVariables(str, "`%MEMBERS`", "`%USERS`", "`%BOTS`", "`%BOOSTS`");
    }

}
