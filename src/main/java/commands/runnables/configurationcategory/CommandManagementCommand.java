package commands.runnables.configurationcategory;

import commands.Category;
import commands.Command;
import commands.CommandContainer;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.DisabledCommands;
import core.EmbedFactory;
import core.TextManager;
import core.modals.StringModalBuilder;
import core.utils.StringUtil;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "cman",
        userGuildPermissions = Permission.ADMINISTRATOR,
        emoji = "🚦",
        executableWithoutArgs = true,
        aliases = { "commandmanagement", "cmanagement", "cm", "commandmanagements", "commandmanager", "commandm", "comman" }
)
public class CommandManagementCommand extends NavigationAbstract {

    private static final int
            SET_CATEGORIES = 1,
            REMOVE_COMMANDS = 2;

    public CommandManagementCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerNavigationListener(event.getMember());
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonMain(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                setState(SET_CATEGORIES);
                return true;
            }
            case 1 -> {
                Modal modal = new StringModalBuilder(this, getString("state0_mcommands"), TextInputStyle.SHORT)
                        .setMinMaxLength(1, TextInput.MAX_VALUE_LENGTH)
                        .setSetterOptionalLogs(input -> {
                            List<String> commands = Arrays.stream(input.split(" "))
                                    .map(trigger -> CommandContainer.getCommandMap().get(trigger))
                                    .filter(Objects::nonNull)
                                    .filter(clazz -> {
                                        CommandProperties commandProperties = Command.getCommandProperties(clazz);
                                        return commandProperties.exclusiveGuilds().length == 0 && commandProperties.exclusiveUsers().length == 0;
                                    })
                                    .map(clazz -> Command.getCommandProperties(clazz).trigger())
                                    .distinct()
                                    .collect(Collectors.toList());

                            if (commands.isEmpty()) {
                                setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                                return false;
                            }

                            GuildEntity guildEntity = getGuildEntity();
                            Set<String> disabledCommands = guildEntity.getDisabledCommandsAndCategories();
                            List<String> newCommands = commands.stream()
                                    .filter(trigger -> !disabledCommands.contains(trigger))
                                    .collect(Collectors.toList());

                            if (newCommands.isEmpty()) {
                                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "element_exists", commands.size() != 1));
                                return false;
                            }

                            guildEntity.beginTransaction();
                            guildEntity.getDisabledCommandsAndCategories().addAll(newCommands);
                            BotLogEntity.log(guildEntity.getEntityManager(), BotLogEntity.Event.COMMAND_MANAGEMENT, event.getMember(), newCommands, null);
                            guildEntity.commitTransaction();

                            setLog(LogStatus.SUCCESS, getString("addcommands_set", newCommands.size() != 1, StringUtil.numToString(newCommands.size())));
                            return false;
                        })
                        .build();
                event.replyModal(modal).queue();
                return false;
            }
            case 2 -> {
                if (!DisabledCommands.getDisabledCommands(getGuildEntity()).isEmpty()) {
                    setState(REMOVE_COMMANDS);
                    return true;
                } else {
                    setLog(LogStatus.FAILURE, getString("nocommand"));
                    return true;
                }
            }
            default -> {
                return false;
            }
        }
    }

    @ControllerButton(state = SET_CATEGORIES)
    public boolean onButtonSetCategories(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }
        return false;
    }

    @ControllerButton(state = REMOVE_COMMANDS)
    public boolean onButtonRemoveCommands(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        } else {
            GuildEntity guildEntity = getGuildEntity();
            Set<String> disabledCommandsAndCateogories = guildEntity.getDisabledCommandsAndCategories();
            String trigger = event.getButton().getLabel().split(" ")[0];

            if (disabledCommandsAndCateogories.contains(trigger)) {
                guildEntity.beginTransaction();
                disabledCommandsAndCateogories.remove(trigger);
                BotLogEntity.log(guildEntity.getEntityManager(), BotLogEntity.Event.COMMAND_MANAGEMENT, event.getMember(), null, trigger);
                guildEntity.commitTransaction();
            }

            setLog(LogStatus.SUCCESS, getString("commandremoved", trigger));
            if (DisabledCommands.getDisabledCommands(guildEntity).isEmpty()) {
                setState(DEFAULT_STATE);
            }
            return true;
        }
    }

    @ControllerStringSelectMenu(state = SET_CATEGORIES)
    public boolean onSelectMenu(StringSelectInteractionEvent event, int i) {
        GuildEntity guildEntity = getGuildEntity();
        Set<String> disabledCommandsAndCategories = guildEntity.getDisabledCommandsAndCategories();

        guildEntity.beginTransaction();
        for (String value : event.getValues()) {
            if (!disabledCommandsAndCategories.contains(value)) {
                disabledCommandsAndCategories.add(value);
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.COMMAND_MANAGEMENT, event.getMember(), value, null);
            }
        }
        for (Category category : Category.independentValues()) {
            if (disabledCommandsAndCategories.contains(category.getId()) && !event.getValues().contains(category.getId())) {
                disabledCommandsAndCategories.remove(category.getId());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.COMMAND_MANAGEMENT, event.getMember(), null, category.getId());
            }
        }
        guildEntity.commitTransaction();

        setLog(LogStatus.SUCCESS, getString("categoryset_set"));
        setState(DEFAULT_STATE);
        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawMain(Member member) {
        setComponents(getString("state0_options").split("\n"));
        List<String> categoryNameList = DisabledCommands.getDisabledCommandCategories(getGuildEntity()).stream()
                .map(category -> TextManager.getString(getLocale(), TextManager.COMMANDS, category.getId()))
                .collect(Collectors.toList());
        return EmbedFactory.getEmbedDefault(this, getString("state0_desc"))
                .addField(getString("state0_mcategories"), StringUtil.shortenString(generateList(categoryNameList), 1024), true)
                .addField(getString("state0_mcommands"), StringUtil.shortenString(generateList(DisabledCommands.getDisabledCommands(getGuildEntity())), 1024), true);
    }

    @Draw(state = SET_CATEGORIES)
    public EmbedBuilder onDrawSetCategory(Member member) {
        List<String> categoryIdList = DisabledCommands.getDisabledCommandCategories(getGuildEntity()).stream()
                .map(Category::getId)
                .collect(Collectors.toList());

        StringSelectMenu.Builder selectionMenuBuilder = StringSelectMenu.create("categories");
        selectionMenuBuilder = selectionMenuBuilder.setRequiredRange(0, Category.independentValues().length);
        for (Category category : Category.independentValues()) {
            selectionMenuBuilder = selectionMenuBuilder.addOption(TextManager.getString(getLocale(), TextManager.COMMANDS, category.getId()), category.getId());
        }
        selectionMenuBuilder = selectionMenuBuilder.setDefaultValues(categoryIdList);
        setComponents(selectionMenuBuilder.build());
        return EmbedFactory.getEmbedDefault(this, getString("state1_desc"), getString("state1_title"));
    }

    @Draw(state = REMOVE_COMMANDS)
    public EmbedBuilder onDrawRemoveCommands(Member member) {
        setComponents(DisabledCommands.getDisabledCommands(getGuildEntity()).stream().map(str -> str + " ✕").toArray(String[]::new));
        return EmbedFactory.getEmbedDefault(this, getString("state3_desc"), getString("state3_title"));
    }

    private String generateList(Collection<String> elements) {
        StringBuilder sb = new StringBuilder();
        for (String element : elements) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append('`').append(element).append('`');
        }
        if (!sb.isEmpty()) {
            return sb.toString();
        } else {
            return TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        }
    }


}
