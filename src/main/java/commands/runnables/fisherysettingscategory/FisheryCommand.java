package commands.runnables.fisherysettingscategory;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import commands.Category;
import commands.CommandEvent;
import commands.NavigationHelper;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.listeners.OnStaticButtonListener;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.LogStatus;
import constants.Settings;
import core.*;
import core.atomicassets.AtomicTextChannel;
import core.atomicassets.MentionableAtomicAsset;
import core.schedule.MainScheduler;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.fishery.FisheryGear;
import modules.fishery.FisheryStatus;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.fisheryusers.FisheryMemberData;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "fishery",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "️⚙️️",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "fishingsetup", "fisherysetup", "levels", "levelsystem", "fisherysettings" }
)
public class FisheryCommand extends NavigationAbstract implements OnStaticButtonListener {

    public static final int MAX_CHANNELS = 50;

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
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
        FisheryGuildData fisheryGuildBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong());
        ignoredChannels = AtomicTextChannel.transformIdList(event.getGuild(), fisheryGuildBean.getIgnoredChannelIds());
        channelNavigationHelper = new NavigationHelper<>(this, ignoredChannels, AtomicTextChannel.class, MAX_CHANNELS);
        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input, int state) {
        if (state == 1) {
            List<TextChannel> channelList = MentionUtil.getTextChannels(event.getGuild(), input).getList();
            return channelNavigationHelper.addData(AtomicTextChannel.from(channelList), input, event.getMessage().getMember(), 0);
        }

        return null;
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
                        guildBean.toggleFisheryTreasureChests();
                        setLog(LogStatus.SUCCESS, getString("treasurechestsset", guildBean.isFisheryTreasureChests()));
                        stopLock = true;
                        return true;

                    case 1:
                        guildBean.toggleFisheryReminders();
                        setLog(LogStatus.SUCCESS, getString("remindersset", guildBean.isFisheryReminders()));
                        stopLock = true;
                        return true;

                    case 2:
                        guildBean.toggleFisheryCoinsGivenLimit();
                        setLog(LogStatus.SUCCESS, getString("coinsgivenset", guildBean.hasFisheryCoinsGivenLimit()));
                        stopLock = true;
                        return true;

                    case 3:
                        channelNavigationHelper.startDataAdd(1);
                        stopLock = true;
                        return true;

                    case 4:
                        channelNavigationHelper.startDataRemove(2);
                        stopLock = true;
                        return true;

                    case 5:
                        if (guildBean.getFisheryStatus() != FisheryStatus.ACTIVE) {
                            guildBean.setFisheryStatus(FisheryStatus.ACTIVE);
                        } else {
                            guildBean.setFisheryStatus(FisheryStatus.PAUSED);
                        }
                        setLog(LogStatus.SUCCESS, getString("setstatus"));
                        stopLock = true;
                        return true;

                    case 6:
                        if (guildBean.getFisheryStatus() == FisheryStatus.ACTIVE) {
                            if (stopLock) {
                                stopLock = false;
                                setLog(LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "confirm_warning_button"));
                            } else {
                                GlobalThreadPool.getExecutorService()
                                        .submit(() -> DBFishery.getInstance().invalidateGuildId(event.getGuild().getIdLong()));
                                DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).setFisheryStatus(FisheryStatus.STOPPED);
                                setLog(LogStatus.SUCCESS, getString("setstatus"));
                                stopLock = true;
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
    public EmbedBuilder draw(Member member, int state) {
        switch (state) {
            case 0:
                String[] options = getString("state0_options_" + guildBean.getFisheryStatus().ordinal()).split("\n");
                Button[] buttons = new Button[options.length];
                for (int i = 0; i < options.length; i++) {
                    buttons[i] = Button.of(
                            i == 6 ? ButtonStyle.DANGER : ButtonStyle.PRIMARY,
                            String.valueOf(i),
                            options[i]
                    );
                }
                setComponents(buttons);

                TextChannel channel = getTextChannel().get();
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_mstatus"), "**" + getString("state0_status").split("\n")[guildBean.getFisheryStatus().ordinal()] + "**\n" + Emojis.ZERO_WIDTH_SPACE.getFormatted(), false)
                        .addField(getString("state0_mtreasurechests_title", StringUtil.getEmojiForBoolean(channel, guildBean.isFisheryTreasureChests()).getFormatted()), getString("state0_mtreasurechests_desc"), true)
                        .addField(getString("state0_mreminders_title", StringUtil.getEmojiForBoolean(channel, guildBean.isFisheryReminders()).getFormatted()), getString("state0_mreminders_desc"), true)
                        .addField(getString("state0_mcoinsgivenlimit_title", StringUtil.getEmojiForBoolean(channel, guildBean.hasFisheryCoinsGivenLimit()).getFormatted()), getString("state0_mcoinsgivenlimit_desc"), true)
                        .addField(getString("state0_mchannels"), new ListGen<AtomicTextChannel>().getList(ignoredChannels, getLocale(), MentionableAtomicAsset::getPrefixedNameInField), false);

            case 1:
                return channelNavigationHelper.drawDataAdd(getString("state1_title"), getString("state1_description"));
            case 2:
                return channelNavigationHelper.drawDataRemove();

            default:
                return null;
        }
    }

    @Override
    public void onStaticButton(ButtonInteractionEvent event) {
        if (event.getChannel() instanceof TextChannel) {
            DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong()).remove(event.getMessage().getIdLong());

            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setTitle(FisheryCommand.EMOJI_TREASURE + " " + TextManager.getString(getLocale(), Category.FISHERY_SETTINGS, "fishery_treasure_title"))
                    .setDescription(TextManager.getString(getLocale(), Category.FISHERY_SETTINGS, "fishery_treasure_opening", event.getMember().getAsMention()));

            event.editMessageEmbeds(eb.build())
                    .setComponents()
                    .queue();
            InteractionHook hook = event.getHook();

            FisheryMemberData userBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberData(event.getMember().getIdLong());
            MainScheduler.schedule(3, ChronoUnit.SECONDS, "treasure_reveal", () -> {
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

                StandardGuildMessageChannel channel = (StandardGuildMessageChannel) event.getChannel();
                if (resultInt == 0 && BotPermissionUtil.canWriteEmbed(channel)) {
                    event.getMessage().editMessageEmbeds(eb2.build(), userBean.changeValuesEmbed(event.getMember(), 0, won).build()).queue();
                } else {
                    hook.editOriginalEmbeds(eb2.build()).queue();
                }

                MainScheduler.schedule(Settings.FISHERY_DESPAWN_MINUTES, ChronoUnit.MINUTES, "treasure_remove", () -> {
                    if (BotPermissionUtil.can(channel, Permission.VIEW_CHANNEL)) {
                        hook.deleteOriginal().queue();
                    }
                });
            });
        }
    }

}
