package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnReactionListener;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.EmojiStateProcessor;
import commands.stateprocessor.FileStateProcessor;
import commands.stateprocessor.RolesStateProcessor;
import commands.stateprocessor.StringStateProcessor;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.LocalFile;
import core.TextManager;
import core.atomicassets.AtomicGuildMessageChannel;
import core.atomicassets.AtomicRole;
import core.modals.DurationModalBuilder;
import core.modals.IntModalBuilder;
import core.modals.StringModalBuilder;
import core.utils.*;
import modules.Giveaway;
import modules.schedulers.GiveawayScheduler;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.GiveawayEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "giveaway",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        userGuildPermissions = Permission.MANAGE_SERVER,
        botGuildPermissions = Permission.MANAGE_ROLES,
        emoji = "🎆",
        releaseDate = {2020, 10, 28},
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = {"giveaways"}
)
public class GiveawayCommand extends NavigationAbstract implements OnReactionListener {

    public final static int ITEM_LENGTH_MAX = MessageEmbed.TITLE_MAX_LENGTH;
    public final static int DESC_LENGTH_MAX = MessageEmbed.VALUE_MAX_LENGTH;
    public final static int WINNERS_MIN = 1;
    public final static int WINNERS_MAX = 20;
    public final static int MAX_ROLE_PRIZES = 10;

    private final static int
            STATE_SET_CHANNEL = 1,
            STATE_EDIT_SELECT_GIVEAWAY = 2,
            STATE_REROLL_SELECT_GIVEAWAY = 3,
            STATE_CONFIG = 4,
            STATE_SET_DESC = 5,
            STATE_SET_EMOJI = 6,
            STATE_SET_IMAGE = 7,
            STATE_SET_ROLE_PRIZES = 10,
            STATE_EXAMPLE = 8,
            STATE_REROLL = 9;

    private GiveawayEntity config;
    private String previousItem;
    private boolean editMode = false;
    private int rerollWinners;
    private EmojiStateProcessor emojiStateProcessor;

    public GiveawayCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        resetGiveawayConfiguration(event.getGuild().getIdLong());
        emojiStateProcessor = new EmojiStateProcessor(this, STATE_SET_EMOJI, STATE_CONFIG, getString("state3_memoji"))
                .setClearButton(false)
                .setGetter(() -> config.getEmoji())
                .setSetter(emoji -> config.setEmoji(emoji));

