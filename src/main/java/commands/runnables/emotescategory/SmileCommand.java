package commands.runnables.emotescategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.EmoteAbstract;

@CommandProperties(
        trigger = "smile",
        emoji = "\uD83D\uDE04",
        executableWithoutArgs = true,
        aliases = { "happy" }
)
public class SmileCommand extends EmoteAbstract {

    public SmileCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[] {
                "https://cdn.discordapp.com/attachments/736256980020101160/736256987121188924/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736256993723023430/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736256999389397047/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257003818713198/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257012870021231/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257019052425266/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257024521928806/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257030448480265/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257032654684180/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257038002290728/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257041190092800/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257043983499264/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257049079316550/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257055605784676/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257060391354368/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257064053243985/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257068087902310/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257073754669166/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257082977812530/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257088401178706/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257091492380702/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257099679400097/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257111196958780/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257116511141948/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257121120681991/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257126615351396/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257134488059938/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257144374034472/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257149281370283/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257153475543201/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257160853454848/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257168273178725/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257173595881482/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257185771815052/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257193539665953/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257197532643328/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257204767817778/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257208488034385/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257214565843066/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257218650832967/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257226980982855/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257234643976283/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257238825697361/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257246014603314/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257250280210472/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257255997177936/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257261684523018/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257267485245591/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257276335358012/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257281997406238/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257290201464922/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257294139916418/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257304453840916/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257313190445107/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257318769000588/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257328088612944/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257349651660870/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257352902377482/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257359520858195/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257367011753984/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257380240719882/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257396166361088/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257406345936906/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257415825195108/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257425551655002/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257434217218108/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257443532636241/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257451543756810/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257458594512976/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257463141007452/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736257468317040680/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736258026385702973/happy.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736258137559924806/happy.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/736258306334654604/happy.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/739030111600509018/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/743053465261179002/smile.gif",
                "https://cdn.discordapp.com/attachments/736256980020101160/755840479375131064/smile.gif"
        };
    }

}
