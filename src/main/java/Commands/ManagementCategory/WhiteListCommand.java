package Commands.ManagementCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onNavigationListener;
import CommandSupporters.Command;
import CommandSupporters.NavigationHelper;
import Constants.LogStatus;
import Constants.Permission;
import Constants.Response;
import General.*;
import General.Mention.MentionTools;
import MySQL.DBServerOld;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.sql.SQLException;
import java.util.ArrayList;

@CommandProperties(
    trigger = "whitelist",
    userPermissions = Permission.MANAGE_SERVER,
    emoji = "✅",
    thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/128/check-1-icon.png",
    executable = true
)
public class WhiteListCommand extends Command implements onNavigationListener {

    private static final int MAX_CHANNELS = 3;

    private ArrayList<ServerTextChannel> channels;
    private NavigationHelper<ServerTextChannel> channelNavigationHelper;

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        channels = DBServerOld.getWhiteListedChannels(event.getServer().get());
        channelNavigationHelper = new NavigationHelper<>(this, channels, ServerTextChannel.class, MAX_CHANNELS);
        return true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable {
        if (state == 1) {
            ArrayList<ServerTextChannel> channelList = MentionTools.getTextChannels(event.getMessage(), inputString).getList();
            return channelNavigationHelper.addData(channelList, inputString, event.getMessage().getUserAuthor().get(), 0, channel -> {
                try {
                    DBServerOld.addWhiteListedChannel(event.getServer().get(), channel);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        }

        return null;
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        deleteNavigationMessage();
                        return false;

                    case 0:
                        channelNavigationHelper.startDataAdd(1);
                        return true;

                    case 1:
                        channelNavigationHelper.startDataRemove(2);
                        return true;

                    case 2:
                        if (channels.size() > 0) {
                            setLog(LogStatus.SUCCESS, getString("channelcleared"));
                            new ArrayList<>(channels).forEach(channel -> {
                                try {
                                    channels.remove(channel);
                                    DBServerOld.removeWhiteListedChannel(event.getServer().get(), channel);
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            });
                            channels.clear();
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
                return channelNavigationHelper.removeData(i, 0, channel -> {
                    try {
                        DBServerOld.removeWhiteListedChannel(event.getServer().get(), channel);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
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
                       .addField(getString("state0_mchannel"), new ListGen<ServerTextChannel>().getList(channels, everyChannel, Mentionable::getMentionTag), true);

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
