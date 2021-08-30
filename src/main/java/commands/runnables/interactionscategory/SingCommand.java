package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "sing",
        emoji = "\uD83C\uDFA4",
        executableWithoutArgs = true
)
public class SingCommand extends RolePlayAbstract {

    public SingCommand(Locale locale, String prefix) {
        super(locale, prefix, false,
                "https://cdn.discordapp.com/attachments/874742187357773844/874742229535699044/sing.gif",
                "https://cdn.discordapp.com/attachments/874742187357773844/874742322699583538/sing.gif",
                "https://cdn.discordapp.com/attachments/874742187357773844/874742401061756928/sing.gif",
                "https://cdn.discordapp.com/attachments/874742187357773844/874742504237436959/sing.gif",
                "https://cdn.discordapp.com/attachments/874742187357773844/874742590652706918/sing.gif",
                "https://cdn.discordapp.com/attachments/874742187357773844/874742690829443132/sing.gif",
                "https://cdn.discordapp.com/attachments/874742187357773844/874742765756481576/sing.gif",
                "https://cdn.discordapp.com/attachments/874742187357773844/874742863513157662/sing.gif",
                "https://cdn.discordapp.com/attachments/874742187357773844/874742945864110120/sing.gif",
                "https://cdn.discordapp.com/attachments/874742187357773844/874743043486543872/sing.gif",
                "https://cdn.discordapp.com/attachments/874742187357773844/881900344035123270/sing.gif",
                "https://cdn.discordapp.com/attachments/874742187357773844/881900424075030528/sing.gif"
        );
    }

}
