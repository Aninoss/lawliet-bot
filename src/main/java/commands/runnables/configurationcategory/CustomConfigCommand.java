package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnReactionListener;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.EmojiStateProcessor;
import commands.stateprocessor.FileStateProcessor;
import commands.stateprocessor.StringStateProcessor;
import constants.ExternalLinks;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.LocalFile;
import core.TextManager;
import core.cache.ServerPatreonBoostCache;
import core.modals.StringModalBuilder;
import core.utils.FileUtil;
import core.utils.RandomUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.CustomCommandEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@CommandProperties(
        trigger = "customconfig",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "🧩",
        executableWithoutArgs = true,
        aliases = {"customcommands"}
)
public class CustomConfigCommand extends NavigationAbstract implements OnReactionListener {

    public static final int MAX_COMMANDS_FREE = 5;
    public static final int MAX_COMMAND_TRIGGER_LENGTH = 25;
    public static final int MAX_COMMAND_TITLE_LENGTH = 200;
    public static final int MAX_TEXT_RESPONSE_LENGTH = MessageEmbed.VALUE_MAX_LENGTH;

    private final int
            STATE_CONFIG = 1,
            STATE_EDIT = 2,
            STATE_SET_EMOJI = 4,
            STATE_SET_TEXT_RESPONSE = 3,
            STATE_SET_IMAGE = 5;

    private EmojiStateProcessor emojiStateProcessor;
    private CustomCommandEntity config;
    private String trigger;
    private String oldTrigger;
    private boolean updateMode;
    private boolean deleteLock = true;

    public CustomConfigCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        emojiStateProcessor = new EmojiStateProcessor(this, STATE_SET_EMOJI, STATE_CONFIG, getString("add_emoji"))
                .setClearButton(true)
                .setGetter(() -> config.getEmoji())
                .setSetter(emoji -> config.setEmojiFormatted(emoji != null ? emoji.getFormatted() : null));

