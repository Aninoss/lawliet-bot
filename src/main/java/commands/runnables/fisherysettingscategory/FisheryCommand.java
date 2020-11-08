package commands.runnables.fisherysettingscategory;

import commands.Command;
import commands.NavigationHelper;
import commands.listeners.CommandProperties;
import commands.listeners.OnNavigationListener;
import commands.listeners.OnReactionAddStaticListener;
import constants.*;
import core.*;
import core.schedule.MainScheduler;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryServerBean;
import mysql.modules.fisheryusers.FisheryUserBean;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;
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

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

@CommandProperties(
        trigger = "fishery",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "️⚙️️",
        executableWithoutArgs = true,
        aliases = {"fishingsetup", "fisherysetup", "levels", "levelsystem", "fisherysettings"}
)
public class FisheryCommand extends Command implements OnNavigationListener, OnReactionAddStaticListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(FisheryCommand.class);

    private static final int MAX_CHANNELS = 50;

    private ServerBean serverBean;
    private boolean stopLock = true;
    private NavigationHelper<ServerTextChannel> channelNavigationHelper;
    private CustomObservableList<ServerTextChannel> ignoredChannels;

    public static final String treasureEmoji = "💰";
    public static final String keyEmoji = "🔑";
    private static final ArrayList<Message> blockedTreasureMessages = new ArrayList<>();

    public FisheryCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

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
                        serverBean.toggleFisheryTreasureChests();
                        setLog(LogStatus.SUCCESS, getString("treasurechestsset", serverBean.isFisheryTreasureChests()));
                        return true;

                    case 1:
                        serverBean.toggleFisheryReminders();
                        setLog(LogStatus.SUCCESS, getString("remindersset", serverBean.isFisheryReminders()));
                        return true;

                    case 2:
                        serverBean.toggleFisheryCoinsGivenLimit();
                        setLog(LogStatus.SUCCESS, getString("coinsgivenset", serverBean.hasFisheryCoinsGivenLimit()));
                        return true;

                    case 3:
                        channelNavigationHelper.startDataAdd(1);
                        return true;

                    case 4:
                        channelNavigationHelper.startDataRemove(2);
                        return true;

                    case 5:
                        if (serverBean.getFisheryStatus() != FisheryStatus.ACTIVE) {
                            serverBean.setFisheryStatus(FisheryStatus.ACTIVE);
                            stopLock = true;
                        } else {
                            serverBean.setFisheryStatus(FisheryStatus.PAUSED);
                        }
                        setLog(LogStatus.SUCCESS, getString("setstatus"));
                        return true;

                    case 6:
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

                    default:
                        return false;
                }

            case 1:
                if (i == -1) {
                    setState(0);
                    return true;
                }
                return false;

            case 2:
                return channelNavigationHelper.removeData(i, 0);

            default:
                return false;
        }
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        switch (state) {
            case 0:
                setOptions(getString("state0_options_"+ serverBean.getFisheryStatus().ordinal()).split("\n"));

                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_mstatus"), "**" + getString("state0_status").split("\n")[serverBean.getFisheryStatus().ordinal()].toUpperCase() + "**\n" + Emojis.EMPTY_EMOJI, false)
                        .addField(getString("state0_mtreasurechests_title", StringUtil.getEmojiForBoolean(serverBean.isFisheryTreasureChests())), getString("state0_mtreasurechests_desc"), true)
                        .addField(getString("state0_mreminders_title", StringUtil.getEmojiForBoolean(serverBean.isFisheryReminders())), getString("state0_mreminders_desc"), true)
                        .addField(getString("state0_mcoinsgivenlimit_title", StringUtil.getEmojiForBoolean(serverBean.hasFisheryCoinsGivenLimit())), getString("state0_mcoinsgivenlimit_desc"), true)
                        .addField(getString("state0_mchannels"), new ListGen<ServerTextChannel>().getList(ignoredChannels, getLocale(), Mentionable::getMentionTag), false);

            case 1: return channelNavigationHelper.drawDataAdd(getString("state1_title"), getString("state1_description"));
            case 2: return channelNavigationHelper.drawDataRemove();

            default: return null;
        }
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 7;
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
                if (message.getChannel().canYouRemoveReactionsOfOthers())
                    message.removeAllReactions().get();

                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(FisheryCommand.treasureEmoji + " " + TextManager.getString(getLocale(), Category.FISHERY_SETTINGS, "fishery_treasure_title"))
                        .setDescription(TextManager.getString(getLocale(), Category.FISHERY_SETTINGS, "fishery_treasure_opening", event.getUser().get().getMentionTag()));
                message.edit(eb).get();

                Thread.sleep(1000 * 3);

                Random r = new Random();
                String[] winLose = new String[]{"win", "lose"};
                int resultInt = r.nextInt(2);
                String result = winLose[resultInt];

                FisheryUserBean userBean = DBFishery.getInstance().getBean(event.getServer().get().getId()).getUserBean(event.getUserId());
                long won = Math.round(userBean.getPowerUp(FisheryCategoryInterface.PER_TREASURE).getEffect() * (0.7 + r.nextDouble() * 0.6));

                String treasureImage;
                if (resultInt == 0) treasureImage = "https://cdn.discordapp.com/attachments/711665837114654781/711665935026618398/treasure_opened_win.png";
                else treasureImage = "https://cdn.discordapp.com/attachments/711665837114654781/711665948549054555/treasure_opened_lose.png";

                eb = EmbedFactory.getEmbedDefault()
                        .setTitle(FisheryCommand.treasureEmoji + " " +getString("treasure_title"))
                        .setDescription(getString("treasure_opened_" + result, event.getUser().get().getMentionTag(), StringUtil.numToString(won)))
                        .setImage(treasureImage)
                        .setFooter(getString("treasure_footer"));

                message.edit(eb);
                if (message.getChannel().canYouRemoveReactionsOfOthers()) message.removeAllReactions();

                ServerTextChannel channel = event.getServerTextChannel().get();
                Message accountUpdateMessage = null;
                if (resultInt == 0 && channel.canYouWrite() && channel.canYouEmbedLinks()) accountUpdateMessage = channel.sendMessage(userBean.changeValues(0, won)).get();

                final Message finalAccountUpdateMessage = accountUpdateMessage;
                MainScheduler.getInstance().schedule(1, ChronoUnit.MINUTES, () -> {
                    blockedTreasureMessages.remove(message);

                    MainScheduler.getInstance().schedule(Settings.FISHERY_DESPAWN_MINUTES, ChronoUnit.MINUTES, () -> {
                        if (finalAccountUpdateMessage != null) finalAccountUpdateMessage.delete();
                        message.delete();
                    });
                });
            }
        }
    }

    @Override
    public String getTitleStartIndicator() {
        return treasureEmoji;
    }
}
