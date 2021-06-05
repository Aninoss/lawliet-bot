package commands.runnables.configurationcategory;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.Category;
import constants.LogStatus;
import constants.Response;
import core.EmbedFactory;
import core.TextManager;
import mysql.modules.commandmanagement.CommandManagementData;
import mysql.modules.commandmanagement.DBCommandManagement;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "cman",
        userGuildPermissions = Permission.ADMINISTRATOR,
        emoji = "🚦",
        executableWithoutArgs = true,
        aliases = { "commandmanagement", "cmanagement", "cm", "commandmanagements", "commandmanager", "commandm", "comman" }
)
public class CommandManagementCommand extends NavigationAbstract {

    private CommandManagementData commandManagementBean;
    private String category;

    public CommandManagementCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        commandManagementBean = DBCommandManagement.getInstance().retrieve(event.getGuild().getIdLong());
        registerNavigationListener();
        return true;
    }

    @Override
    public Response controllerMessage(GuildMessageReceivedEvent event, String input, int state) {
        return null;
    }

    @Override
    public boolean controllerButton(ButtonClickEvent event, int i, int state) throws Throwable {
        switch (state) {
            case 0:
                if (i == -1) {
                    deregisterListenersWithButtonMessage();
                    return false;
                } else if (i >= 0 && i < Category.LIST.length) {
                    category = Category.LIST[i];
                    setState(1);
                    return true;
                }
                return false;

            case 1:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        turnOnAllCategoryCommands();
                        commandManagementBean.getSwitchedOffElements().remove(category);
                        setLog(LogStatus.SUCCESS, getString("categoryset_all", true, TextManager.getString(getLocale(), TextManager.COMMANDS, category)));
                        return true;

                    case 1:
                        setState(2);
                        return true;

                    case 2:
                        turnOnAllCategoryCommands();
                        commandManagementBean.getSwitchedOffElements().add(category);
                        setLog(LogStatus.SUCCESS, getString("categoryset_all", false, TextManager.getString(getLocale(), TextManager.COMMANDS, category)));
                        return true;

                    default:
                        return false;
                }

            case 2:
                List<Command> commandList = CommandContainer.getInstance().getCommandCategoryMap().get(category).stream()
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
                        if (commandManagementBean.getSwitchedOffElements().contains(command.getCategory())) {
                            commandManagementBean.getSwitchedOffElements().remove(command.getCategory());
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

    private void turnOnAllCategoryCommands() {
        commandManagementBean.getSwitchedOffElements().removeIf(element -> {
            Class<? extends Command> clazz = CommandContainer.getInstance().getCommandMap().get(element);
            if (clazz == null) return false;
            return CommandManager.createCommandByClass(clazz, getLocale(), getPrefix()).getCategory().equals(category);
        });
    }

    private int getCategoryStatus(String category) {
        boolean hasOn = false, hasOff = false;

        if (!commandManagementBean.getSwitchedOffElements().contains(category)) {
            for (Class<? extends Command> clazz : CommandContainer.getInstance().getCommandCategoryMap().get(category)) {
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

    @Override
    public EmbedBuilder draw(int state) {
        switch (state) {
            case 0:
                String[] options = Arrays.stream(Category.LIST)
                        .map(id -> {
                            String name = TextManager.getString(getLocale(), TextManager.COMMANDS, id);
                            return getString("category", getCategoryStatus(id), name);
                        })
                        .filter(Objects::nonNull)
                        .toArray(String[]::new);
                setOptions(options);
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"));

            case 1:
                setOptions(getString("state1_options").split("\n"));
                String categoryName = TextManager.getString(getLocale(), TextManager.COMMANDS, category);
                return EmbedFactory.getEmbedDefault(this, getString("state1_description", getCategoryStatus(category), categoryName));

            case 2:
                options = CommandContainer.getInstance().getCommandCategoryMap().get(category).stream()
                        .map(clazz -> CommandManager.createCommandByClass(clazz, getLocale(), getPrefix()))
                        .map(command -> getString("command", commandManagementBean.commandIsTurnedOn(command), command.getTrigger(), TextManager.getString(getLocale(), command.getCategory(), command.getTrigger() + "_title")))
                        .toArray(String[]::new);
                setOptions(options);
                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));

            default:
                return null;
        }
    }

}
