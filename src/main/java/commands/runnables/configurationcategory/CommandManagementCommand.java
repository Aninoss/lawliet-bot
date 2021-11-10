package commands.runnables.configurationcategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import commands.*;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.TextManager;
import mysql.modules.commandmanagement.CommandManagementData;
import mysql.modules.commandmanagement.DBCommandManagement;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "cman",
        userGuildPermissions = Permission.ADMINISTRATOR,
        emoji = "🚦",
        executableWithoutArgs = true,
        aliases = { "commandmanagement", "cmanagement", "cm", "commandmanagements", "commandmanager", "commandm", "comman" }
)
public class CommandManagementCommand extends NavigationAbstract {

    private CommandManagementData commandManagementBean;
    private Category category;

    public CommandManagementCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        commandManagementBean = DBCommandManagement.getInstance().retrieve(event.getGuild().getIdLong());
        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(GuildMessageReceivedEvent event, String input, int state) {
        return null;
    }

    @Override
    public boolean controllerSelectionMenu(SelectionMenuEvent event, int i, int state) throws Throwable {
        category = Category.independentValues()[i];
        setState(1);
        return true;
    }

    @Override
    public boolean controllerButton(ButtonClickEvent event, int i, int state) throws Throwable {
        switch (state) {
            case 0:
                if (i == -1) {
                    deregisterListenersWithComponentMessage();
                }
                return false;

            case 1:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        turnOnAllCategoryCommands();
                        commandManagementBean.getSwitchedOffElements().remove(category.getId());
                        setLog(LogStatus.SUCCESS, getString("categoryset_all", true, TextManager.getString(getLocale(), TextManager.COMMANDS, category.getId())));
                        return true;

                    case 1:
                        setState(2);
                        return true;

                    case 2:
                        turnOnAllCategoryCommands();
                        commandManagementBean.getSwitchedOffElements().add(category.getId());
                        setLog(LogStatus.SUCCESS, getString("categoryset_all", false, TextManager.getString(getLocale(), TextManager.COMMANDS, category.getId())));
                        return true;

                    default:
                        return false;
                }

            case 2:
                List<Command> commandList = CommandContainer.getCommandCategoryMap().get(category).stream()
                        .map(clazz -> CommandManager.createCommandByClass(clazz, getLocale(), getPrefix()))
                        .collect(Collectors.toList());

                if (i == -1) {
                    setState(1);
                    return true;
                } else if (i >= 0 && i < commandList.size()) {
                    Command command = commandList.get(i);
                    if (commandManagementBean.commandIsTurnedOn(command)) {
                        commandManagementBean.getSwitchedOffElements().add(command.getTrigger());
                        setLog(LogStatus.SUCCESS, getString("commandset", false, command.getTrigger()));
                    } else {
                        if (commandManagementBean.getSwitchedOffElements().contains(command.getCategory().getId())) {
                            commandManagementBean.getSwitchedOffElements().remove(command.getCategory().getId());
                            commandList.stream()
                                    .filter(c -> !c.equals(command))
                                    .forEach(c -> commandManagementBean.getSwitchedOffElements().add(c.getTrigger()));
                        } else {
                            commandManagementBean.getSwitchedOffElements().remove(command.getTrigger());
                        }
                        setLog(LogStatus.SUCCESS, getString("commandset", true, command.getTrigger()));
                    }
                    return true;
                }
                return false;

            default:
                return false;
        }
    }

    @Override
    public EmbedBuilder draw(Member member, int state) {
        switch (state) {
            case 0:
                setComponents(generateSelectionMenu(null));
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"));

            case 1:
                List<Button> buttonsList = optionsToButtons(getString("state1_options").split("\n"));
                setActionRows(
                        ActionRow.of(buttonsList),
                        ActionRow.of(generateSelectionMenu(category))
                );
                String categoryName = TextManager.getString(getLocale(), TextManager.COMMANDS, category.getId());
                return EmbedFactory.getEmbedDefault(this, getString("state1_description", getCategoryStatus(category), categoryName));

            case 2:
                ArrayList<Class<? extends Command>> commands = CommandContainer.getCommandCategoryMap().get(category);
                Button[] buttons = new Button[commands.size()];
                for (int i = 0; i < buttons.length; i++) {
                    Command command = CommandManager.createCommandByClass(commands.get(i), getLocale(), getPrefix());
                    int status = commandManagementBean.commandIsTurnedOn(command) ? 2 : 0;
                    buttons[i] = Button.of(
                            getButtonStyleFromStatus(status),
                            String.valueOf(i),
                            getString("command", command.getTrigger(), TextManager.getString(getLocale(), command.getCategory(), command.getTrigger() + "_title")),
                            Emoji.fromUnicode(getUnicodeEmojiFromStatus(status))
                    );
                }
                setComponents(buttons);
                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));

            default:
                return null;
        }
    }

    private SelectionMenu generateSelectionMenu(Category currentCategory) {
        SelectionMenu.Builder builder = SelectionMenu.create("category")
                .setPlaceholder(getString("category_placeholder"));
        Category[] categories = Category.independentValues();
        for (int i = 0; i < categories.length; i++) {
            Category category = categories[i];
            int status = getCategoryStatus(category);
            builder.addOption(
                    TextManager.getString(getLocale(), TextManager.COMMANDS, category.getId()),
                    String.valueOf(i),
                    Emoji.fromUnicode(getUnicodeEmojiFromStatus(status))
            );
            if (category == currentCategory) {
                builder.setDefaultValues(List.of(String.valueOf(i)));
            }
        }
        return builder.build();
    }

    private void turnOnAllCategoryCommands() {
        commandManagementBean.getSwitchedOffElements().removeIf(element -> {
            Class<? extends Command> clazz = CommandContainer.getCommandMap().get(element);
            if (clazz == null) return false;
            return CommandManager.createCommandByClass(clazz, getLocale(), getPrefix()).getCategory().equals(category);
        });
    }

    private int getCategoryStatus(Category category) {
        boolean hasOn = false, hasOff = false;

        if (!commandManagementBean.getSwitchedOffElements().contains(category.getId())) {
            for (Class<? extends Command> clazz : CommandContainer.getCommandCategoryMap().get(category)) {
                Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
                if (!hasOn && commandManagementBean.commandIsTurnedOn(command)) {
                    hasOn = true;
                } else if (!hasOff && !commandManagementBean.commandIsTurnedOn(command)) {
                    hasOff = true;
                }
            }
        }

        return hasOn ? (hasOff ? 1 : 2) : 0;
    }

    private String getUnicodeEmojiFromStatus(int status) {
        return switch (status) {
            case 0 -> Emojis.X;
            case 1 -> "❔";
            default -> "☑️";
        };
    }

    private ButtonStyle getButtonStyleFromStatus(int status) {
        return switch (status) {
            case 0, 1 -> ButtonStyle.SECONDARY;
            default -> ButtonStyle.SUCCESS;
        };
    }

}
