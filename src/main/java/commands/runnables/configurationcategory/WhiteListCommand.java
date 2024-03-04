package commands.runnables.configurationcategory;

import commands.Category;
import commands.CommandEvent;
import commands.NavigationHelper;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.CustomObservableList;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicTextChannel;
import core.utils.JDAUtil;
import core.utils.MentionUtil;
import mysql.hibernate.entity.BotLogEntity;
import mysql.modules.whitelistedchannels.DBWhiteListedChannels;
import mysql.modules.whitelistedchannels.WhiteListedChannelsData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "whitelist",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "✅",
        executableWithoutArgs = true,
        aliases = { "wl" },
        obsolete = true
)
public class WhiteListCommand extends NavigationAbstract {

    public static final int MAX_CHANNELS = 100;

    private NavigationHelper<AtomicTextChannel> channelNavigationHelper;
    private CustomObservableList<AtomicTextChannel> whiteListedChannels;

    public WhiteListCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        WhiteListedChannelsData whiteListedChannelsBean = DBWhiteListedChannels.getInstance().retrieve(event.getGuild().getIdLong());
        whiteListedChannels = AtomicTextChannel.transformIdList(event.getGuild(), whiteListedChannelsBean.getChannelIds());
        channelNavigationHelper = new NavigationHelper<>(this, guildEntity -> whiteListedChannels, AtomicTextChannel.class, MAX_CHANNELS);
        setLog(LogStatus.WARNING, TextManager.getString(getLocale(), Category.CONFIGURATION, "cperms_obsolete", getPrefix()));
        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input, int state) {
        if (state == 1) {
            List<TextChannel> channelList = MentionUtil.getTextChannels(event.getGuild(), input).getList();
            return channelNavigationHelper.addData(AtomicTextChannel.from(channelList), input, event.getMember(), 0, BotLogEntity.Event.CHANNEL_WHITELIST);
        }

        return null;
    }

    @Override
    public boolean controllerButton(ButtonInteractionEvent event, int i, int state) {
        switch (state) {
            case 0:
                switch (i) {
                    case -1 -> {
                        deregisterListenersWithComponentMessage();
                        return false;
                    }
                    case 0 -> {
                        channelNavigationHelper.startDataAdd(1);
                        return true;
                    }
                    case 1 -> {
                        channelNavigationHelper.startDataRemove(2);
                        return true;
                    }
                    case 2 -> {
                        if (!whiteListedChannels.isEmpty()) {
                            getEntityManager().getTransaction().begin();
                            BotLogEntity.log(getEntityManager(), BotLogEntity.Event.CHANNEL_WHITELIST, event.getMember(), null, JDAUtil.toIdList(whiteListedChannels));
                            getEntityManager().getTransaction().commit();

                            whiteListedChannels.clear();
                            setLog(LogStatus.SUCCESS, getString("channelcleared"));
                        } else {
                            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "element_start_remove_none_channel"));
                        }
                        return true;
                    }
                }
                return false;

            case 1:
                if (i == -1) {
                    setState(0);
                    return true;
                }
                break;

            case 2:
                return channelNavigationHelper.removeData(i, event.getMember(), 0, BotLogEntity.Event.CHANNEL_WHITELIST);
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(Member member, int state) {
        String everyChannel = getString("all");
        switch (state) {
            case 0 -> {
                setComponents(getString("state0_options").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(
                                getString("state0_mchannel"),
                                new ListGen<AtomicTextChannel>().getList(whiteListedChannels, everyChannel, m -> m.getPrefixedNameInField(getLocale())),
                                true
                        );
            }
            case 1 -> {
                return channelNavigationHelper.drawDataAdd();
            }
            case 2 -> {
                return channelNavigationHelper.drawDataRemove(getLocale());
            }
        }
        return null;
    }

}
