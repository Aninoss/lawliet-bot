package commands.runnables.informationcategory;

import commands.Command;
import commands.CommandContainer;
import commands.CommandEvent;
import commands.CommandManager;
import commands.listeners.CommandProperties;
import commands.runnables.ListAbstract;
import core.utils.StringUtil;
import javafx.util.Pair;
import mysql.modules.commandusages.CommandUsagesData;
import mysql.modules.commandusages.DBCommandUsages;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Locale;

@CommandProperties(
        trigger = "commandusages",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83D\uDCD3",
        executableWithoutArgs = true,
        onlyPublicVersion = true,
        usesExtEmotes = true,
        aliases = { "cu", "commandusage" }
)
public class CommandUsagesCommand extends ListAbstract {

    private final ArrayList<Pair<CommandUsagesData, String>> commandUsages = new ArrayList<>();

    public CommandUsagesCommand(Locale locale, String prefix) {
        super(locale, prefix, 10);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        registerList(event.getMember(), args);
        return true;
    }

    @Override
    protected int configure(Member member, int orderBy) throws Throwable {
        for (Class<? extends Command> clazz : CommandContainer.getFullCommandList()) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            commandUsages.add(new Pair<>(DBCommandUsages.getInstance().retrieve(command.getTrigger()), command.getCommandProperties().emoji()));
        }
        commandUsages.sort((a0, a1) -> Long.compare(a1.getKey().getValue(), a0.getKey().getValue()));
        return commandUsages.size();
    }

    @Override
    protected Pair<String, String> getEntry(Member member, int i, int orderBy) throws Throwable {
        Pair<CommandUsagesData, String> commandUsagesPair = commandUsages.get(i);
        return new Pair<>(
                getString("slot_title", commandUsagesPair.getKey().getCommand(), commandUsagesPair.getValue()),
                getString("slot_desc", StringUtil.numToString(commandUsagesPair.getKey().getValue()))
        );
    }

}