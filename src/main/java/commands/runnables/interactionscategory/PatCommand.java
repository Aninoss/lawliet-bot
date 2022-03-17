package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "pat",
        emoji = "\uD83E\uDD1A",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "praise", "yaoipat", "yuripat" }
)
public class PatCommand extends RolePlayAbstract {

    public PatCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true,
                "https://cdn.discordapp.com/attachments/736275536317382717/736275542193733702/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275544949260368/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275548061302886/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275551500763186/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275553941848176/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275564264161350/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275566885601391/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275577140543569/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275590251937872/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275593615769640/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275599550578778/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275602939576380/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275607645847572/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275612804579328/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275622304677908/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275627279122502/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275637765144716/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275648313688124/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275656794701904/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275665401413632/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275674821689404/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275680597377064/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275700214136892/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275702793371654/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275705926778895/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275710263558225/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275713761476668/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275763661242478/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/736275767389978676/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/741227933955522570/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/772095989560639509/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/774620244597407755/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/816317244048277536/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834743417765908/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834756721180682/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834770239160370/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834785276002364/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834799951609886/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834813315317800/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834825408020571/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834837562195978/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834849062846494/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834862556053534/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834874576928821/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834888242495488/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834899860455504/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834914121744424/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834928117088276/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834941078274119/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834957557170216/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834970291863582/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834981780062258/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834834996371259423/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834835007772164226/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834835022044332052/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834835034618855454/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834835047940358234/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834835061823111259/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834835074746548224/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834835087224209448/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834835098473070652/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834835110205063278/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834835123949535252/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/834835137807646720/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/896118127451070514/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/938229898726760458/pat.gif",
                "https://cdn.discordapp.com/attachments/736275536317382717/954006748858040350/pat.gif"
        );
    }

}
