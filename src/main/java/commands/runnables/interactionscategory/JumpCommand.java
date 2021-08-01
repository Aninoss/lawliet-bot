package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "jump",
        emoji = "\uD83D\uDCA8",
        executableWithoutArgs = true,
        requiresMemberCache = true
)
public class JumpCommand extends RolePlayAbstract {

    public JumpCommand(Locale locale, String prefix) {
        super(locale, prefix, false,
                "https://cdn.discordapp.com/attachments/834509030110003300/834509331642712074/jump.gif",
                "https://cdn.discordapp.com/attachments/834509030110003300/834509345584447518/jump.gif",
                "https://cdn.discordapp.com/attachments/834509030110003300/834509359094169621/jump.gif",
                "https://cdn.discordapp.com/attachments/834509030110003300/834509372553035806/jump.gif",
                "https://cdn.discordapp.com/attachments/834509030110003300/834509387619237900/jump.gif",
                "https://cdn.discordapp.com/attachments/834509030110003300/834509401423216670/jump.gif",
                "https://cdn.discordapp.com/attachments/834509030110003300/834509413934563328/jump.gif",
                "https://cdn.discordapp.com/attachments/834509030110003300/834509425452253254/jump.gif",
                "https://cdn.discordapp.com/attachments/834509030110003300/834509436521414756/jump.gif",
                "https://cdn.discordapp.com/attachments/834509030110003300/834509447429881876/jump.gif",
                "https://cdn.discordapp.com/attachments/834509030110003300/834509462760194108/jump.gif",
                "https://cdn.discordapp.com/attachments/834509030110003300/834509475452813349/jump.gif",
                "https://cdn.discordapp.com/attachments/834509030110003300/834509486927773726/jump.gif",
                "https://cdn.discordapp.com/attachments/834509030110003300/834509502312480838/jump.gif",
                "https://cdn.discordapp.com/attachments/834509030110003300/834509518033125416/jump.gif",
                "https://cdn.discordapp.com/attachments/834509030110003300/834509533853515856/jump.gif",
                "https://cdn.discordapp.com/attachments/834509030110003300/834509549179240458/jump.gif",
                "https://cdn.discordapp.com/attachments/834509030110003300/834509562987413522/jump.gif",
                "https://cdn.discordapp.com/attachments/834509030110003300/834509577025355806/jump.gif",
                "https://cdn.discordapp.com/attachments/834509030110003300/834509592561975296/jump.gif"
        );
    }

}
