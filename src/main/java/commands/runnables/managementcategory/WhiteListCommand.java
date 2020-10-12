package commands.runnables.managementcategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnNavigationListener;
import commands.Command;
import commands.NavigationHelper;
import constants.LogStatus;
import constants.Permission;
import constants.Response;
import core.*;
import core.utils.MentionUtil;
import mysql.modules.whitelistedchannels.DBWhiteListedChannels;
import mysql.modules.whitelistedchannels.WhiteListedChannelsBean;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.ArrayList;
import java.util.Locale;

@CommandProperties(
        trigger = "whitelist",
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "✅",
        executableWithoutArgs = true,
        aliases = {"wl"}
)
public class WhiteListCommand extends Command implements OnNavigationListener {

    private static final int MAX_CHANNELS = 50;

    private NavigationHelper<ServerTextChannel> channelNavigationHelper;
    private CustomObservableList<ServerTextChannel> whiteListedChannels;

    public WhiteListCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        WhiteListedChannelsBean whiteListedChannelsBean = DBWhiteListedChannels.getInstance().getBean(event.getServer().get().getId());
        whiteListedChannels = whiteListedChannelsBean.getChannelIds().transform(channelId -> event.getServer().get().getTextChannelById(channelId), DiscordEntity::getId);
        channelNavigationHelper = new NavigationHelper<>(this, whiteListedChannels, ServerTextChannel.class, MAX_CHANNELS);
        return true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable {
        if (state == 1) {
            ArrayList<ServerTextChannel> channelList = MentionUtil.getTextChannels(event.getMessage(), inputString).getList();
            return channelNavigationHelper.addData(channelList, inputString, event.getMessage().getUserAuthor().get(), 0);
        }

        return null;
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        removeNavigationWithMessage();
                        return false;

                    case 0:
                        channelNavigationHelper.startDataAdd(1);
                        return true;

                    case 1:
                        channelNavigationHelper.startDataRemove(2);
                        return true;

                    case 2:
                        if (whiteListedChannels.size() > 0) {
                            whiteListedChannels.clear();
                            setLog(LogStatus.SUCCESS, getString("channelcleared"));
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "element_start_remove_none_channel"));
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
                return channelNavigationHelper.removeData(i, 0);
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        String everyChannel = getString("all");
        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description"))
                       .addField(getString("state0_mchannel"), new ListGen<ServerTextChannel>().getList(whiteListedChannels, everyChannel, Mentionable::getMentionTag), true);

            case 1: return channelNavigationHelper.drawDataAdd();
            case 2: return channelNavigationHelper.drawDataRemove();
        }
        return null;
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 12;
    }
}
