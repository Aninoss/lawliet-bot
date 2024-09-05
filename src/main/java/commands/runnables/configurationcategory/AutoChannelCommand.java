package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.GuildChannelsStateProcessor;
import commands.stateprocessor.StringStateProcessor;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.atomicassets.AtomicVoiceChannel;
import core.utils.CollectionUtil;
import core.utils.StringUtil;
import modules.AutoChannel;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.AutoChannelEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "autochannel",
        botGuildPermissions = {Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL},
        userGuildPermissions = {Permission.MANAGE_CHANNEL},
        emoji = "🔊",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = {"tempchannel"}
)
public class AutoChannelCommand extends NavigationAbstract {

    public static final int MAX_CHANNEL_NAME_LENGTH = 50;
    public static final int MAX_PARENT_CHANNELS = 10;

    public static final int STATE_SET_CHANNEL = 1,
            STATE_SET_NAME = 2;

    public AutoChannelCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerNavigationListener(event.getMember(), List.of(
                new GuildChannelsStateProcessor(this, STATE_SET_CHANNEL, DEFAULT_STATE, getString("state0_mchannel"))
                        .setLogEvent(BotLogEntity.Event.AUTO_CHANNEL_INITIAL_VOICE_CHANNELS)
                        .setMinMax(0, MAX_PARENT_CHANNELS)
                        .setChannelTypes(List.of(ChannelType.VOICE))
                        .setCheckPermissions(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_MOVE_OTHERS)
                        .setCheckPermissionsParentCategory(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL)
                        .setGetter(() -> getGuildEntity().getAutoChannel().getParentChannelIds())
                        .setSetter(channelIds -> CollectionUtil.replace(getGuildEntity().getAutoChannel().getParentChannelIds(), channelIds)),
                new StringStateProcessor(this, STATE_SET_NAME, DEFAULT_STATE, getString("state0_mchannelname"))
                        .setDescription(getString("state2_description"))
                        .setMax(MAX_CHANNEL_NAME_LENGTH)
                        .setClearButton(false)
                        .setLogEvent(BotLogEntity.Event.AUTO_CHANNEL_NEW_CHANNEL_NAME)
                        .setGetter(() -> getGuildEntity().getAutoChannel().getNameMask())
                        .setSetter(input -> getGuildEntity().getAutoChannel().setNameMask(input))
        ));
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        AutoChannelEntity autoChannel = getGuildEntity().getAutoChannel();
        switch (i) {
            case -1:
                deregisterListenersWithComponentMessage();
                return false;

            case 0:
                autoChannel.beginTransaction();
                autoChannel.setActive(!autoChannel.getActive());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.AUTO_CHANNEL_ACTIVE, event.getMember(), null, autoChannel.getActive());
                autoChannel.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("activeset", autoChannel.getActive()));
                return true;

            case 1:
                setState(STATE_SET_CHANNEL);
                return true;

            case 2:
                setState(STATE_SET_NAME);
                return true;

            case 3:
                autoChannel.beginTransaction();
                autoChannel.setBeginLocked(!autoChannel.getBeginLocked());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.AUTO_CHANNEL_BEGIN_LOCKED, event.getMember(), null, autoChannel.getBeginLocked());
                autoChannel.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("lockedset", autoChannel.getBeginLocked()));
                return true;

            default:
                return false;
        }
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) {
        setComponents(getString("state0_options").split("\n"));

        AutoChannelEntity autoChannel = getGuildEntity().getAutoChannel();
        GuildMessageChannel channel = getGuildMessageChannel().get();
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                .addField(getString("state0_mactive"), StringUtil.getOnOffForBoolean(channel, getLocale(), autoChannel.getActive()), true)
                .addField(getString("state0_mchannel"), new ListGen<AtomicVoiceChannel>().getList(autoChannel.getParentChannels(), getLocale(), m -> m.getPrefixedNameInField(getLocale())), true)
                .addField(getString("state0_mchannelname"), AutoChannel.resolveVariables(
                        StringUtil.escapeMarkdown(autoChannel.getNameMask()),
                        "`%VCNAME`",
                        "`%INDEX`",
                        "`%CREATOR`"
                ), true)
                .addField(getString("state0_mlocked"), getString("state0_mlocked_desc", StringUtil.getOnOffForBoolean(channel, getLocale(), autoChannel.getBeginLocked())), true);
    }

}
