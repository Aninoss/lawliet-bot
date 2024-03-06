package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.EmbedFactory;
import core.TextManager;
import core.atomicassets.AtomicVoiceChannel;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.AutoChannel;
import mysql.hibernate.entity.BotLogEntity;
import mysql.modules.autochannel.AutoChannelData;
import mysql.modules.autochannel.DBAutoChannel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "autochannel",
        botGuildPermissions = { Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL },
        userGuildPermissions = { Permission.MANAGE_CHANNEL },
        emoji = "🔊",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "tempchannel" }
)
public class AutoChannelCommand extends NavigationAbstract {

    public static final int MAX_CHANNEL_NAME_LENGTH = 50;

    private AutoChannelData autoChannelData;

    public AutoChannelCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        autoChannelData = DBAutoChannel.getInstance().retrieve(event.getGuild().getIdLong());
        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input, int state) {
        switch (state) {
            case 1:
                List<VoiceChannel> channelList = MentionUtil.getVoiceChannels(event.getMessage(), input).getList();
                if (channelList.isEmpty()) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return MessageInputResponse.FAILED;
                } else {
                    VoiceChannel voiceChannel = channelList.get(0);
                    String channelMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(getLocale(), voiceChannel, Permission.VOICE_CONNECT, Permission.VOICE_MOVE_OTHERS);
                    if (channelMissingPerms != null) {
                        setLog(LogStatus.FAILURE, channelMissingPerms);
                        return MessageInputResponse.FAILED;
                    }

                    Category parent = voiceChannel.getParentCategory();
                    if (parent != null) {
                        String categoryMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(getLocale(), parent, Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL);
                        if (categoryMissingPerms != null) {
                            setLog(LogStatus.FAILURE, categoryMissingPerms);
                            return MessageInputResponse.FAILED;
                        }
                    }

                    getEntityManager().getTransaction().begin();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.AUTO_CHANNEL_INITIAL_VOICE_CHANNEL, event.getMember(), autoChannelData.getParentChannelId().orElse(null), voiceChannel.getIdLong());
                    getEntityManager().getTransaction().commit();

                    autoChannelData.setParentChannelId(voiceChannel.getIdLong());
                    setLog(LogStatus.SUCCESS, getString("channelset"));
                    setState(0);
                    return MessageInputResponse.SUCCESS;
                }

            case 2:
                if (!input.isEmpty() && input.length() < MAX_CHANNEL_NAME_LENGTH) {
                    getEntityManager().getTransaction().begin();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.AUTO_CHANNEL_NEW_CHANNEL_NAME, event.getMember(), autoChannelData.getNameMask(), input);
                    getEntityManager().getTransaction().commit();

                    autoChannelData.setNameMask(input);
                    setLog(LogStatus.SUCCESS, getString("channelnameset"));
                    setState(0);
                    return MessageInputResponse.SUCCESS;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "args_too_long", StringUtil.numToString(MAX_CHANNEL_NAME_LENGTH)));
                    return MessageInputResponse.FAILED;
                }

            default:
                return null;
        }
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
                        autoChannelData.toggleActive();

                        getEntityManager().getTransaction().begin();
                        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.AUTO_CHANNEL_ACTIVE, event.getMember(), null, autoChannelData.isActive());
                        getEntityManager().getTransaction().commit();

                        setLog(LogStatus.SUCCESS, getString("activeset", autoChannelData.isActive()));
                        return true;

                    case 1:
                        setState(1);
                        return true;

                    case 2:
                        setState(2);
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

            case 1:
                if (i == -1) {
                    setState(0);
                    return true;
                }

            case 2:
                if (i == -1) {
                    setState(0);
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
                TextChannel textChannel = getTextChannel().get();
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_mactive"), StringUtil.getOnOffForBoolean(textChannel, getLocale(), autoChannelData.isActive()), true)
                        .addField(getString("state0_mchannel"), autoChannelData.getParentChannel().map(c -> new AtomicVoiceChannel(c).getPrefixedNameInField(getLocale())).orElse(notSet), true)
                        .addField(getString("state0_mchannelname"), AutoChannel.resolveVariables(
                                StringUtil.escapeMarkdown(autoChannelData.getNameMask()),
                                "`%VCNAME`",
                                "`%INDEX`",
                                "`%CREATOR`"
                        ), true)
                        .addField(getString("state0_mlocked"), getString("state0_mlocked_desc", StringUtil.getOnOffForBoolean(textChannel, getLocale(), autoChannelData.isLocked())), true);

            case 1:
                return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

            case 2:
                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));

            default:
                return null;
        }
    }

}
