package commands.runnables.fisherysettingscategory;

import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnStaticButtonListener;
import commands.listeners.OnStaticEntitySelectMenuListener;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.GuildChannelsStateProcessor;
import constants.Emojis;
import constants.ExceptionIds;
import constants.LogStatus;
import constants.Settings;
import core.*;
import core.atomicassets.AtomicGuildChannel;
import core.cache.ServerPatreonBoostCache;
import core.modals.DurationModalBuilder;
import core.modals.IntModalBuilder;
import core.modals.ModalMediator;
import core.utils.*;
import modules.fishery.Fishery;
import modules.fishery.FisheryGear;
import modules.fishery.FisheryPowerUp;
import modules.fishery.FisheryStatus;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.FisheryEntity;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.redis.fisheryusers.FisheryGuildData;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@CommandProperties(
        trigger = "fishery",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "️⚙️️",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = {"fishingsetup", "fisherysetup", "levels", "levelsystem", "fisherysettings"}
)
public class FisheryCommand extends NavigationAbstract implements OnStaticButtonListener, OnStaticEntitySelectMenuListener {

    public static final int MAX_EXCLUDED_CHANNELS = 25;
    public static final int MAX_WEEKLY_TREASURE_CHEST_LIMIT = 999;

    public static final String BUTTON_ID_TREASURE = "open";
    public static final String BUTTON_ID_POWERUP = "use";
    public static final String ENTITY_SELECT_MENU_ID_COLLABORATION = "member";

    private static final int STATE_SET_EXCLUDED_CHANNELS = 1;

    private boolean stopLock = true;

    public static final String EMOJI_TREASURE = "💰";
    public static final String EMOJI_KEY = "🔑";
    public static final String EMOJI_POWERUP = "❔";

