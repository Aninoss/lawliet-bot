package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.listeners.OnReactionListener;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.EmojiStateProcessor;
import commands.stateprocessor.StringStateProcessor;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.LocalFile;
import core.TextManager;
import core.modals.ModalMediator;
import core.modals.StringModalBuilder;
import core.utils.FileUtil;
import core.utils.InternetUtil;
import core.utils.RandomUtil;
import core.utils.StringUtil;
import modules.CustomRolePlay;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.CustomRolePlayEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "customrp",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "👐",
        executableWithoutArgs = true,
        patreonRequired = true,
        aliases = {"customroleplay"}
)
public class CustomRolePlayCommand extends NavigationAbstract implements OnReactionListener {

    public static final int MAX_COMMANDS = 50;
    public static final int MAX_COMMAND_TRIGGER_LENGTH = 25;
    public static final int MAX_COMMAND_TITLE_LENGTH = 200;
    public static final int MAX_TEXT_LENGTH = MessageEmbed.VALUE_MAX_LENGTH;
    public static final int MAX_ATTACHMENTS = 50;

    private static final int
            STATE_CONFIG = 1,
            STATE_EDIT_SELECT = 2,
            STATE_SET_EMOJI = 3,
            STATE_SET_TEXT_NO_MEMBERS = 4,
            STATE_SET_TEXT_SINGLE_MEMBER = 5,
            STATE_SET_TEXT_MULTI_MEMBERS = 6,
            STATE_ADD_ATTACHMENTS = 7,
            STATE_REMOVE_ATTACHMENTS = 8;

    private EmojiStateProcessor emojiStateProcessor;
    private CustomRolePlayEntity config;
    private String trigger;
    private String oldTrigger;
    private final ArrayList<File> attachmentFiles = new ArrayList<>();
    private boolean updateMode;
    private int currentImage = 0;
    private boolean deleteLock = true;

    public CustomRolePlayCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        emojiStateProcessor = new EmojiStateProcessor(this, STATE_SET_EMOJI, STATE_CONFIG, getString("config_property_emoji"))
                .setClearButton(false)
                .setGetter(() -> config.getEmoji())
                .setSetter(emoji -> config.setEmojiFormatted(emoji.getFormatted()));

