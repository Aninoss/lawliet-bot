package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "stare",
        emoji = "\uD83D\uDC40",
        executableWithoutArgs = true,
        aliases = { "see" }
)
public class StareCommand extends RolePlayAbstract {

    public StareCommand(Locale locale, String prefix) {
        super(locale, prefix, false);

        setFtaGifs(
                "https://cdn.discordapp.com/attachments/736260687633842216/736260702947508244/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260707611312288/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260716276875354/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260731065991278/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260774288162957/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260786942509076/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260805212766288/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260815098871828/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260832987578398/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260838255755294/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260861676486726/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260870103105617/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260884573323394/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260888947851304/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260891879931964/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260905527934986/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260910791917690/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260936528166963/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834842787513368646/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834842860666224741/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834842954874748948/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834842966858006578/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834842979768205342/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843045517590548/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843085501890640/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843109099438170/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843194927087729/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843216678486066/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843227865612358/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843239940620308/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843254322888814/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843305925279814/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843320487510076/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843331036577843/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843344143908904/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843366747144192/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843392604241960/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843416515838032/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843445624569956/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843460611604500/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843476478263366/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/881902518102270002/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/887273588191789086/stare.gif"
        );
        setMtaGifs(
                "https://cdn.discordapp.com/attachments/736260687633842216/736260725416263691/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260742839271424/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260754600362054/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260765467672756/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260792239915088/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260809755328573/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260822317137940/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260849764794508/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260901484625950/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260917704261812/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260921600508005/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/736260930098298931/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834842771629146122/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834842801942429757/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834842817250852884/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834842832446423110/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834842846355259422/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834842874095861780/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834842887279607808/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834842900277100594/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834842930560499802/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834842942854004766/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834842992724279357/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843006162042934/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843020129468476/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843033262358580/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843060931264522/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843073506050108/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843097237422080/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843120784244766/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843132561588254/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843144029863936/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843155659751515/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843168494977054/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843183686090762/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843205538545704/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843267774283776/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843281778671715/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843294831083570/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843355544158248/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843378658050058/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843404537561168/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/834843430449315901/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/881902019202400286/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/881902152551890944/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/881902240707788840/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/881902309519528006/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/881902383024734288/stare.gif",
                "https://cdn.discordapp.com/attachments/736260687633842216/881902458861932575/stare.gif"
        );
        setAtaGifs(
                "https://cdn.discordapp.com/attachments/736260687633842216/834842916132356146/stare.gif"
        );
    }

}
