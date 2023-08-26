package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.ExternalLinks;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.cache.ServerPatreonBoostCache;
import core.modals.ModalMediator;
import core.utils.StringUtil;
import mysql.hibernate.entity.CustomCommandEntity;
import mysql.hibernate.entity.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

@CommandProperties(
        trigger = "customconfig",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "🧩",
        executableWithoutArgs = true,
        aliases = {"customcommands"}
)
public class CustomConfigCommand extends NavigationAbstract {

    public static final int MAX_COMMANDS_FREE = 3;
    public static final int MAX_COMMAND_TRIGGER_LENGTH = 25;
    public static final int MAX_TEXT_RESPONSE_LENGTH = MessageEmbed.VALUE_MAX_LENGTH;

    private final int
            STATE_ADD_AND_UPDATE = 1,
            STATE_EDIT = 2;

    private String trigger;
    private String textResponse;
    private boolean updateMode;
    private String oldTrigger;

    public CustomConfigCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerNavigationListener(event.getMember());
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
                    textResponse = null;
                    updateMode = false;
                    oldTrigger = null;
                    setState(STATE_ADD_AND_UPDATE);
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
        textResponse = new ArrayList<>(guildEntity.getCustomCommands().values()).get(i).getTextResponse();
        updateMode = true;
        oldTrigger = trigger;
        setState(STATE_ADD_AND_UPDATE);
        return true;
    }

    @ControllerButton(state = STATE_ADD_AND_UPDATE)
    public boolean onButtonAdd(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                setState(updateMode ? STATE_EDIT : DEFAULT_STATE);
                return true;
            }
            case 0 -> {
                String textId = "trigger";
                TextInput message = TextInput.create(textId, getString("add_trigger"), TextInputStyle.SHORT)
                        .setValue(trigger)
                        .setMinLength(1)
                        .setMaxLength(MAX_COMMAND_TRIGGER_LENGTH)
                        .build();

                Modal modal = ModalMediator.createDrawableCommandModal(this, getString("add_trigger"), e -> {
                            String newTrigger = e.getValue(textId).getAsString();
                            newTrigger = newTrigger.replaceAll("[^a-zA-Z0-9-_]", "").toLowerCase();
                            if (newTrigger.isEmpty()) {
                                setLog(LogStatus.FAILURE, getString("error_triggerinvalidchars"));
                            } else {
                                trigger = newTrigger;
                            }
                            return null;
                        }).addActionRows(ActionRow.of(message))
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case 1 -> {
                String textId = "textResponse";
                TextInput message = TextInput.create(textId, getString("add_textresponse"), TextInputStyle.PARAGRAPH)
                        .setValue(textResponse)
                        .setMinLength(1)
                        .setMaxLength(MAX_TEXT_RESPONSE_LENGTH)
                        .build();

                Modal modal = ModalMediator.createDrawableCommandModal(this, getString("add_textresponse"), e -> {
                            textResponse = e.getValue(textId).getAsString();
                            return null;
                        }).addActionRows(ActionRow.of(message))
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case 2 -> {
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
                customCommands.put(trigger, new CustomCommandEntity(guildEntity, textResponse));
                if (oldTrigger != null && !trigger.equals(oldTrigger)) {
                    customCommands.remove(oldTrigger);
                }
                guildEntity.commitTransaction();

                if (updateMode) {
                    setLog(LogStatus.SUCCESS, getString("log_update", oldTrigger));
                } else {
                    setLog(LogStatus.SUCCESS, getString("log_add", trigger));
                }
                setState(updateMode ? STATE_EDIT : DEFAULT_STATE);
                return true;
            }
            case 3 -> {
                GuildEntity guildEntity = getGuildEntity();
                Map<String, CustomCommandEntity> customCommands = guildEntity.getCustomCommands();

                guildEntity.beginTransaction();
                customCommands.remove(oldTrigger);
                guildEntity.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("log_deleted", oldTrigger));
                setState(customCommands.isEmpty() ? DEFAULT_STATE : STATE_EDIT);
                return true;
            }
        }

        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawDefault(Member member) {
        setComponents(getString("default_options").split("\n"));
        return EmbedFactory.getEmbedDefault(
                this,
                getString("default_desc", StringUtil.numToString(MAX_COMMANDS_FREE), ExternalLinks.PREMIUM_WEBSITE)
        ).addField(getString("default_list_title"), new ListGen<String>().getList(getGuildEntity().getCustomCommands().keySet(), getLocale(), m -> "`" + m + "`"), false);
    }

    @Draw(state = STATE_ADD_AND_UPDATE)
    public EmbedBuilder onDrawAdd(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");

        if (updateMode) {
            String[] options = getString("add_update_options").split("\n");
            Button[] buttons = new Button[options.length];
            for (int i = 0; i < options.length; i++) {
                buttons[i] = Button.of(i == options.length - 1 ? ButtonStyle.DANGER : ButtonStyle.PRIMARY, String.valueOf(i), options[i]);
            }
            setComponents(buttons);
        } else {
            String[] options = trigger == null || textResponse == null
                    ? getString("add_options").split("\n")
                    : getString("add_options2").split("\n");
            setComponents(options);
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(
                this,
                getString("add_desc"), getString("add_title")
        );
        eb.addField(getString("add_trigger"), trigger != null ? "`" + trigger + "`" : notSet, true);
        eb.addField(getString("add_textresponse"), textResponse != null ? StringUtil.shortenString(StringUtil.escapeMarkdown(textResponse), MAX_TEXT_RESPONSE_LENGTH) : notSet, true);
        return eb;
    }

    @Draw(state = STATE_EDIT)
    public EmbedBuilder onDrawEdit(Member member) {
        setComponents(getGuildEntity().getCustomCommands().keySet().toArray(String[]::new));
        return EmbedFactory.getEmbedDefault(
                this,
                getString("edit_desc"), getString("edit_title")
        );
    }

}