        registerNavigationListener(event.getMember(), List.of(
                emojiStateProcessor,
                new StringStateProcessor(this, STATE_SET_TEXT_NO_MEMBERS, STATE_CONFIG, getString("config_property_textno"))
                        .setMax(MAX_TEXT_LENGTH)
                        .setDescription(getString("text_nomembers_desc"))
                        .setClearButton(true)
                        .setGetter(() -> config.getTextNoMembers())
                        .setSetter(input -> config.setTextNoMembers(input)),
                new StringStateProcessor(this, STATE_SET_TEXT_SINGLE_MEMBER, STATE_CONFIG, getString("config_property_textsingle"))
                        .setMax(MAX_TEXT_LENGTH)
                        .setDescription(getString("text_members_desc"))
                        .setClearButton(true)
                        .setGetter(() -> config.getTextSingleMember())
                        .setSetter(input -> config.setTextSingleMember(input)),
                new StringStateProcessor(this, STATE_SET_TEXT_MULTI_MEMBERS, STATE_CONFIG, getString("config_property_textmulti"))
                        .setMax(MAX_TEXT_LENGTH)
                        .setDescription(getString("text_members_desc"))
                        .setClearButton(true)
                        .setGetter(() -> config.getTextMultiMembers())
                        .setSetter(input -> config.setTextMultiMembers(input))
        ));
        registerReactionListener(event.getMember());
        return true;
    }

    @ControllerMessage(state = STATE_ADD_ATTACHMENTS)
    public MessageInputResponse onMessageAddAttachments(MessageReceivedEvent event, String input) {
        List<File> attachments = event.getMessage().getAttachments().stream()
                .filter(attachment -> InternetUtil.uriIsImage(attachment.getUrl(), true))
                .map(attachment -> {
                    LocalFile tempFile = new LocalFile(LocalFile.Directory.CDN, String.format("customrp/%s.%s", RandomUtil.generateRandomString(30), attachment.getFileExtension()));
                    FileUtil.downloadImageAttachment(attachment, tempFile);
                    return tempFile;
                })
                .limit(MAX_ATTACHMENTS - config.getImageFilenames().size())
                .collect(Collectors.toList());

        if (attachments.isEmpty()) {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "imagenotfound"));
            return MessageInputResponse.FAILED;
        }

        config.getImageFilenames().addAll(attachments.stream().map(File::getName).collect(Collectors.toList()));
        attachmentFiles.addAll(attachments);
        setLog(LogStatus.SUCCESS, getString("log_addattachment", attachments.size() != 1, StringUtil.numToString(attachments.size())));
        setState(STATE_CONFIG);
        return MessageInputResponse.SUCCESS;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                config = new CustomRolePlayEntity();
                updateMode = false;
                trigger = null;
                oldTrigger = null;
                config.setTitle(getString("title"));
                config.setEmojiFormatted(getCommandProperties().emoji());
                setState(STATE_CONFIG);
                return true;
            }
            case 1 -> {
                if (!getGuildEntity().getCustomRolePlayCommands().isEmpty()) {
                    setState(STATE_EDIT_SELECT);
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

    @ControllerButton(state = STATE_EDIT_SELECT)
    public boolean onButtonEdit(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }

        GuildEntity guildEntity = getGuildEntity();
        trigger = new ArrayList<>(guildEntity.getCustomRolePlayCommands().keySet()).get(i);
        oldTrigger = trigger;
        config = new ArrayList<>(guildEntity.getCustomRolePlayCommands().values()).get(i).copy();
        updateMode = true;
        deleteLock = true;
        setState(STATE_CONFIG);
        return true;
    }

    @ControllerButton(state = STATE_CONFIG)
    public boolean onButtonAdd(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                attachmentFiles.forEach(File::delete);
                attachmentFiles.clear();
                setState(updateMode ? STATE_EDIT_SELECT : DEFAULT_STATE);
                return true;
            }
            case 0 -> {
                Modal modal = new StringModalBuilder(this, getString("config_property_trigger"), TextInputStyle.SHORT)
                        .setMinMaxLength(1, MAX_COMMAND_TRIGGER_LENGTH)
                        .setGetter(() -> trigger)
                        .setSetter(newTrigger -> {
                            newTrigger = newTrigger.replaceAll("[^a-zA-Z0-9-_]", "").toLowerCase();
                            if (newTrigger.isEmpty()) {
                                setLog(LogStatus.FAILURE, getString("error_triggerinvalidchars"));
                            } else {
                                trigger = newTrigger;
                            }
                        })
                        .build();
                event.replyModal(modal).queue();
                deleteLock = true;
                return false;
            }
            case 1 -> {
                Modal modal = new StringModalBuilder(this, getString("config_property_title"), TextInputStyle.SHORT)
                        .setMinMaxLength(1, MAX_COMMAND_TITLE_LENGTH)
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
                setState(STATE_SET_TEXT_NO_MEMBERS);
                return true;
            }
            case 4 -> {
                deleteLock = true;
                setState(STATE_SET_TEXT_SINGLE_MEMBER);
                return true;
            }
            case 5 -> {
                deleteLock = true;
                setState(STATE_SET_TEXT_MULTI_MEMBERS);
                return true;
            }
            case 6 -> {
                config.setNsfw(!config.getNsfw());
                deleteLock = true;
                setLog(LogStatus.SUCCESS, getString("log_nsfw", config.getNsfw()));
                return true;
            }
            case 7 -> {
                if (config.getImageFilenames().size() >= MAX_ATTACHMENTS) {
                    setLog(LogStatus.FAILURE, getString("error_attachmentlimitreached"));
                    return true;
                }
                deleteLock = true;
                setState(STATE_ADD_ATTACHMENTS);
                return true;
            }
            case 8 -> {
                currentImage = 0;
                deleteLock = true;
                setState(STATE_REMOVE_ATTACHMENTS);
                return true;
            }
            case 9 -> {
                GuildEntity guildEntity = getGuildEntity();
                Map<String, CustomRolePlayEntity> customRolePlay = guildEntity.getCustomRolePlayCommands();

                if (!updateMode && customRolePlay.size() >= MAX_COMMANDS) {
                    setLog(LogStatus.FAILURE, getString("error_limitreached"));
                    return true;
                }
                if (customRolePlay.containsKey(trigger) && !trigger.equals(oldTrigger)) {
                    setLog(LogStatus.FAILURE, getString("error_alreadyexists"));
                    return true;
                }

                guildEntity.beginTransaction();
                customRolePlay.put(trigger, config.copy());
                if (oldTrigger != null && !trigger.equals(oldTrigger)) {
                    customRolePlay.remove(oldTrigger);
                }
                if (updateMode) {
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.CUSTOM_ROLE_PLAY_EDIT, event.getMember(), oldTrigger);
                } else {
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.CUSTOM_ROLE_PLAY_ADD, event.getMember(), trigger);
                }
                guildEntity.commitTransaction();

                attachmentFiles.clear();
                deleteLock = true;
                setLog(LogStatus.SUCCESS, getString("log_success", updateMode ? oldTrigger : trigger));
                setState(updateMode ? STATE_EDIT_SELECT : DEFAULT_STATE);
                return true;
            }
            case 10 -> {
                if (deleteLock) {
                    deleteLock = false;
                    setLog(LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "confirm_warning_button"));
                    return true;
                }

                GuildEntity guildEntity = getGuildEntity();
                Map<String, CustomRolePlayEntity> customRolePlay = guildEntity.getCustomRolePlayCommands();

                guildEntity.beginTransaction();
                customRolePlay.remove(oldTrigger);
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.CUSTOM_ROLE_PLAY_DELETE, event.getMember(), oldTrigger);
                guildEntity.commitTransaction();

                attachmentFiles.clear();
                setLog(LogStatus.SUCCESS, getString("log_deleted", oldTrigger));
                setState(customRolePlay.isEmpty() ? DEFAULT_STATE : STATE_EDIT_SELECT);
                return true;
            }
        }

        return true;
    }

    @ControllerButton(state = STATE_ADD_ATTACHMENTS)
    public boolean onButtonAddAttachments(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(STATE_CONFIG);
            return true;
        }
        return false;
    }

    @ControllerButton(state = STATE_REMOVE_ATTACHMENTS)
    public boolean onButtonRemoveAttachments(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                setState(STATE_CONFIG);
                return true;
            }
            case 0 -> {
                currentImage -= 1;
                if (currentImage < 0) {
                    currentImage = config.getImageFilenames().size() - 1;
                }
                return true;
            }
            case 1 -> {
                int attachmentsSize = config.getImageFilenames().size();
                String textId = "page";
                String textLabel = getString("goto_label", String.valueOf(attachmentsSize));
                TextInput message = TextInput.create(textId, textLabel, TextInputStyle.SHORT)
                        .setPlaceholder(String.valueOf(currentImage + 1))
                        .setMinLength(1)
                        .setMaxLength(2)
                        .build();

                String title = TextManager.getString(getLocale(), TextManager.GENERAL, "list_goto");
                Modal modal = ModalMediator.createDrawableCommandModal(this, title, e -> {
                            String pageString = e.getValue(textId).getAsString();
                            if (StringUtil.stringIsInt(pageString)) {
                                currentImage = Math.min(attachmentsSize - 1, Math.max(0, Integer.parseInt(pageString) - 1));
                            }
                            return null;
                        }).addComponents(ActionRow.of(message))
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case 2 -> {
                currentImage += 1;
                if (currentImage >= config.getImageFilenames().size()) {
                    currentImage = 0;
                }
                return true;
            }
            case 3 -> {
                config.getImageFilenames().remove(currentImage);
                currentImage = Math.min(currentImage, config.getImageFilenames().size() - 1);
                if (config.getImageFilenames().isEmpty()) {
                    setState(STATE_CONFIG);
                }
                setLog(LogStatus.SUCCESS, getString("log_removeattachment"));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onReaction(@NotNull GenericMessageReactionEvent event) {
        return emojiStateProcessor.handleReactionEvent(event);
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawDefault(Member member) {
        setComponents(getString("default_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this)
                .addField(getString("default_list_title"), new ListGen<String>().getList(getGuildEntity().getCustomRolePlayCommands().keySet(), getLocale(), m -> "`" + m + "`"), false);
    }

    @Draw(state = STATE_CONFIG)
    public EmbedBuilder onDrawAdd(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");

        String[] options = getString("config_options").split("\n");
        if (!updateMode) {
            options[10] = "";
        }
        setComponents(options, Set.of(9), Set.of(10), trigger == null || config.getImageFilenames().isEmpty() ? Set.of(9) : null);

        return EmbedFactory.getEmbedDefault(this, getString("config_desc"), getString("config_title", updateMode))
                .addField(getString("config_property_trigger"), trigger != null ? "`" + trigger + "`" : notSet, true)
                .addField(getString("config_property_title"), config.getTitle(), true)
                .addField(getString("config_property_emoji"), config.getEmojiFormatted(), true)
                .addField(getString("config_property_textno"), config.getTextNoMembers() != null ? stressVariables(config.getTextNoMembers()) : notSet, false)
                .addField(getString("config_property_textsingle"), config.getTextSingleMember() != null ? stressVariables(config.getTextSingleMember()) : notSet, false)
                .addField(getString("config_property_textmulti"), config.getTextMultiMembers() != null ? stressVariables(config.getTextMultiMembers()) : notSet, false)
                .addField(getString("config_property_nsfw"), StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), config.getNsfw()), true)
                .addField(getString("config_property_attachments"), StringUtil.numToString(config.getImageFilenames().size()), true);
    }

    @Draw(state = STATE_EDIT_SELECT)
    public EmbedBuilder onDrawEdit(Member member) {
        setComponents(getGuildEntity().getCustomRolePlayCommands().keySet().toArray(String[]::new));
        return EmbedFactory.getEmbedDefault(this, getString("edit_desc"), getString("edit_title"));
    }

    @Draw(state = STATE_ADD_ATTACHMENTS)
    public EmbedBuilder onDrawAddAttachments(Member member) {
        return EmbedFactory.getEmbedDefault(this, getString("addattachments_desc"), getString("addattachments_title"));
    }

    @Draw(state = STATE_REMOVE_ATTACHMENTS)
    public EmbedBuilder onDrawRemoveAttachments(Member member) {
        String[] options = new String[]{
                TextManager.getString(getLocale(), TextManager.GENERAL, "list_previous"),
                TextManager.getString(getLocale(), TextManager.GENERAL, "list_goto"),
                TextManager.getString(getLocale(), TextManager.GENERAL, "list_next"),
                getString("removeattachments_delete")
        };
        setComponents(options, null, Set.of(3));
        return EmbedFactory.getEmbedDefault(this, getString("removeattachments_current",
                        StringUtil.numToString(currentImage + 1), StringUtil.numToString(config.getImageFilenames().size())),
                        getString("removeattachments_title")
                ).setImage(config.getImageUrls().get(currentImage));
    }

    private String stressVariables(String text) {
        return CustomRolePlay.resolveVariables(
                StringUtil.escapeMarkdown(text),
                "`%AUTHOR`",
                "`%USER_MENTIONS`"
        );
    }

}
