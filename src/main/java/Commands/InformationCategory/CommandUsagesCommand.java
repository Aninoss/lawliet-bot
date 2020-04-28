package Commands.InformationCategory;

import CommandListeners.CommandProperties;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import CommandSupporters.CommandManager;
import Commands.ListAbstract;
import Commands.ManagementCategory.CommandManagementCommand;
import Constants.FisheryStatus;
import Constants.Permission;
import Core.DiscordApiCollection;
import Core.EmbedFactory;
import Core.TextManager;
import Core.Utils.StringUtil;
import MySQL.Modules.CommandUsages.CommandUsagesBean;
import MySQL.Modules.CommandUsages.DBCommandUsages;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryUserBean;
import MySQL.Modules.Server.DBServer;
import javafx.util.Pair;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

@CommandProperties(
        trigger = "commandusages",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat/128/note-icon.png",
        emoji = "\uD83D\uDCD3",
        executable = true,
        aliases = {"cu", "commandusage"}
)
public class CommandUsagesCommand extends ListAbstract {

    private final ArrayList<Pair<CommandUsagesBean, String>> commandUsages = new ArrayList<>();

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        for(Class<? extends Command> clazz: CommandContainer.getInstance().getCommandList()) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            commandUsages.add(new Pair<>(DBCommandUsages.getInstance().getBean(command.getTrigger()), command.getEmoji()));
        }

        commandUsages.sort((a0, a1) -> Long.compare(a1.getKey().getValue(), a0.getKey().getValue()));

        init(event.getServerTextChannel().get(), followedString);
        return true;
    }

    protected Pair<String, String> getEntry(ServerTextChannel channel, int i) throws Throwable {
        Pair<CommandUsagesBean, String> commandUsagesPair = commandUsages.get(i);

        return new Pair<>(
                getString("slot_title", commandUsagesPair.getKey().getCommand(), commandUsagesPair.getValue()),
                getString("slot_desc", StringUtil.numToString(commandUsagesPair.getKey().getValue()))
        );
    }

    protected int getSize() {
        return commandUsages.size();
    }

    protected int getEntriesPerPage() { return 10; }

}