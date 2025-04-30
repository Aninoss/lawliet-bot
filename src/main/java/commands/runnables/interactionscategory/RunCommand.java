package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "run",
        emoji = "\uD83C\uDFC3",
        executableWithoutArgs = true
)
public class RunCommand extends RolePlayAbstract {

    public RunCommand(Locale locale, String prefix) {
        super(locale, prefix, false);

        setFtaGifs(
                "https://cdn.discordapp.com/attachments/736259684763435059/736259690962485268/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259702135980144/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259705567182848/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259723288117369/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259754514579537/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259776480149544/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259782364758128/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259787804639351/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259801151045702/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259848282308628/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259867735621683/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259884076499074/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259890531532810/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259984127557692/run.gif"
        );
        setMtaGifs(
                "https://cdn.discordapp.com/attachments/736259684763435059/736259694955331694/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259715662741504/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259730497994792/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259740811788318/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259744276414534/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259758641905724/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259857266638899/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259922651512865/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259931270807592/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259953446092921/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259959783948358/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259968616890428/run.gif"
        );
        setAtaGifs(
                "https://cdn.discordapp.com/attachments/736259684763435059/736259792263184515/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259811766829116/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259833338003456/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259873305526353/run.gif",
                "https://cdn.discordapp.com/attachments/736259684763435059/736259900216311848/run.gif"
        );
    }

}
