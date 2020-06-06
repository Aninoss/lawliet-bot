package Commands.FisheryCategory;

import CommandListeners.*;
import CommandSupporters.Command;
import CommandSupporters.NavigationHelper;
import Constants.*;
import Core.*;
import Core.Mention.MentionUtil;
import Core.Utils.StringUtil;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryServerBean;
import MySQL.Modules.FisheryUsers.FisheryUserBean;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "fishery",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "\u2699\uFE0F️",
        executable = true,
        aliases = {"fishingsetup", "fisherysetup", "levels", "levelsystem"}
)
public class FisheryCommand extends Command implements OnNavigationListener, OnReactionAddStaticListener {

    final static Logger LOGGER = LoggerFactory.getLogger(FisheryCommand.class);

    private static final int MAX_CHANNELS = 50;

    private ServerBean serverBean;
    private boolean stopLock = true;
    private NavigationHelper<ServerTextChannel> channelNavigationHelper;
    private CustomObservableList<ServerTextChannel> ignoredChannels;

    public static final String treasureEmoji = "\uD83D\uDCB0";
    public static final String keyEmoji = "\uD83D\uDD11";
    private static final ArrayList<Message> blockedTreasureMessages = new ArrayList<>();

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
        FisheryServerBean fisheryServerBean = DBFishery.getInstance().getBean(event.getServer().get().getId());
        ignoredChannels = fisheryServerBean.getIgnoredChannelIds().transform(channelId -> event.getServer().get().getTextChannelById(channelId), DiscordEntity::getId);
        channelNavigationHelper = new NavigationHelper<>(this, ignoredChannels, ServerTextChannel.class, MAX_CHANNELS);
        return true;
    }
    
    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable {
        switch (state) {
            case 1:
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
                        serverBean.toggleFisheryTreasureChests();
                        setLog(LogStatus.SUCCESS, getString("treasurechestsset", serverBean.isFisheryTreasureChests()));
                        return true;

                    case 1:
                        serverBean.toggleFisheryReminders();
                        setLog(LogStatus.SUCCESS, getString("remindersset", serverBean.isFisheryReminders()));
                        return true;

                    case 2:
                        channelNavigationHelper.startDataAdd(1);
                        return true;

                    case 3:
                        channelNavigationHelper.startDataRemove(2);
                        return true;

                    case 4:
                        if (serverBean.getFisheryStatus() != FisheryStatus.ACTIVE) {
                            serverBean.setFisheryStatus(FisheryStatus.ACTIVE);
                            stopLock = true;
                        } else {
                            serverBean.setFisheryStatus(FisheryStatus.PAUSED);
                        }
                        setLog(LogStatus.SUCCESS, getString("setstatus"));
                        return true;

                    case 5:
                        if (serverBean.getFisheryStatus() == FisheryStatus.ACTIVE) {
                            if (stopLock) {
                                stopLock = false;
                                setLog(LogStatus.WARNING, getString("stoplock"));
                                return true;
                            } else {
                                DBFishery.getInstance().removePowerPlant(event.getServer().get().getId());
                                setLog(LogStatus.SUCCESS, getString("setstatus"));
                                return true;
                            }
                        }
                }
                break;

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
        switch (state) {
            case 0:
                setOptions(getString("state0_options_"+ serverBean.getFisheryStatus().ordinal()).split("\n"));

                return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description"))
                        .addField(getString("state0_mstatus"), "**" + getString("state0_status").split("\n")[serverBean.getFisheryStatus().ordinal()].toUpperCase() + "**", true)
                        .addField(getString("state0_mtreasurechests"), StringUtil.getOnOffForBoolean(getLocale(), serverBean.isFisheryTreasureChests()), true)
                        .addField(getString("state0_mreminders"), StringUtil.getOnOffForBoolean(getLocale(), serverBean.isFisheryReminders()), true)
                        .addField(getString("state0_mchannels"), new ListGen<ServerTextChannel>().getList(ignoredChannels, getLocale(), Mentionable::getMentionTag), false);

            case 1: return channelNavigationHelper.drawDataAdd(getString("state1_title"), getString("state1_description"));
            case 2: return channelNavigationHelper.drawDataRemove();
        }
        return null;
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 6;
    }

    @Override
    public void onReactionAddStatic(Message message, ReactionAddEvent event) throws Throwable {
        if (event.getEmoji().getMentionTag().equalsIgnoreCase(keyEmoji)) {
            boolean blocked = false;
            for(Message message1: blockedTreasureMessages) {
                if (message1.getId() == message.getId()) {
                    blocked = true;
                    break;
                }
            }

            if (!blocked) {
                blockedTreasureMessages.add(message);
                if (message.getChannel().canYouRemoveReactionsOfOthers()) message.removeAllReactions().get();

                EmbedBuilder eb = EmbedFactory.getEmbed()
                        .setTitle(FisheryCommand.treasureEmoji + " " + TextManager.getString(getLocale(), Category.FISHERY, "fishery_treasure_title"))
                        .setDescription(TextManager.getString(getLocale(), Category.FISHERY, "fishery_treasure_opening", event.getUser().getMentionTag()));
                message.edit(eb).get();

                Thread.sleep(1000 * 3);

                Random r = new Random();
                String[] winLose = new String[]{"win", "lose"};
                int resultInt = r.nextInt(2);
                String result = winLose[resultInt];

                FisheryUserBean userBean = DBFishery.getInstance().getBean(event.getServer().get().getId()).getUserBean(event.getUser().getId());
                long won = Math.round(userBean.getPowerUp(FisheryCategoryInterface.PER_TREASURE).getEffect() * (0.7 + r.nextDouble() * 0.6));

                String treasureImage;
                if (resultInt == 0) treasureImage = "https://cdn.discordapp.com/attachments/711665837114654781/711665935026618398/treasure_opened_win.png";
                else treasureImage = "https://cdn.discordapp.com/attachments/711665837114654781/711665948549054555/treasure_opened_lose.png";

                eb = EmbedFactory.getEmbed()
                        .setTitle(FisheryCommand.treasureEmoji + " " +getString("treasure_title"))
                        .setDescription(getString("treasure_opened_" + result, event.getUser().getMentionTag(), StringUtil.numToString(getLocale(), won)))
                        .setImage(treasureImage)
                        .setFooter(getString("treasure_footer"));
                message.edit(eb);
                if (message.getChannel().canYouRemoveReactionsOfOthers()) message.removeAllReactions();

                ServerTextChannel channel = event.getServerTextChannel().get();
                if (resultInt == 0 && channel.canYouWrite() && channel.canYouEmbedLinks()) channel.sendMessage(userBean.changeValues(0, won)).get();

                Thread t = new CustomThread(() -> {
                    try {
                        Thread.sleep(1000 * 60);
                        blockedTreasureMessages.remove(message);
                    } catch (InterruptedException e) {
                        LOGGER.error("Interrupted", e);
                    }
                }, "treasure_block_countdown", 1);
                t.start();
            }
        } else {
            event.removeReaction();
        }
    }

    @Override
    public String getTitleStartIndicator() {
        return treasureEmoji;
    }
}
