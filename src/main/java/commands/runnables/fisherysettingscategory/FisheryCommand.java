package commands.runnables.fisherysettingscategory;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import commands.NavigationHelper;
import commands.listeners.CommandProperties;
import commands.listeners.OnStaticButtonListener;
import commands.runnables.NavigationAbstract;
import constants.*;
import core.CustomObservableList;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicTextChannel;
import core.schedule.MainScheduler;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.fisheryusers.FisheryMemberData;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "fishery",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "️⚙️️",
        executableWithoutArgs = true,
        aliases = { "fishingsetup", "fisherysetup", "levels", "levelsystem", "fisherysettings" }
)
public class FisheryCommand extends NavigationAbstract implements OnStaticButtonListener {

    private static final int MAX_CHANNELS = 50;

    private GuildData guildBean;
    private boolean stopLock = true;
    private NavigationHelper<AtomicTextChannel> channelNavigationHelper;
    private CustomObservableList<AtomicTextChannel> ignoredChannels;

    public static final String EMOJI_TREASURE = "💰";
    public static final String EMOJI_KEY = "🔑";

    public FisheryCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws Throwable {
        guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
        FisheryGuildData fisheryGuildBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong());
        ignoredChannels = AtomicTextChannel.transformIdList(event.getGuild(), fisheryGuildBean.getIgnoredChannelIds());
        channelNavigationHelper = new NavigationHelper<>(this, ignoredChannels, AtomicTextChannel.class, MAX_CHANNELS);
        registerNavigationListener(7);
        return true;
    }

    @Override
    public Response controllerMessage(GuildMessageReceivedEvent event, String input, int state) {
        if (state == 1) {
            List<TextChannel> channelList = MentionUtil.getTextChannels(event.getMessage(), input).getList();
            return channelNavigationHelper.addData(AtomicTextChannel.from(channelList), input, event.getMessage().getMember(), 0);
        }

        return null;
    }

    @Override
    public boolean controllerReaction(GenericGuildMessageReactionEvent event, int i, int state) {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        removeNavigationWithMessage();
                        return false;

                    case 0:
                        guildBean.toggleFisheryTreasureChests();
                        setLog(LogStatus.SUCCESS, getString("treasurechestsset", guildBean.isFisheryTreasureChests()));
                        return true;

                    case 1:
                        guildBean.toggleFisheryReminders();
                        setLog(LogStatus.SUCCESS, getString("remindersset", guildBean.isFisheryReminders()));
                        return true;

                    case 2:
                        guildBean.toggleFisheryCoinsGivenLimit();
                        setLog(LogStatus.SUCCESS, getString("coinsgivenset", guildBean.hasFisheryCoinsGivenLimit()));
                        return true;

                    case 3:
                        channelNavigationHelper.startDataAdd(1);
                        return true;

                    case 4:
                        channelNavigationHelper.startDataRemove(2);
                        return true;

                    case 5:
                        if (guildBean.getFisheryStatus() != FisheryStatus.ACTIVE) {
                            guildBean.setFisheryStatus(FisheryStatus.ACTIVE);
                            stopLock = true;
                        } else {
                            guildBean.setFisheryStatus(FisheryStatus.PAUSED);
                        }
                        setLog(LogStatus.SUCCESS, getString("setstatus"));
                        return true;

                    case 6:
                        if (guildBean.getFisheryStatus() == FisheryStatus.ACTIVE) {
                            if (stopLock) {
                                stopLock = false;
                                setLog(LogStatus.WARNING, getString("stoplock"));
                            } else {
                                DBFishery.getInstance().removePowerPlant(event.getGuild().getIdLong());
                                setLog(LogStatus.SUCCESS, getString("setstatus"));
                            }
                            return true;
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
    public EmbedBuilder draw(int state) {
        switch (state) {
            case 0:
                setOptions(getString("state0_options_" + guildBean.getFisheryStatus().ordinal()).split("\n"));

                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_mstatus"), "**" + getString("state0_status").split("\n")[guildBean.getFisheryStatus().ordinal()].toUpperCase() + "**\n" + Emojis.ZERO_WIDTH_SPACE, false)
                        .addField(getString("state0_mtreasurechests_title", StringUtil.getEmojiForBoolean(guildBean.isFisheryTreasureChests())), getString("state0_mtreasurechests_desc"), true)
                        .addField(getString("state0_mreminders_title", StringUtil.getEmojiForBoolean(guildBean.isFisheryReminders())), getString("state0_mreminders_desc"), true)
                        .addField(getString("state0_mcoinsgivenlimit_title", StringUtil.getEmojiForBoolean(guildBean.hasFisheryCoinsGivenLimit())), getString("state0_mcoinsgivenlimit_desc"), true)
                        .addField(getString("state0_mchannels"), new ListGen<AtomicTextChannel>().getList(ignoredChannels, getLocale(), IMentionable::getAsMention), false);

            case 1:
                return channelNavigationHelper.drawDataAdd(getString("state1_title"), getString("state1_description"));
            case 2:
                return channelNavigationHelper.drawDataRemove();

            default:
                return null;
        }
    }

    @Override
    public void onStaticButton(ButtonClickEvent event) throws Throwable {
        DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong()).remove(event.getMessage().getIdLong());
        Message message = event.getMessage();

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(FisheryCommand.EMOJI_TREASURE + " " + TextManager.getString(getLocale(), Category.FISHERY_SETTINGS, "fishery_treasure_title"))
                .setDescription(TextManager.getString(getLocale(), Category.FISHERY_SETTINGS, "fishery_treasure_opening", event.getMember().getAsMention()));

        message.editMessage(eb.build())
                .setActionRows()
                .queue();

        FisheryMemberData userBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberBean(event.getMember().getIdLong());
        MainScheduler.getInstance().schedule(3, ChronoUnit.SECONDS, "treasure_reveal", () -> {
            Random r = new Random();
            String[] winLose = new String[] { "win", "lose" };
            int resultInt = r.nextInt(2);
            String result = winLose[resultInt];

            long won = Math.round(userBean.getMemberGear(FisheryGear.TREASURE).getEffect() * (0.7 + r.nextDouble() * 0.6));

            String treasureImage;
            if (resultInt == 0) {
                treasureImage = "https://cdn.discordapp.com/attachments/711665837114654781/711665935026618398/treasure_opened_win.png";
            } else {
                treasureImage = "https://cdn.discordapp.com/attachments/711665837114654781/711665948549054555/treasure_opened_lose.png";
            }

            EmbedBuilder eb2 = EmbedFactory.getEmbedDefault()
                    .setTitle(FisheryCommand.EMOJI_TREASURE + " " + getString("treasure_title"))
                    .setDescription(getString("treasure_opened_" + result, event.getMember().getAsMention(), StringUtil.numToString(won)))
                    .setImage(treasureImage)
                    .setFooter(getString("treasure_footer"));

            message.editMessage(eb2.build()).queue();

            TextChannel channel = event.getTextChannel();
            if (resultInt == 0 && BotPermissionUtil.canWriteEmbed(channel)) {
                channel.sendMessage(userBean.changeValuesEmbed(0, won).build())
                        .queue(m -> {
                            MainScheduler.getInstance().schedule(Settings.FISHERY_DESPAWN_MINUTES, ChronoUnit.MINUTES, "treasure_remove_account_change", () -> {
                                if (BotPermissionUtil.can(channel, Permission.VIEW_CHANNEL)) {
                                    m.delete().queue();
                                }
                            });
                        });
            }

            MainScheduler.getInstance().schedule(Settings.FISHERY_DESPAWN_MINUTES, ChronoUnit.MINUTES, "treasure_remove", () -> {
                if (BotPermissionUtil.can(channel, Permission.VIEW_CHANNEL)) {
                    message.delete().queue();
                }
            });
        });
    }

}