        registerNavigationListener(event.getMember(), List.of(
                new StringStateProcessor(this, STATE_SET_DESC, STATE_CONFIG, getString("state3_mdescription"))
                        .setClearButton(true)
                        .setMax(DESC_LENGTH_MAX)
                        .setGetter(() -> config.getDescription())
                        .setSetter(value -> config.setDescription(value)),
                new FileStateProcessor(this, STATE_SET_IMAGE, STATE_CONFIG, getString("dashboard_includedimage"))
                        .setClearButton(true)
                        .setAllowGifs(true)
                        .setGetter(() -> config.getImageFilename())
                        .setSetter(attachment -> {
                            if (attachment != null) {
                                LocalFile tempFile = new LocalFile(LocalFile.Directory.CDN, String.format("giveaway/%s.%s", RandomUtil.generateRandomString(30), attachment.getFileExtension()));
                                FileUtil.downloadImageAttachment(attachment, tempFile);
                                config.setImageFilename(tempFile.getName());
                            } else {
                                config.setImageFilename(null);
                            }
                        }),
                emojiStateProcessor,
                new RolesStateProcessor(this, STATE_SET_ROLE_PRIZES, STATE_CONFIG, getString("state3_mprizeroles"))
                        .setCheckAccess(true)
                        .setMinMax(0, MAX_ROLE_PRIZES)
                        .setDescription(getString("state10_description"))
                        .setGetter(() -> config.getPrizeRoleIds())
                        .setSetter(roleIds -> config.setPrizeRoleIds(roleIds))
        ));
        registerReactionListener(event.getMember());
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonAddOrEdit(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                deregisterListenersWithComponentMessage();
                return false;

            case 0:
                setState(STATE_SET_CHANNEL);
                editMode = false;
                return true;

            case 1:
                if (!getActiveGiveaways().isEmpty()) {
                    setState(STATE_EDIT_SELECT_GIVEAWAY);
                    editMode = true;
                } else {
                    setLog(LogStatus.FAILURE, getString("nothing"));
                }
                return true;

            case 2:
                if (!getCompletedGiveaways().isEmpty()) {
                    setState(STATE_REROLL_SELECT_GIVEAWAY);
                } else {
                    setLog(LogStatus.FAILURE, getString("nothing_completed"));
                }
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = STATE_SET_CHANNEL)
    public boolean onButtonAddMessage(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }
        return false;
    }

    @ControllerButton(state = STATE_EDIT_SELECT_GIVEAWAY)
    public boolean onButtonEditMessage(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }

        List<GiveawayEntity> giveaways = getActiveGiveaways();
        if (i >= 0 && i < giveaways.size()) {
            config = giveaways.get(i).copy();
            previousItem = config.getItem();
            setState(STATE_CONFIG);
            return true;
        }

        return false;
    }

    @ControllerButton(state = STATE_REROLL_SELECT_GIVEAWAY)
    public boolean onButtonRerollMessage(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }

        List<GiveawayEntity> giveaways = getCompletedGiveaways();
        if (i >= 0 && i < giveaways.size()) {
            config = giveaways.get(i).copy();
            rerollWinners = config.getWinners();
            setState(STATE_REROLL);
            return true;
        }

        return false;
    }

    @ControllerButton(state = STATE_CONFIG)
    public boolean onButtonConfigureMessage(ButtonInteractionEvent event, int i) throws InterruptedException {
        switch (i) {
            case -1:
                if (!editMode) {
                    setState(STATE_SET_CHANNEL);
                } else {
                    resetGiveawayConfiguration(event.getGuild().getIdLong());
                    setState(STATE_EDIT_SELECT_GIVEAWAY);
                }
                return true;

            case 0:
                Modal modal = new StringModalBuilder(this, getString("state3_mtitle"), TextInputStyle.SHORT)
                        .setMinMaxLength(1, ITEM_LENGTH_MAX)
                        .setGetter(() -> config.getItem())
                        .setSetter(value -> config.setItem(value))
                        .build();
                event.replyModal(modal).queue();
                return false;

            case 1:
                setState(STATE_SET_DESC);
                return true;

            case 2:
                if (editMode) {
                    setLog(LogStatus.FAILURE, getString("locked"));
                    return true;
                }

                modal = new DurationModalBuilder(this, getString("state3_mduration"))
                        .setMinMinutes(1)
                        .setGetterInt(() -> config.getDurationMinutes())
                        .setSetterInt(value -> config.setDurationMinutes(value))
                        .build();
                event.replyModal(modal).queue();
                return false;

            case 3:
                modal = new IntModalBuilder(this, getString("state3_mwinners"))
                        .setMinMax(1, WINNERS_MAX)
                        .setGetter(() -> config.getWinners())
                        .setSetter(value -> config.setWinners(value))
                        .build();
                event.replyModal(modal).queue();
                return false;

            case 4:
                if (!editMode) {
                    setState(STATE_SET_EMOJI);
                } else {
                    setLog(LogStatus.FAILURE, getString("locked"));
                }
                return true;

            case 5:
                setState(STATE_SET_IMAGE);
                return true;

            case 6:
                setState(STATE_SET_ROLE_PRIZES);
                return true;

            case 7:
                if (config.getItem().isEmpty()) {
                    setLog(LogStatus.FAILURE, getString("noitem"));
                    return true;
                }

                setState(STATE_EXAMPLE);
                return true;

            case 8:
                if (config.getItem().isEmpty()) {
                    setLog(LogStatus.FAILURE, getString("noitem"));
                    return true;
                }

                submit(event.getMember(), true);
                return true;

            case 9:
                if (config.getItem().isEmpty()) {
                    setLog(LogStatus.FAILURE, getString("noitem"));
                    return true;
                }

                submit(event.getMember(), false);
                return true;

            default:
                return false;
        }
    }

    @Override
    public boolean onReaction(@NotNull GenericMessageReactionEvent event) {
        return emojiStateProcessor.handleReactionEvent(event);
    }

    @ControllerButton(state = STATE_REROLL)
    public boolean onButtonRerollNumber(ButtonInteractionEvent event, int i) {
        Map<Long, GiveawayEntity> giveaways = getGuildEntity().getGiveaways();

        switch (i) {
            case -1 -> {
                setState(STATE_REROLL_SELECT_GIVEAWAY);
                return true;
            }
            case 0 -> {
                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.GIVEAWAYS_REMOVE, event.getMember(), config.getItem());
                giveaways.remove(config.getMessageId());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("removed", config.getItem()));
                setState(getCompletedGiveaways().isEmpty() ? DEFAULT_STATE : STATE_REROLL_SELECT_GIVEAWAY);
                return true;
            }
            case 1 -> {
                Modal modal = new IntModalBuilder(this, getString("state3_mwinners"))
                        .setMinMax(WINNERS_MIN, WINNERS_MAX)
                        .setGetter(() -> rerollWinners)
                        .setSetter(value -> rerollWinners = value)
                        .build();
                event.replyModal(modal).queue();
                return false;
            }
            case 2 -> {
                if (GiveawayScheduler.processGiveawayUsers(config, getLocale(), rerollWinners, true)) {
                    getEntityManager().getTransaction().begin();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.GIVEAWAYS_REROLL, event.getMember(), config.getItem());
                    getEntityManager().getTransaction().commit();

                    setLog(LogStatus.SUCCESS, getString("rerollset", config.getItem()));
                } else {
                    setLog(LogStatus.FAILURE, getString("error"));
                }
                setState(STATE_REROLL_SELECT_GIVEAWAY);
                return true;
            }
        }
        return false;
    }

    @ControllerButton
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(STATE_CONFIG);
            return true;
        }
        return false;
    }

    @ControllerEntitySelectMenu(state = STATE_SET_CHANNEL)
    public boolean onSelectMenuAddMessage(EntitySelectInteractionEvent event) {
        GuildMessageChannel channel = (GuildMessageChannel) event.getMentions().getChannels().get(0);
        if (checkWriteEmbedInChannelWithLog(channel)) {
            config.setChannelId(channel.getIdLong());
            setState(STATE_CONFIG);
        }
        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawAddOrEdit(Member member) {
        setComponents(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"));
    }

    @Draw(state = STATE_SET_CHANNEL)
    public EmbedBuilder onDrawAddMessage(Member member) {
        EntitySelectMenu channelSelectMenu = EntitySelectMenu.create("select_channel", EntitySelectMenu.SelectTarget.CHANNEL)
                .setChannelTypes(JDAUtil.GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES)
                .setRequiredRange(1, 1)
                .build();
        setComponents(channelSelectMenu);
        return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));
    }

    @Draw(state = STATE_EDIT_SELECT_GIVEAWAY)
    public EmbedBuilder onDrawEditMessage(Member member) {
        String[] options = getActiveGiveaways().stream()
                .map(giveaway -> {
                    AtomicGuildMessageChannel atomicGuildMessageChannel = new AtomicGuildMessageChannel(giveaway.getGuildId(), giveaway.getChannelId());
                    return getString("state2_slot", giveaway.getItem(), atomicGuildMessageChannel.getName(getLocale()));
                })
                .toArray(String[]::new);
        setComponents(options);
        return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));
    }

    @Draw(state = STATE_REROLL_SELECT_GIVEAWAY)
    public EmbedBuilder onDrawRerollMessage(Member member) {
        String[] options = getCompletedGiveaways().stream()
                .map(giveawayData -> getString("state2_slot", giveawayData.getItem(), new AtomicGuildMessageChannel(member.getGuild().getIdLong(), giveawayData.getChannelId()).getName(getLocale())))
                .toArray(String[]::new);
        setComponents(options);
        return EmbedFactory.getEmbedDefault(this, getString("state12_description"), getString("state12_title"));
    }

    @Draw(state = STATE_CONFIG)
    public EmbedBuilder onDrawConfigureMessage(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");

        String[] options = getString("state3_options").split("\n");
        if (!editMode) {
            options[8] = "";
        }
        setComponents(options, Set.of(8, 9));

        return EmbedFactory.getEmbedDefault(this, getString("state3_description"), getString("state3_title_" + (editMode ? "edit" : "new")))
                .addField(getString("state3_mtitle"), StringUtil.escapeMarkdown(config.getItem().isEmpty() ? notSet : config.getItem()), false)
                .addField(getString("state3_mdescription"), StringUtil.escapeMarkdown(config.getDescription() == null ? notSet : config.getDescription()), false)
                .addField(getString("state3_mduration"), TimeUtil.getDurationString(getLocale(), Duration.ofMinutes(config.getDurationMinutes())), true)
                .addField(getString("state3_mwinners"), String.valueOf(config.getWinners()), true)
                .addField(getString("state3_memoji"), config.getEmojiFormatted(), true)
                .addField(getString("state3_mimage"), StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), config.getImageFilename() != null), true)
                .addField(getString("state3_mprizeroles"), new ListGen<AtomicRole>().getList(config.getPrizeRoles(), getLocale(), m -> m.getPrefixedNameInField(getLocale())), true);
    }

    @Draw(state = STATE_EXAMPLE)
    public EmbedBuilder onDrawExample(Member member) {
        return Giveaway.getMessageEmbed(getLocale(), config, Instant.now());
    }

    @Draw(state = STATE_REROLL)
    public EmbedBuilder onDrawRerollNumber(Member member) {
        setComponents(getString("state13_options").split("\n"), Set.of(2), Set.of(0));
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(
                this,
                getString("state13_description", config.getItem(), StringUtil.numToString(rerollWinners)),
                getString("state13_title")
        );
        if (!config.getPrizeRoleIds().isEmpty()) {
            eb.addField(getString("state3_mprizeroles"), new ListGen<AtomicRole>().getList(config.getPrizeRoles(), getLocale(), m -> m.getPrefixedNameInField(getLocale())), true);
        }
        return eb;
    }

    private List<GiveawayEntity> getActiveGiveaways() {
        return getGuildEntity().getGiveaways().values().stream()
                .filter(GiveawayEntity::getActive)
                .collect(Collectors.toList());
    }

    private List<GiveawayEntity> getCompletedGiveaways() {
        return getGuildEntity().getGiveaways().values().stream()
                .filter(g -> !g.getActive())
                .collect(Collectors.toList());
    }

    private void resetGiveawayConfiguration(long guildId) {
        config = new GiveawayEntity();
        previousItem = config.getItem();
        config.setGuildId(guildId);
    }

    private void submit(Member member, boolean endPrematurely) throws InterruptedException {
        Map<Long, GiveawayEntity> giveaways = getGuildEntity().getGiveaways();

        if (editMode && (!giveaways.containsKey(config.getMessageId()) || !giveaways.get(config.getMessageId()).getActive())) {
            setLog(LogStatus.FAILURE, getString("dashboard_toolate"));
            setState(STATE_EDIT_SELECT_GIVEAWAY);
            return;
        }

        if (!editMode) {
            config.setCreated(Instant.now());
        }

        Long messageId = sendMessage();
        if (messageId == null) {
            if (editMode) {
                setLog(LogStatus.FAILURE, getString("nomessage"));
                setState(STATE_EDIT_SELECT_GIVEAWAY);
                return;
            } else {
                setLog(LogStatus.FAILURE, getString("error"));
                return;
            }
        }

        GiveawayEntity giveaway = config.copy();
        giveaway.setMessageId(messageId);
        if (endPrematurely) {
            giveaway.setDurationMinutes(0);
        }

        getEntityManager().getTransaction().begin();
        if (endPrematurely) {
            BotLogEntity.log(getEntityManager(), BotLogEntity.Event.GIVEAWAYS_END, member, previousItem);
        } else {
            if (editMode) {
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.GIVEAWAYS_EDIT, member, previousItem);
            } else {
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.GIVEAWAYS_ADD, member, config.getItem());
            }
        }

        boolean newGiveaway = giveaways.put(giveaway.getMessageId(), giveaway) == null;
        getEntityManager().getTransaction().commit();

        setLog(LogStatus.SUCCESS, getString("sent", giveaway.getItem()));
        setState(editMode && (!endPrematurely || getActiveGiveaways().size() > 1) ? STATE_EDIT_SELECT_GIVEAWAY : DEFAULT_STATE);
        if (endPrematurely || newGiveaway) {
            GiveawayScheduler.loadGiveaway(giveaway);
        }
        resetGiveawayConfiguration(giveaway.getGuildId());
    }

    private Long sendMessage() {
        Message message;
        GuildMessageChannel channel = getGuild().map(guild -> guild.getChannelById(GuildMessageChannel.class, config.getChannelId()))
                .orElse(null);

        if (channel == null || !checkWriteEmbedInChannelWithLog(channel)) {
            return null;
        }

        if (!editMode) {
            EmbedBuilder eb = Giveaway.getMessageEmbed(getLocale(), config);
            message = channel.sendMessageEmbeds(eb.build()).complete();
            if (BotPermissionUtil.canReadHistory(channel, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY)) {
                message.addReaction(config.getEmoji()).queue();
            }
            return message.getIdLong();
        } else {
            if (config.getCreated().plus(config.getDurationMinutes(), ChronoUnit.MINUTES).isBefore(Instant.now())) {
                return null;
            }
            EmbedBuilder eb = Giveaway.getMessageEmbed(getLocale(), config);
            try {
                channel.editMessageEmbedsById(config.getMessageId(), eb.build()).complete();
                return config.getMessageId();
            } catch (Throwable e) {
                //Ignore
            }

            return null;
        }
    }

}
