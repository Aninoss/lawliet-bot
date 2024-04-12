package commands.runnables.moderationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.GuildChannelsStateProcessor;
import commands.stateprocessor.MembersStateProcessor;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.atomicassets.AtomicGuildChannel;
import core.atomicassets.AtomicMember;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.InviteFilterEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "invitefilter",
        botGuildPermissions = Permission.MESSAGE_MANAGE,
        userGuildPermissions = {Permission.MESSAGE_MANAGE, Permission.KICK_MEMBERS, Permission.BAN_MEMBERS},
        emoji = "✉️",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        usesExtEmotes = true,
        aliases = {"invitesfilter", "spblock", "inviteblock", "spfilter", "invitesblock"}
)
public class InviteFilterCommand extends NavigationAbstract {

    public static int MAX_EXCLUDED_MEMBERS = 25;
    public static int MAX_EXCLUDED_CHANNELS = 25;
    public static int MAX_LOG_RECEIVERS = 10;

    private static final int STATE_SET_EXCLUDED_MEMBERS = 1,
            STATE_SET_EXCLUDED_CHANNELS = 2,
            STATE_SET_LOG_RECEIVERS = 3,
            STATE_SET_ACTION = 4;

    public InviteFilterCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerNavigationListener(event.getMember(), List.of(
                new MembersStateProcessor(this, STATE_SET_EXCLUDED_MEMBERS, DEFAULT_STATE, getString("state0_mignoredusers"))
                        .setMinMax(0, MAX_EXCLUDED_MEMBERS)
                        .setLogEvent(BotLogEntity.Event.INVITE_FILTER_EXCLUDED_MEMBERS)
                        .setGetter(() -> getGuildEntity().getInviteFilter().getExcludedMemberIds())
                        .setSetter(userIds -> getGuildEntity().getInviteFilter().setExcludedMemberIds(userIds)),
                new GuildChannelsStateProcessor(this, STATE_SET_EXCLUDED_CHANNELS, DEFAULT_STATE, getString("state0_mignoredchannels"))
                        .setMinMax(0, MAX_EXCLUDED_CHANNELS)
                        .setChannelTypes(JDAUtil.GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES)
                        .setLogEvent(BotLogEntity.Event.INVITE_FILTER_EXCLUDED_CHANNELS)
                        .setGetter(() -> getGuildEntity().getInviteFilter().getExcludedChannelIds())
                        .setSetter(userIds -> getGuildEntity().getInviteFilter().setExcludedChannelIds(userIds)),
                new MembersStateProcessor(this, STATE_SET_LOG_RECEIVERS, DEFAULT_STATE, getString("state0_mlogreciever"))
                        .setMinMax(0, MAX_LOG_RECEIVERS)
                        .setLogEvent(BotLogEntity.Event.INVITE_FILTER_LOG_RECEIVERS)
                        .setGetter(() -> getGuildEntity().getInviteFilter().getLogReceiverUserIds())
                        .setSetter(userIds -> getGuildEntity().getInviteFilter().setLogReceiverUserIds(userIds))
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
                InviteFilterEntity inviteFilter = getGuildEntity().getInviteFilter();
                inviteFilter.beginTransaction();
                inviteFilter.setActive(!inviteFilter.getActive());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_FILTER_ACTIVE, event.getMember(), null, inviteFilter.getActive());
                inviteFilter.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("onoffset", !inviteFilter.getActive()));
                return true;

            case 1:
                setState(STATE_SET_EXCLUDED_MEMBERS);
                return true;

            case 2:
                setState(STATE_SET_EXCLUDED_CHANNELS);
                return true;

            case 3:
                setState(STATE_SET_LOG_RECEIVERS);
                return true;

            case 4:
                setState(STATE_SET_ACTION);
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = STATE_SET_ACTION)
    public boolean onButtonSetAction(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        } else if (i <= 2) {
            InviteFilterEntity.Action newAction = InviteFilterEntity.Action.values()[i];

            InviteFilterEntity inviteFilter = getGuildEntity().getInviteFilter();
            inviteFilter.beginTransaction();
            BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_FILTER_ACTION, event.getMember(), inviteFilter.getAction(), newAction);
            inviteFilter.setAction(newAction);
            inviteFilter.commitTransaction();

            setState(DEFAULT_STATE);
            setLog(LogStatus.SUCCESS, getString("actionset"));
            return true;
        }
        return false;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) {
        InviteFilterEntity inviteFilter = getGuildEntity().getInviteFilter();
        Locale locale = getLocale();

        setComponents(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                .addField(getString("state0_menabled"), StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), inviteFilter.getActive()), true)
                .addField(getString("state0_mignoredusers"), new ListGen<AtomicMember>().getList(inviteFilter.getExcludedMembers(), getLocale(), m -> m.getPrefixedNameInField(locale)), true)
                .addField(getString("state0_mignoredchannels"), new ListGen<AtomicGuildChannel>().getList(inviteFilter.getExcludedChannels(), getLocale(), m -> m.getPrefixedNameInField(locale)), true)
                .addField(getString("state0_mlogreciever"), new ListGen<AtomicMember>().getList(inviteFilter.getLogReceivers(), getLocale(), m -> m.getPrefixedNameInField(locale)), true)
                .addField(getString("state0_maction"), getString("state0_mactionlist").split("\n")[inviteFilter.getAction().ordinal()], true);
    }

    @Draw(state = STATE_SET_ACTION)
    public EmbedBuilder drawSetAction(Member member) {
        setComponents(getString("state0_mactionlist").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state4_description"), getString("state4_title"));
    }

}
