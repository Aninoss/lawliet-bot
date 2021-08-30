package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "assgrab",
        emoji = "\uD83C\uDF51️",
        executableWithoutArgs = true,
        nsfw = true,
        requiresFullMemberCache = true,
        aliases = "grabass"
)
public class AssGrabCommand extends RolePlayAbstract {

    public AssGrabCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/834460432818241566/834461925894127651/assgrab.gif",
                "https://cdn.discordapp.com/attachments/834460432818241566/834461937664000040/assgrab.gif",
                "https://cdn.discordapp.com/attachments/834460432818241566/834461948993208340/assgrab.gif",
                "https://cdn.discordapp.com/attachments/834460432818241566/834461961277931610/assgrab.gif",
                "https://cdn.discordapp.com/attachments/834460432818241566/834461976402460732/assgrab.gif",
                "https://cdn.discordapp.com/attachments/834460432818241566/834461989321441300/assgrab.gif",
                "https://cdn.discordapp.com/attachments/834460432818241566/834462003274711077/assgrab.gif",
                "https://cdn.discordapp.com/attachments/834460432818241566/834462015236997191/assgrab.gif",
                "https://cdn.discordapp.com/attachments/834460432818241566/834462028906233866/assgrab.gif",
                "https://cdn.discordapp.com/attachments/834460432818241566/834462042706542643/assgrab.gif",
                "https://cdn.discordapp.com/attachments/834460432818241566/881905588206968832/assgrab.gif"
        );
    }

}