    public FisheryCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        registerNavigationListener(event.getMember(), List.of(
                new GuildChannelsStateProcessor(this, STATE_SET_EXCLUDED_CHANNELS, DEFAULT_STATE, getString("state0_mchannels"))
                        .setMinMax(0, MAX_EXCLUDED_CHANNELS)
                        .setChannelTypes(Collections.emptyList())
                        .setDescription(getString("excludedchannels"))
                        .setLogEvent(BotLogEntity.Event.FISHERY_EXCLUDED_CHANNELS)
                        .setGetter(() -> getGuildEntity().getFishery().getExcludedChannelIds())
                        .setSetter(channelIds -> CollectionUtil.replace(getGuildEntity().getFishery().getExcludedChannelIds(), channelIds))
        ));
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        FisheryEntity fishery = getGuildEntity().getFishery();
        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                fishery.beginTransaction();
                fishery.setTreasureChests(!fishery.getTreasureChests());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_TREASURE_CHESTS, event.getMember(), null, fishery.getTreasureChests());
                fishery.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("treasurechestsset", fishery.getTreasureChests()));
                stopLock = true;
                return true;
            }
            case 1 -> {
                fishery.beginTransaction();
                fishery.setPowerUps(!fishery.getPowerUps());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_POWER_UPS, event.getMember(), null, fishery.getPowerUps());
                fishery.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("powerupsset", fishery.getPowerUps()));
                stopLock = true;
                return true;
            }
            case 2 -> {
                fishery.beginTransaction();
                fishery.setFishReminders(!fishery.getFishReminders());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_FISH_REMINDERS, event.getMember(), null, fishery.getFishReminders());
                fishery.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("remindersset", fishery.getFishReminders()));
                stopLock = true;
                return true;
            }
            case 3 -> {
                fishery.beginTransaction();
                fishery.setCoinGiftLimit(!fishery.getCoinGiftLimit());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_COIN_GIFT_LIMIT, event.getMember(), null, fishery.getCoinGiftLimit());
                fishery.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("coinsgivenset", fishery.getCoinGiftLimit()));
                stopLock = true;
                return true;
            }
            case 4 -> {
                if (!ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
                    return true;
                }

                fishery.beginTransaction();
                fishery.setGraphicallyGeneratedAccountCards(!fishery.getGraphicallyGeneratedAccountCards());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_ACCOUNT_CARDS, event.getMember(), null, fishery.getGraphicallyGeneratedAccountCards());
                fishery.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("cardsset", fishery.getGraphicallyGeneratedAccountCards()));
                stopLock = true;
                return true;
            }
            case 5 -> {
                if (!ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
                    return true;
                }

                String treasureChestsId = "treasure_chests";
                TextInput textTreasureChests = TextInput.create(treasureChestsId, getString("probabilities_treasure"), TextInputStyle.SHORT)
                        .setValue(StringUtil.doubleToString(fishery.getTreasureChestProbabilityInPercentEffectively(), 2, getLocale()))
                        .setMinLength(1)
                        .setMaxLength(5)
                        .build();

                String powerUpsId = "power_ups";
                TextInput textPowerUps = TextInput.create(powerUpsId, getString("probabilities_powerups"), TextInputStyle.SHORT)
                        .setValue(StringUtil.doubleToString(fishery.getPowerUpProbabilityInPercentEffectively(), 2, getLocale()))
                        .setMinLength(1)
                        .setMaxLength(5)
                        .build();

                Modal modal = ModalMediator.createDrawableCommandModal(this, getString("probabilities_title"), e -> {
                            String treasureChestsProbabilityStr = e.getValue(treasureChestsId).getAsString().replace(",", ".");
                            if (!StringUtil.stringIsDouble(treasureChestsProbabilityStr)) {
                                setLog(LogStatus.FAILURE, getString("probabilities_invalid_treasure", StringUtil.escapeMarkdown(treasureChestsProbabilityStr)));
                                return null;
                            }

                            String powerUpsProbabilityStr = e.getValue(powerUpsId).getAsString().replace(",", ".");
                            if (!StringUtil.stringIsDouble(powerUpsProbabilityStr)) {
                                setLog(LogStatus.FAILURE, getString("probabilities_invalid_powerups", StringUtil.escapeMarkdown(powerUpsProbabilityStr)));
                                return null;
                            }

                            double treasureChestsProbability = Double.parseDouble(treasureChestsProbabilityStr);
                            if (treasureChestsProbability < 0 || treasureChestsProbability > 100) {
                                setLog(LogStatus.FAILURE, getString("probabilities_outofrange_treasure"));
                                return null;
                            }

                            double powerUpsProbability = Double.parseDouble(powerUpsProbabilityStr);
                            if (powerUpsProbability < 0 || powerUpsProbability > 100) {
                                setLog(LogStatus.FAILURE, getString("probabilities_outofrange_powerup"));
                                return null;
                            }

                            FisheryEntity newFishery = getGuildEntity().getFishery();
                            newFishery.beginTransaction();

                            double newTreasureChestProbability = NumberUtil.trimDecimalPositions(treasureChestsProbability, 2);
                            BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_TREASURE_CHEST_PROBABILITY, e.getMember(),
                                    newFishery.getTreasureChestProbabilityInPercent(), newTreasureChestProbability
                            );

                            double newPowerUpProbability = NumberUtil.trimDecimalPositions(powerUpsProbability, 2);
                            BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_POWER_UP_PROBABILITY, e.getMember(),
                                    newFishery.getPowerUpProbabilityInPercent(), newPowerUpProbability
                            );


                            newFishery.setTreasureChestProbabilityInPercent(newTreasureChestProbability);
                            newFishery.setPowerUpProbabilityInPercent(newPowerUpProbability);
                            newFishery.commitTransaction();

                            setLog(LogStatus.SUCCESS, getString("probabilitiesset"));
                            return null;
                        }).addComponents(ActionRow.of(textTreasureChests), ActionRow.of(textPowerUps))
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case 6 -> {
                if (!ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
                    return true;
                }

                Modal modal = new DurationModalBuilder(this, getString("dashboard_workinterval"))
                        .setLogEvent(BotLogEntity.Event.FISHERY_WORK_INTERVAL)
                        .setGetter(() -> getGuildEntity().getFishery().getWorkIntervalMinutesEffectively())
                        .setSetter(minutes -> getGuildEntity().getFishery().setWorkIntervalMinutes(minutes))
                        .build();
                event.replyModal(modal).queue();
                return false;
            }
            case 7 -> {
                if (!ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
                    return true;
                }

                Modal modal = new IntModalBuilder(this, getString("dashboard_chestlimit"))
                        .setMinMax(1, MAX_WEEKLY_TREASURE_CHEST_LIMIT)
                        .setLogEvent(BotLogEntity.Event.FISHERY_WEEKLY_TREASURE_CHEST_LIMIT)
                        .setGetter(() -> getGuildEntity().getFishery().getWeeklyTreasureChestUserLimitEffectively())
                        .setSetter(value -> getGuildEntity().getFishery().setWeeklyTreasureChestUserLimit(value))
                        .build();
                event.replyModal(modal).queue();
                return false;
            }
            case 8 -> {
                setState(STATE_SET_EXCLUDED_CHANNELS);
                stopLock = true;
                return true;
            }
            case 9 -> {
                if (fishery.getFisheryStatus() != FisheryStatus.ACTIVE) {
                    fishery.beginTransaction();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_STATUS, event.getMember(), fishery.getFisheryStatus(), FisheryStatus.ACTIVE);
                    fishery.setFisheryStatus(FisheryStatus.ACTIVE);
                    fishery.commitTransaction();
                } else {
                    fishery.beginTransaction();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_STATUS, event.getMember(), fishery.getFisheryStatus(), FisheryStatus.PAUSED);
                    fishery.setFisheryStatus(FisheryStatus.PAUSED);
                    fishery.commitTransaction();
                }

                setLog(LogStatus.SUCCESS, getString("setstatus"));
                stopLock = true;
                return true;
            }
            case 10 -> {
                if (fishery.getFisheryStatus() == FisheryStatus.ACTIVE) {
                    if (stopLock) {
                        stopLock = false;
                        setLog(LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "confirm_warning_button"));
                    } else {
                        GlobalThreadPool.submit(() -> FisheryUserManager.deleteGuildData(event.getGuild().getIdLong()));

                        fishery.beginTransaction();
                        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_STATUS, event.getMember(), fishery.getFisheryStatus(), FisheryStatus.STOPPED);
                        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_DATA_RESET, event.getMember());
                        fishery.setFisheryStatus(FisheryStatus.STOPPED);
                        fishery.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("setstatus"));
                        stopLock = true;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) {
        FisheryEntity fishery = getGuildEntity().getFishery();

        String[] options = getString("state0_options_" + fishery.getFisheryStatus().ordinal()).split("\n");
        setComponents(options, fishery.getFisheryStatus() == FisheryStatus.ACTIVE ? null : Set.of(9), Set.of(10));

        GuildMessageChannel channel = getGuildMessageChannel().get();
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                .addField(getString("state0_mstatus"), "**" + getString("state0_status").split("\n")[fishery.getFisheryStatus().ordinal()] + "**\n" + Emojis.ZERO_WIDTH_SPACE.getFormatted(), false)
                .addField(getString("state0_mtreasurechests_title", StringUtil.getEmojiForBoolean(channel, fishery.getTreasureChests()).getFormatted()), "-# " + getString("state0_mtreasurechests_desc"), true)
                .addField(getString("state0_mpowerups_title", StringUtil.getEmojiForBoolean(channel, fishery.getPowerUps()).getFormatted()), "-# " + getString("state0_mpowerups_desc"), true)
                .addField(getString("state0_mreminders_title", StringUtil.getEmojiForBoolean(channel, fishery.getFishReminders()).getFormatted()), "-# " + getString("state0_mreminders_desc"), true)
                .addField(getString("state0_mcoinsgivenlimit_title", StringUtil.getEmojiForBoolean(channel, fishery.getCoinGiftLimit()).getFormatted()), "-# " + getString("state0_mcoinsgivenlimit_desc") + "\n" + Emojis.ZERO_WIDTH_SPACE.getFormatted(), true)
                .addField(getString("state0_mcards_title", StringUtil.getEmojiForBoolean(channel, fishery.getGraphicallyGeneratedAccountCardsEffectively()).getFormatted(), Emojis.COMMAND_ICON_PREMIUM.getFormatted()), "-# " + getString("state0_mcards_desc") + "\n" + Emojis.ZERO_WIDTH_SPACE.getFormatted(), true)
                .addField(getString("state0_mprobs", Emojis.COMMAND_ICON_PREMIUM.getFormatted()), generateProbabilitiesTextValue(fishery) + "\n" + Emojis.ZERO_WIDTH_SPACE.getFormatted(), false)
                .addField(getString("state0_mworkinterval", Emojis.COMMAND_ICON_PREMIUM.getFormatted()), TimeUtil.getDurationString(getLocale(), Duration.ofMinutes(fishery.getWorkIntervalMinutesEffectively())), true)
                .addField(getString("state0_mchestlimit", Emojis.COMMAND_ICON_PREMIUM.getFormatted()), StringUtil.numToString(fishery.getWeeklyTreasureChestUserLimitEffectively()), true)
                .addField(getString("state0_mchannels"), new ListGen<AtomicGuildChannel>().getList(fishery.getExcludedChannels(), getLocale(), m -> m.getPrefixedNameInField(getLocale())), true);
    }

    @Override
    public void onStaticButton(ButtonInteractionEvent event, String secondaryId) {
        if (event.getComponent().getId().equals(BUTTON_ID_TREASURE)) {
            processTreasureChest(event);
        } else if (event.getComponent().getId().equals(BUTTON_ID_POWERUP)) {
            if (event.getUser().getId().equals(secondaryId)) {
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

    @Override
    public void onStaticEntitySelectMenu(EntitySelectInteractionEvent event, String secondaryId) {
        if (!event.getComponent().getId().equals(ENTITY_SELECT_MENU_ID_COLLABORATION)) {
            return;
        }

        if (!event.getUser().getId().equals(secondaryId)) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "rejected"))
                    .setDescription(getString("powerup_notforyou"));
            event.replyEmbeds(eb.build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (event.getUser().getIdLong() == event.getMentions().getUsers().get(0).getIdLong()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "rejected"))
                    .setDescription(getString("powerup_collab_yourself"));
            event.replyEmbeds(eb.build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (event.getMentions().getUsers().get(0).isBot()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "rejected"))
                    .setDescription(getString("powerup_collab_bot"));
            event.replyEmbeds(eb.build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (event.getMentions().getMembers().isEmpty()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "rejected"))
                    .setDescription(getString("powerup_collab_notpresent"));
            event.replyEmbeds(eb.build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong())
                .remove(event.getMessage().getIdLong());
        processCollaboration(event);
    }

    private void processTreasureChest(ButtonInteractionEvent event) {
        FisheryMemberData memberData = FisheryUserManager.getGuildData(event.getGuild().getIdLong())
                .getMemberData(event.getMember().getIdLong());

        int weeklyOpenedTreasureChests = memberData.getWeeklyOpenedTreasureChests();
        int weeklyTreasureChestUserLimit = getGuildEntity().getFishery().getWeeklyTreasureChestUserLimitEffectively();
        if (weeklyOpenedTreasureChests >= weeklyTreasureChestUserLimit) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "wrong_args"))
                    .setDescription(TextManager.getString(getLocale(), Category.FISHERY_SETTINGS, "fishery_treasure_limitreached", StringUtil.numToString(weeklyOpenedTreasureChests), StringUtil.numToString(weeklyTreasureChestUserLimit)));
            event.replyEmbeds(eb.build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong())
                .remove(event.getMessage().getIdLong());
        memberData.increaseWeeklyOpenedTreasureChests();

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(FisheryCommand.EMOJI_TREASURE + " " + TextManager.getString(getLocale(), Category.FISHERY_SETTINGS, "fishery_treasure_title"))
                .setDescription(TextManager.getString(getLocale(), Category.FISHERY_SETTINGS, "fishery_treasure_opening", event.getMember().getEffectiveName()));

        event.editMessageEmbeds(eb.build())
                .setComponents()
                .queue();

        schedule(Duration.ofSeconds(3), () -> processTreasureChestReveal(event, memberData));
    }

    private void processTreasureChestReveal(ButtonInteractionEvent event, FisheryMemberData memberData) {
        InteractionHook hook = event.getHook();

        Random r = new Random();
        String[] winLose = new String[]{"win", "lose"};
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

        GuildMessageChannel channel = event.getGuildChannel();
        if (resultInt == 0 && BotPermissionUtil.canWriteEmbed(channel)) {
            event.getMessage().editMessageEmbeds(eb.build(), memberData.changeValuesEmbed(event.getMember(), 0, won, getGuildEntity()).build()).submit()
                    .exceptionally(ExceptionLogger.get(ExceptionIds.UNKNOWN_MESSAGE, ExceptionIds.UNKNOWN_CHANNEL));
        } else {
            hook.editOriginalEmbeds(eb.build()).submit()
                    .exceptionally(ExceptionLogger.get(ExceptionIds.UNKNOWN_MESSAGE, ExceptionIds.UNKNOWN_CHANNEL));
        }

        schedule(Duration.ofMinutes(Settings.FISHERY_DESPAWN_MINUTES), () -> {
            if (BotPermissionUtil.can(channel, Permission.VIEW_CHANNEL)) {
                hook.deleteOriginal().submit()
                        .exceptionally(ExceptionLogger.get(ExceptionIds.UNKNOWN_MESSAGE, ExceptionIds.UNKNOWN_CHANNEL));
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

        schedule(Duration.ofSeconds(3), () -> processPowerUpReveal(event));
    }

    private void processPowerUpReveal(ButtonInteractionEvent event) {
        Random r = new Random();
        InteractionHook hook = event.getHook();
        FisheryMemberData memberData = FisheryUserManager.getGuildData(event.getGuild().getIdLong())
                .getMemberData(event.getMember().getIdLong());

        ArrayList<FisheryPowerUp> possiblePowerUps = new ArrayList<>(List.of(FisheryPowerUp.values()));
        possiblePowerUps.removeAll(memberData.getActivePowerUps());
        FisheryPowerUp powerUp = possiblePowerUps.get(r.nextInt(possiblePowerUps.size()));
        Instant expiration = Instant.now().plus(powerUp.getValidDuration());

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(FisheryCommand.EMOJI_POWERUP + " " + getString("powerup", powerUp.ordinal()))
                .setDescription(getString("powerup_description_" + powerUp.ordinal()))
                .setThumbnail(powerUp.getImageUrl())
                .setFooter(getString("powerup_footer"));

        if (!powerUp.getValidDuration().isZero()) {
            eb = eb.addField(
                    Emojis.ZERO_WIDTH_SPACE.getFormatted(),
                    getString("powerup_expires", TimeFormat.DATE_TIME_SHORT.atInstant(expiration).toString()),
                    false
            );
        }

        GuildMessageChannel channel = event.getGuildChannel();
        if (powerUp == FisheryPowerUp.TEAM) {
            EntitySelectMenu memberSelectMenu = EntitySelectMenu.create(ENTITY_SELECT_MENU_ID_COLLABORATION, EntitySelectMenu.SelectTarget.USER)
                    .setRequiredRange(1, 1)
                    .setPlaceholder(getString("powerup_collab_memberselect"))
                    .build();
            hook.editOriginalEmbeds(eb.build())
                    .setActionRow(memberSelectMenu)
                    .submit()
                    .exceptionally(ExceptionLogger.get(ExceptionIds.UNKNOWN_MESSAGE, ExceptionIds.UNKNOWN_CHANNEL));
            registerStaticReactionMessage(event.getMessage(), event.getUser().getId());
        } else {
            if (powerUp == FisheryPowerUp.SHOP) {
                memberData.setCoupons(3);
            }
            hook.editOriginalEmbeds(eb.build()).submit()
                    .exceptionally(ExceptionLogger.get(ExceptionIds.UNKNOWN_MESSAGE, ExceptionIds.UNKNOWN_CHANNEL));
        }
        memberData.activatePowerUp(powerUp, expiration);

        int despawnMinutes = powerUp == FisheryPowerUp.TEAM ? Settings.FISHERY_POWERUP_TIMEOUT_MINUTES : Settings.FISHERY_DESPAWN_MINUTES;
        schedule(Duration.ofMinutes(despawnMinutes), () -> {
            if (BotPermissionUtil.can(channel, Permission.VIEW_CHANNEL)) {
                hook.deleteOriginal().submit()
                        .exceptionally(ExceptionLogger.get(ExceptionIds.UNKNOWN_MESSAGE, ExceptionIds.UNKNOWN_CHANNEL));
            }
        });
    }

    private void processCollaboration(EntitySelectInteractionEvent event) {
        Member member0 = event.getMember();
        Member member1 = event.getMentions().getMembers().get(0);

        FisheryGuildData fisheryGuildData = FisheryUserManager.getGuildData(event.getGuild().getIdLong());
        FisheryMemberData memberData0 = fisheryGuildData.getMemberData(member0.getIdLong());
        FisheryMemberData memberData1 = fisheryGuildData.getMemberData(member1.getIdLong());

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(FisheryCommand.EMOJI_POWERUP + " " + getString("powerup", FisheryPowerUp.TEAM.ordinal()))
                .setDescription(getString("powerup_collab_result", member0.getEffectiveName(), member1.getEffectiveName()))
                .setThumbnail(FisheryPowerUp.TEAM.getImageUrl())
                .setFooter(getString("powerup_footer"));

        EmbedBuilder changeEmbed0 = memberData0.changeValuesEmbed(member0, 0, memberData0.getMemberGear(FisheryGear.TREASURE).getEffect(), getGuildEntity());
        EmbedBuilder changeEmbed1 = memberData1.changeValuesEmbed(member1, 0, memberData1.getMemberGear(FisheryGear.TREASURE).getEffect(), getGuildEntity());

        event.editMessageEmbeds(eb.build(), changeEmbed0.build(), changeEmbed1.build())
                .setComponents()
                .queue();
    }

    private String generateProbabilitiesTextValue(FisheryEntity fishery) {
        return getString("state0_probs",
                StringUtil.doubleToString(fishery.getTreasureChestProbabilityInPercentEffectively(), 2, getLocale()),
                StringUtil.doubleToString(fishery.getPowerUpProbabilityInPercentEffectively(), 2, getLocale())
        );
    }

}
