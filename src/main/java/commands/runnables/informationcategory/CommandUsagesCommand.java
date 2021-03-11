package commands.runnables.informationcategory;

import java.util.ArrayList;
import java.util.Locale;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.CommandProperties;
import commands.runnables.ListAbstract;
import core.utils.StringUtil;
import javafx.util.Pair;
import mysql.modules.commandusages.CommandUsagesBean;
import mysql.modules.commandusages.DBCommandUsages;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "commandusages",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83D\uDCD3",
        executableWithoutArgs = true,
        onlyPublicVersion = true,
        aliases = {"cu", "commandusage"}
)
public class CommandUsagesCommand extends ListAbstract {

    private final ArrayList<Pair<CommandUsagesBean, String>> commandUsages = new ArrayList<>();

    public CommandUsagesCommand(Locale locale, String prefix) {
        super(locale, prefix, 10);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        for(Class<? extends Command> clazz: CommandContainer.getInstance().getFullCommandList()) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            commandUsages.add(new Pair<>(DBCommandUsages.getInstance().retrieve(command.getTrigger()), command.getCommandProperties().emoji()));
        }

        commandUsages.sort((a0, a1) -> Long.compare(a1.getKey().getValue(), a0.getKey().getValue()));

        registerList(commandUsages.size(), args);
        return true;
    }

    @Override
    protected Pair<String, String> getEntry(int i) throws Throwable {
        Pair<CommandUsagesBean, String> commandUsagesPair = commandUsages.get(i);

        return new Pair<>(
                getString("slot_title", commandUsagesPair.getKey().getCommand(), commandUsagesPair.getValue()),
                getString("slot_desc", StringUtil.numToString(commandUsagesPair.getKey().getValue()))
        );
    }

}