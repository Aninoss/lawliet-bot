package commands.runnables.fisherysettingscategory;

import commands.Category;
import commands.CommandEvent;
import commands.NavigationHelper;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.listeners.OnStaticButtonListener;
import commands.listeners.OnStaticEntitySelectMenuListener;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.ExceptionIds;
import constants.LogStatus;
import constants.Settings;
import core.*;
import core.atomicassets.AtomicTextChannel;
import core.cache.ServerPatreonBoostCache;
import core.modals.ModalMediator;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.NumberUtil;
import core.utils.StringUtil;
import modules.fishery.Fishery;
import modules.fishery.FisheryGear;
import modules.fishery.FisheryPowerUp;
import modules.fishery.FisheryStatus;
import mysql.hibernate.entity.guild.FisheryEntity;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.redis.fisheryusers.FisheryGuildData;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

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

    public static final int MAX_CHANNELS = 50;

    public static final String BUTTON_ID_TREASURE = "open";
    public static final String BUTTON_ID_POWERUP = "use";
    public static final String ENTITY_SELECT_MENU_ID_COLLABORATION = "member";

    private boolean stopLock = true;
    private NavigationHelper<AtomicTextChannel> channelNavigationHelper;

    public static final String EMOJI_TREASURE = "💰";
    public static final String EMOJI_KEY = "🔑";
    public static final String EMOJI_POWERUP = "❔";

    public FisheryCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        channelNavigationHelper = new NavigationHelper<>(this, guildEntity -> guildEntity.getFishery().getExcludedChannels(), AtomicTextChannel.class, MAX_CHANNELS);
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
        FisheryEntity fishery = getGuildEntity().getFishery();

        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        deregisterListenersWithComponentMessage();
                        return false;

                    case 0:
                        fishery.beginTransaction();
                        fishery.setTreasureChests(!fishery.getTreasureChests());
                        fishery.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("treasurechestsset", fishery.getTreasureChests()));
                        stopLock = true;
                        return true;

                    case 1:
                        fishery.beginTransaction();
                        fishery.setPowerUps(!fishery.getPowerUps());
                        fishery.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("powerupsset", fishery.getPowerUps()));
                        stopLock = true;
                        return true;

                    case 2:
                        fishery.beginTransaction();
                        fishery.setFishReminders(!fishery.getFishReminders());
                        fishery.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("remindersset", fishery.getFishReminders()));
                        stopLock = true;
                        return true;

                    case 3:
                        fishery.beginTransaction();
                        fishery.setCoinGiftLimit(!fishery.getCoinGiftLimit());
                        fishery.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("coinsgivenset", fishery.getCoinGiftLimit()));
                        stopLock = true;
                        return true;

                    case 4:
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
                                    newFishery.setTreasureChestProbabilityInPercent(NumberUtil.trimDecimalPositions(treasureChestsProbability, 2));
                                    newFishery.setPowerUpProbabilityInPercent(NumberUtil.trimDecimalPositions(powerUpsProbability, 2));
                                    newFishery.commitTransaction();

                                    setLog(LogStatus.SUCCESS, getString("probabilitiesset"));
                                    return null;
                                }).addActionRows(ActionRow.of(textTreasureChests), ActionRow.of(textPowerUps))
                                .build();

                        event.replyModal(modal).queue();
                        return false;

                    case 5:
                        channelNavigationHelper.startDataAdd(1);
                        stopLock = true;
                        return true;

                    case 6:
                        channelNavigationHelper.startDataRemove(2);
                        stopLock = true;
                        return true;

                    case 7:
                        fishery.beginTransaction();
                        if (fishery.getFisheryStatus() != FisheryStatus.ACTIVE) {
                            fishery.setFisheryStatus(FisheryStatus.ACTIVE);
                        } else {
                            fishery.setFisheryStatus(FisheryStatus.PAUSED);
                        }
                        fishery.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("setstatus"));
                        stopLock = true;
                        return true;

                    case 8:
                        if (fishery.getFisheryStatus() == FisheryStatus.ACTIVE) {
                            if (stopLock) {
                                stopLock = false;
                                setLog(LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "confirm_warning_button"));
                            } else {
                                GlobalThreadPool.submit(() -> FisheryUserManager.deleteGuildData(event.getGuild().getIdLong()));

                                fishery.beginTransaction();
                                fishery.setFisheryStatus(FisheryStatus.STOPPED);
                                fishery.commitTransaction();

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
                FisheryEntity fishery = getGuildEntity().getFishery();

                String[] options = getString("state0_options_" + fishery.getFisheryStatus().ordinal()).split("\n");
                Button[] buttons = new Button[options.length];
                for (int i = 0; i < options.length; i++) {
                    buttons[i] = Button.of(
                            i == 7 ? ButtonStyle.DANGER : ButtonStyle.PRIMARY,
                            String.valueOf(i),
                            options[i]
                    );
                }
                setComponents(buttons);

                TextChannel channel = getTextChannel().get();
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_mstatus"), "**" + getString("state0_status").split("\n")[fishery.getFisheryStatus().ordinal()] + "**\n" + Emojis.ZERO_WIDTH_SPACE.getFormatted(), false)
                        .addField(getString("state0_mtreasurechests_title", StringUtil.getEmojiForBoolean(channel, fishery.getTreasureChests()).getFormatted()), getString("state0_mtreasurechests_desc"), true)
                        .addField(getString("state0_mpowerups_title", StringUtil.getEmojiForBoolean(channel, fishery.getPowerUps()).getFormatted()), getString("state0_mpowerups_desc"), true)
                        .addField(getString("state0_mreminders_title", StringUtil.getEmojiForBoolean(channel, fishery.getFishReminders()).getFormatted()), getString("state0_mreminders_desc"), true)
                        .addField(getString("state0_mcoinsgivenlimit_title", StringUtil.getEmojiForBoolean(channel, fishery.getCoinGiftLimit()).getFormatted()), getString("state0_mcoinsgivenlimit_desc") + "\n" + Emojis.ZERO_WIDTH_SPACE.getFormatted(), true)
                        .addField(getString("state0_mprobs", Emojis.COMMAND_ICON_PREMIUM.getFormatted()), generateProbabilitiesTextValue(fishery) + "\n" + Emojis.ZERO_WIDTH_SPACE.getFormatted(), false)
                        .addField(getString("state0_mchannels"), new ListGen<AtomicTextChannel>().getList(fishery.getExcludedChannels(), getLocale(), m -> m.getPrefixedNameInField(getLocale())), false);

            case 1:
                return channelNavigationHelper.drawDataAdd(getString("state1_title"), getString("state1_description"));
            case 2:
                return channelNavigationHelper.drawDataRemove(getLocale());

            default:
                return null;
        }
    }

    @Override
    public void onStaticButton(ButtonInteractionEvent event, String secondaryId) {
        if (!(event.getChannel() instanceof TextChannel)) {
            return;
        }

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

    private void processTreasureChest(ButtonInteractionEvent event) {
        DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong())
                .remove(event.getMessage().getIdLong());

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(FisheryCommand.EMOJI_TREASURE + " " + TextManager.getString(getLocale(), Category.FISHERY_SETTINGS, "fishery_treasure_title"))
                .setDescription(TextManager.getString(getLocale(), Category.FISHERY_SETTINGS, "fishery_treasure_opening", event.getMember().getEffectiveName()));

        event.editMessageEmbeds(eb.build())
                .setComponents()
                .queue();

        schedule(Duration.ofSeconds(3), () -> processTreasureChestReveal(event));
    }

    private void processTreasureChestReveal(ButtonInteractionEvent event) {
        InteractionHook hook = event.getHook();
        FisheryMemberData memberData = FisheryUserManager.getGuildData(event.getGuild().getIdLong())
                .getMemberData(event.getMember().getIdLong());

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

        StandardGuildMessageChannel channel = (StandardGuildMessageChannel) event.getChannel();
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

        StandardGuildMessageChannel channel = (StandardGuildMessageChannel) event.getChannel();
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

    @Override
    public void onStaticEntitySelectMenu(EntitySelectInteractionEvent event, String secondaryId) {
        if (!(event.getChannel() instanceof TextChannel) ||
                !event.getComponent().getId().equals(ENTITY_SELECT_MENU_ID_COLLABORATION)
        ) {
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

        DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong())
                .remove(event.getMessage().getIdLong());
        processCollaboration(event);
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
