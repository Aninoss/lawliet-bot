package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.GuildChannelsStateProcessor;
import commands.stateprocessor.StringStateProcessor;
import constants.LogStatus;
import core.EmbedFactory;
import core.TextManager;
import core.atomicassets.AtomicVoiceChannel;
import core.utils.StringUtil;
import modules.AutoChannel;
import mysql.hibernate.entity.BotLogEntity;
import mysql.modules.autochannel.AutoChannelData;
import mysql.modules.autochannel.DBAutoChannel;
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

    public static final int STATE_SET_CHANNEL = 1,
            STATE_SET_NAME = 2;

    private AutoChannelData autoChannelData;

    public AutoChannelCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        autoChannelData = DBAutoChannel.getInstance().retrieve(event.getGuild().getIdLong());
        registerNavigationListener(event.getMember(), List.of(
                new GuildChannelsStateProcessor(this, STATE_SET_CHANNEL, DEFAULT_STATE, getString("state0_mchannel"))
                        .setLogEvent(BotLogEntity.Event.AUTO_CHANNEL_INITIAL_VOICE_CHANNEL)
                        .setMinMax(1, 1)
                        .setChannelTypes(List.of(ChannelType.VOICE))
                        .setCheckPermissions(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.VOICE_MOVE_OTHERS)
                        .setCheckPermissionsParentCategory(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL)
                        .setSingleGetter(() -> autoChannelData.getParentChannelId().orElse(null))
                        .setSingleSetter(channelId -> autoChannelData.setParentChannelId(channelId)),
                new StringStateProcessor(this, STATE_SET_NAME, DEFAULT_STATE, getString("state0_mchannelname"))
                        .setDescription(getString("state2_description"))
                        .setMax(MAX_CHANNEL_NAME_LENGTH)
                        .setClearButton(false)
                        .setLogEvent(BotLogEntity.Event.AUTO_CHANNEL_NEW_CHANNEL_NAME)
                        .setGetter(() -> autoChannelData.getNameMask())
                        .setSetter(input -> autoChannelData.setNameMask(input))
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
                autoChannelData.toggleActive();

                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.AUTO_CHANNEL_ACTIVE, event.getMember(), null, autoChannelData.isActive());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("activeset", autoChannelData.isActive()));
                return true;

            case 1:
                setState(STATE_SET_CHANNEL);
                return true;

            case 2:
                setState(STATE_SET_NAME);
                return true;

            case 3:
                autoChannelData.toggleLocked();

                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.AUTO_CHANNEL_BEGIN_LOCKED, event.getMember(), null, autoChannelData.isLocked());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("lockedset", autoChannelData.isLocked()));
                return true;

            default:
                return false;
        }
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) {
        setComponents(getString("state0_options").split("\n"));

        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        GuildMessageChannel channel = getGuildMessageChannel().get();
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                .addField(getString("state0_mactive"), StringUtil.getOnOffForBoolean(channel, getLocale(), autoChannelData.isActive()), true)
                .addField(getString("state0_mchannel"), autoChannelData.getParentChannel().map(c -> new AtomicVoiceChannel(c).getPrefixedNameInField(getLocale())).orElse(notSet), true)
                .addField(getString("state0_mchannelname"), AutoChannel.resolveVariables(
                        StringUtil.escapeMarkdown(autoChannelData.getNameMask()),
                        "`%VCNAME`",
                        "`%INDEX`",
                        "`%CREATOR`"
                ), true)
                .addField(getString("state0_mlocked"), getString("state0_mlocked_desc", StringUtil.getOnOffForBoolean(channel, getLocale(), autoChannelData.isLocked())), true);
    }

}