        registerNavigationListener(event.getMember(), List.of(
                emojiStateProcessor,
                new StringStateProcessor(this, STATE_SET_TEXT_RESPONSE, STATE_CONFIG, getString("add_textresponse"))
                        .setMax(MAX_TEXT_RESPONSE_LENGTH)
                        .setClearButton(false)
                        .setGetter(() -> config.getTextResponse())
                        .setSetter(input -> config.setTextResponse(input)),
                new FileStateProcessor(this, STATE_SET_IMAGE, STATE_CONFIG, getString("add_image"))
                        .setAllowGifs(true)
                        .setClearButton(true)
                        .setGetter(() -> config.getImageFilename())
                        .setSetter(attachment -> {
                            if (attachment != null) {
                                LocalFile tempFile = new LocalFile(LocalFile.Directory.CDN, String.format("custom/%s.%s", RandomUtil.generateRandomString(30), attachment.getFileExtension()));
                                FileUtil.downloadImageAttachment(attachment, tempFile);
                                config.setImageUrl(tempFile.cdnGetUrl());
                            } else {
                                config.setImageFilename(null);
                            }
                        })
        ));
        registerReactionListener(event.getMember());
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                if (ServerPatreonBoostCache.get(event.getGuild().getIdLong()) || getGuildEntity().getCustomCommands().size() < MAX_COMMANDS_FREE) {
                    trigger = null;
                    oldTrigger = null;
                    config = new CustomCommandEntity();
                    updateMode = false;
                    setState(STATE_CONFIG);
                } else {
                    setLog(LogStatus.FAILURE, getString("error_freeslotsfull", StringUtil.numToString(MAX_COMMANDS_FREE)));
                }
                return true;
            }
            case 1 -> {
                if (!getGuildEntity().getCustomCommands().isEmpty()) {
                    setState(STATE_EDIT);
                } else {
                    setLog(LogStatus.FAILURE, getString("error_noentries"));
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @ControllerButton(state = STATE_EDIT)
    public boolean onButtonEdit(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }

        GuildEntity guildEntity = getGuildEntity();
        trigger = new ArrayList<>(guildEntity.getCustomCommands().keySet()).get(i);
        oldTrigger = trigger;
        config = new ArrayList<>(guildEntity.getCustomCommands().values()).get(i).copy();
        updateMode = true;
        deleteLock = true;
        setState(STATE_CONFIG);
        return true;
    }

    @ControllerButton(state = STATE_CONFIG)
    public boolean onButtonAdd(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                setState(updateMode ? STATE_EDIT : DEFAULT_STATE);
                return true;
            }
            case 0 -> {
                Modal modal = new StringModalBuilder(this, getString("add_trigger"), TextInputStyle.SHORT)
                        .setMinMaxLength(1, MAX_COMMAND_TRIGGER_LENGTH)
                        .setGetter(() -> trigger)
                        .setSetterOptionalLogs(newTrigger -> {
                            newTrigger = newTrigger.replaceAll("[^a-zA-Z0-9-_]", "").toLowerCase();
                            if (newTrigger.isEmpty()) {
                                setLog(LogStatus.FAILURE, getString("error_triggerinvalidchars"));
                            } else {
                                trigger = newTrigger;
                            }
                            return false;
                        })
                        .build();
                event.replyModal(modal).queue();
                deleteLock = true;
                return false;
            }
            case 1 -> {
                Modal modal = new StringModalBuilder(this, getString("add_header_title"), TextInputStyle.SHORT)
                        .setMinMaxLength(0, MAX_COMMAND_TITLE_LENGTH)
                        .setGetter(() -> config.getTitle())
                        .setSetter(input -> config.setTitle(input))
                        .build();
                event.replyModal(modal).queue();
                deleteLock = true;
                return false;
            }
            case 2 -> {
                deleteLock = true;
                setState(STATE_SET_EMOJI);
                return true;
            }
            case 3 -> {
                deleteLock = true;
                setState(STATE_SET_TEXT_RESPONSE);
                return true;
            }
            case 4 -> {
                deleteLock = true;
                setState(STATE_SET_IMAGE);
                return true;
            }
            case 5 -> {
                GuildEntity guildEntity = getGuildEntity();
                Map<String, CustomCommandEntity> customCommands = guildEntity.getCustomCommands();

                if (customCommands.containsKey(trigger) && !trigger.equals(oldTrigger)) {
                    setLog(LogStatus.FAILURE, getString("error_alreadyexists"));
                    return true;
                }
                if (!ServerPatreonBoostCache.get(event.getGuild().getIdLong()) && customCommands.size() >= MAX_COMMANDS_FREE) {
                    setLog(LogStatus.FAILURE, getString("error_freeslotsfull", StringUtil.numToString(MAX_COMMANDS_FREE)));
                    return true;
                }

                guildEntity.beginTransaction();
                customCommands.put(trigger, config.copy());
                if (oldTrigger != null && !trigger.equals(oldTrigger)) {
                    customCommands.remove(oldTrigger);
                }
                if (updateMode) {
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.CUSTOM_COMMANDS_EDIT, event.getMember(), oldTrigger);
                } else {
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.CUSTOM_COMMANDS_ADD, event.getMember(), trigger);
                }
                guildEntity.commitTransaction();

                if (updateMode) {
                    setLog(LogStatus.SUCCESS, getString("log_update", oldTrigger));
                } else {
                    setLog(LogStatus.SUCCESS, getString("log_add", trigger));
                }
                deleteLock = true;
                setState(updateMode ? STATE_EDIT : DEFAULT_STATE);
                return true;
            }
            case 6 -> {
                if (deleteLock) {
                    deleteLock = false;
                    setLog(LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "confirm_warning_button"));
                    return true;
                }

                GuildEntity guildEntity = getGuildEntity();
                Map<String, CustomCommandEntity> customCommands = guildEntity.getCustomCommands();

                guildEntity.beginTransaction();
                customCommands.remove(oldTrigger);
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.CUSTOM_COMMANDS_DELETE, event.getMember(), oldTrigger);
                guildEntity.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("log_deleted", oldTrigger));
                setState(customCommands.isEmpty() ? DEFAULT_STATE : STATE_EDIT);
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean onReaction(@NotNull GenericMessageReactionEvent event) {
        return emojiStateProcessor.handleReactionEvent(event);
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawDefault(Member member) {
        setComponents(getString("default_options").split("\n"));
        return EmbedFactory.getEmbedDefault(
                this,
                getString("default_desc", StringUtil.numToString(MAX_COMMANDS_FREE), ExternalLinks.PREMIUM_WEBSITE)
        ).addField(getString("default_list_title"), new ListGen<String>().getList(getGuildEntity().getCustomCommands().keySet(), getLocale(), m -> "`" + m + "`"), false);
    }

    @Draw(state = STATE_CONFIG)
    public EmbedBuilder onDrawAdd(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        String[] options = getString("add_options").split("\n");
        if (!updateMode) {
            options[6] = "";
        }
        setComponents(options, Set.of(5), Set.of(6), trigger == null || config.getTextResponse().isEmpty() ? Set.of(5) : null);

        return EmbedFactory.getEmbedDefault(
                        this,
                        getString("add_desc"), getString("add_title", updateMode)
                )
                .addField(getString("add_trigger"), trigger != null ? "`" + trigger + "`" : notSet, true)
                .addField(getString("add_header_title"), Objects.requireNonNullElse(config.getTitle(), notSet), true)
                .addField(getString("add_emoji"), Objects.requireNonNullElse(config.getEmojiFormatted(), notSet), true)
                .addField(getString("add_textresponse"), config.getTextResponse().isEmpty() ? notSet : StringUtil.shortenString(StringUtil.escapeMarkdown(config.getTextResponse()), MAX_TEXT_RESPONSE_LENGTH), false)
                .addField(getString("add_includesimage"), StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), config.getImageFilename() != null), true);
    }

    @Draw(state = STATE_EDIT)
    public EmbedBuilder onDrawEdit(Member member) {
        setComponents(getGuildEntity().getCustomCommands().keySet().stream().map(str -> str + " ✕").toArray(String[]::new));
        return EmbedFactory.getEmbedDefault(
                this,
                getString("edit_desc"), getString("edit_title")
        );
    }

}
