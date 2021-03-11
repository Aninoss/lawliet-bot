package commands.runnables.emotescategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.EmoteAbstract;

@CommandProperties(
        trigger = "angry",
        emoji = "\uD83D\uDCA2",
        executableWithoutArgs = true,
        aliases = { "rage", "mad" }
)
public class AngryCommand extends EmoteAbstract {

    public AngryCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[] {
                "https://media.discordapp.net/attachments/736258706383175720/736258715128299620/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258717447880874/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258727711211600/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258731771166760/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258738112954458/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258744857657354/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258747059404840/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258759726203010/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258768211279962/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258773491908728/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258777413845072/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258782253809834/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258785206861864/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258791414169620/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258800821993472/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258808606883880/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258811819720865/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258818601779361/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258826109452359/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258830651883588/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258837891252274/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258842047938589/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258846586306590/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258849463599104/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258854009962526/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258863497478244/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258873782042704/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258887140769792/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258904404656159/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258908997419068/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258918317162578/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258922326917150/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258933739749467/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258938382712842/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258942451318867/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258960599941219/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258974235754576/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258982913507488/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736258988232146944/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736259236425760778/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736259006410260560/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736259311432499260/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736259025141891142/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/736259035447296100/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/745278166981017620/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/772196563313360896/angry.gif",
                "https://media.discordapp.net/attachments/736258706383175720/772196703045550125/angry.gif"
        };
    }

}
