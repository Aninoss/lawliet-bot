package commands.runnables.fisherysettingscategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
import modules.fishery.Fishery;
import modules.fishery.FisheryGear;
import modules.fishery.FisheryPowerUp;
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
import net.dv8tion.jda.api.utils.TimeFormat;
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

    public static final String STATIC_MESSAGE_ID_TREASURE = "treasure";
    public static final String STATIC_MESSAGE_ID_POWERUP = "powerup";

    private GuildData guildBean;
    private boolean stopLock = true;
    private NavigationHelper<AtomicTextChannel> channelNavigationHelper;
    private CustomObservableList<AtomicTextChannel> ignoredChannels;

    public static final String EMOJI_TREASURE = "💰";
    public static final String EMOJI_KEY = "🔑";
    public static final String EMOJI_POWERUP = "❔";

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
                        guildBean.toggleFisheryPowerups();
                        setLog(LogStatus.SUCCESS, getString("powerupsset", guildBean.isFisheryPowerups()));
                        stopLock = true;
                        return true;

                    case 2:
                        guildBean.toggleFisheryReminders();
                        setLog(LogStatus.SUCCESS, getString("remindersset", guildBean.isFisheryReminders()));
                        stopLock = true;
                        return true;

                    case 3:
                        guildBean.toggleFisheryCoinsGivenLimit();
                        setLog(LogStatus.SUCCESS, getString("coinsgivenset", guildBean.hasFisheryCoinsGivenLimit()));
                        stopLock = true;
                        return true;

                    case 4:
                        channelNavigationHelper.startDataAdd(1);
                        stopLock = true;
                        return true;

                    case 5:
                        channelNavigationHelper.startDataRemove(2);
                        stopLock = true;
                        return true;

                    case 6:
                        if (guildBean.getFisheryStatus() != FisheryStatus.ACTIVE) {
                            guildBean.setFisheryStatus(FisheryStatus.ACTIVE);
                        } else {
                            guildBean.setFisheryStatus(FisheryStatus.PAUSED);
                        }
                        setLog(LogStatus.SUCCESS, getString("setstatus"));
                        stopLock = true;
                        return true;

                    case 7:
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
                        .addField(getString("state0_mpowerups_title", StringUtil.getEmojiForBoolean(channel, guildBean.isFisheryPowerups()).getFormatted()), getString("state0_mpowerups_desc"), true)
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
    public void onStaticButton(ButtonInteractionEvent event, String secondaryId) {
        if (!(event.getChannel() instanceof TextChannel)) {
            return;
        }

        if (secondaryId == null || secondaryId.equals(STATIC_MESSAGE_ID_TREASURE)) {
            processTreasureChest(event);
        } else if (secondaryId.split(":")[0].equals(STATIC_MESSAGE_ID_POWERUP)) {
            if (event.getUser().getId().equals(secondaryId.split(":")[1])) {
                processPowerUp(event);
            } else {
                EmbedBuilder eb = EmbedFactory.getEmbedError()
                        .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "rejected"))
                        .setDescription(getString("powerup_notforyou"));
                event.replyEmbeds(eb.build())
                        .setEphemeral(true)
                        .queue();
            }
        }
    }

    private void processTreasureChest(ButtonInteractionEvent event) {
        DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong())
                .remove(event.getMessage().getIdLong());

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(FisheryCommand.EMOJI_TREASURE + " " + TextManager.getString(getLocale(), Category.FISHERY_SETTINGS, "fishery_treasure_title"))
                .setDescription(TextManager.getString(getLocale(), Category.FISHERY_SETTINGS, "fishery_treasure_opening", event.getMember().getEffectiveName()));

        event.editMessageEmbeds(eb.build())
                .setComponents()
                .queue();

        MainScheduler.schedule(3, ChronoUnit.SECONDS, "treasure_reveal", () -> processTreasureChestReveal(event));
    }

    private void processTreasureChestReveal(ButtonInteractionEvent event) {
        InteractionHook hook = event.getHook();
        FisheryMemberData memberData = DBFishery.getInstance().retrieve(event.getGuild().getIdLong())
                .getMemberData(event.getMember().getIdLong());

        Random r = new Random();
        String[] winLose = new String[] { "win", "lose" };
        int resultInt = r.nextInt(2);
        String result = winLose[resultInt];

        long won = Math.round(memberData.getMemberGear(FisheryGear.TREASURE).getEffect() * (0.7 + r.nextDouble() * 0.6));

        String treasureImage;
        if (resultInt == 0) {
            treasureImage = "https://cdn.discordapp.com/attachments/711665837114654781/711665935026618398/treasure_opened_win.png";
        } else {
            treasureImage = "https://cdn.discordapp.com/attachments/711665837114654781/711665948549054555/treasure_opened_lose.png";
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(FisheryCommand.EMOJI_TREASURE + " " + getString("treasure_title"))
                .setDescription(getString("treasure_opened_" + result, event.getMember().getEffectiveName(), StringUtil.numToString(won)))
                .setImage(treasureImage)
                .setFooter(getString("treasure_footer"));

        StandardGuildMessageChannel channel = (StandardGuildMessageChannel) event.getChannel();
        if (resultInt == 0 && BotPermissionUtil.canWriteEmbed(channel)) {
            event.getMessage().editMessageEmbeds(eb.build(), memberData.changeValuesEmbed(event.getMember(), 0, won).build()).queue();
        } else {
            hook.editOriginalEmbeds(eb.build()).queue();
        }

        MainScheduler.schedule(Settings.FISHERY_DESPAWN_MINUTES, ChronoUnit.MINUTES, "treasure_remove", () -> {
            if (BotPermissionUtil.can(channel, Permission.VIEW_CHANNEL)) {
                hook.deleteOriginal().queue();
            }
        });
    }

    private void processPowerUp(ButtonInteractionEvent event) {
        Fishery.deregisterPowerUp(event.getMessageIdLong());
        DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong())
                .remove(event.getMessage().getIdLong());

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(FisheryCommand.EMOJI_POWERUP + " " + TextManager.getString(getLocale(), Category.FISHERY_SETTINGS, "fishery_powerup_title"))
                .setDescription(TextManager.getString(getLocale(), Category.FISHERY_SETTINGS, "fishery_powerup_using"))
                .setThumbnail("https://cdn.discordapp.com/attachments/1077245845440827562/1077942025460129852/roulette.gif");

        event.editMessageEmbeds(eb.build())
                .setComponents()
                .queue();

        MainScheduler.schedule(3, ChronoUnit.SECONDS, "powerup_reveal", () -> processPowerUpReveal(event));
    }

    private void processPowerUpReveal(ButtonInteractionEvent event) {
        Random r = new Random();
        InteractionHook hook = event.getHook();
        FisheryMemberData memberData = DBFishery.getInstance().retrieve(event.getGuild().getIdLong())
                .getMemberData(event.getMember().getIdLong());

        ArrayList<FisheryPowerUp> possiblePowerUps = new ArrayList<>(List.of(FisheryPowerUp.values()));
        possiblePowerUps.removeAll(memberData.getActivePowerUps());
        FisheryPowerUp powerUp = possiblePowerUps.get(r.nextInt(possiblePowerUps.size()));
        Instant expiration = Instant.now().plus(powerUp.getValidDuration());

        String desc = getString(
                "powerup_result",
                !powerUp.getValidDuration().isZero(),
                getString("powerup_description_" + powerUp.ordinal()),
                TimeFormat.DATE_TIME_SHORT.atInstant(expiration).toString()
        );

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(FisheryCommand.EMOJI_POWERUP + " " + getString("powerup", powerUp.ordinal()))
                .setDescription(desc)
                .setThumbnail(powerUp.getImageUrl())
                .setFooter(getString("powerup_footer"));

        StandardGuildMessageChannel channel = (StandardGuildMessageChannel) event.getChannel();
        hook.editOriginalEmbeds(eb.build()).queue();
        memberData.activatePowerUp(powerUp, expiration);

        MainScheduler.schedule(Settings.FISHERY_DESPAWN_MINUTES, ChronoUnit.MINUTES, "powerup_remove", () -> {
            if (BotPermissionUtil.can(channel, Permission.VIEW_CHANNEL)) {
                hook.deleteOriginal().queue();
            }
        });
    }

}
