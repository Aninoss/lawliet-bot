package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "sleep",
        emoji = "\uD83D\uDCA4",
        executableWithoutArgs = true
)
public class SleepCommand extends RolePlayAbstract {

    public SleepCommand(Locale locale, String prefix) {
        super(locale, prefix, false);

        setFtaGifs(
                "https://cdn.discordapp.com/attachments/736261025166524498/736261044783153222/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261050495926303/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261070666203158/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261078563946618/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261089083392020/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261105264885770/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261126576144394/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261138173526076/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261144829755452/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261150777540708/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261165151420486/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261210546372688/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261216426524753/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261224064483338/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261237263826964/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261246642552912/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261278250565662/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261283615342672/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261314514780230/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261433293144085/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838064702554112/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838157203603496/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838167886626846/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838180778868779/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838193835606016/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838206079303771/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838218721329212/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838256935108672/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838270260019230/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838281291038780/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838307082862613/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838320945037342/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838347734057060/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838361859686471/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838413634306078/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838428717416468/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838443477303316/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838458077675580/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838472006696960/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838504341110784/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838537467461662/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838551332913172/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838571981996114/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838584820629544/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838596102914108/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838608724230154/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838622884462642/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838647203299429/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838669801816184/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838680869797908/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838704639967282/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838717574545438/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838728525479956/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838739850100798/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/881899546731479140/sleep.gif"
        );
        setMtaGifs(
                "https://cdn.discordapp.com/attachments/736261025166524498/736261034935058563/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261177361039462/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261183094521856/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261270105358396/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261293211648110/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261303915642880/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261332067942400/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838080115441674/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838092778176512/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838105830981642/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838132175536128/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838145089667132/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838231144988742/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838244017307659/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838400732364890/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838519906172968/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838658128674906/sleep.gif"
        );
        setAtaGifs(
                "https://cdn.discordapp.com/attachments/736261025166524498/736261058494333008/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261117239885854/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/736261135648555098/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838334265491466/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838375834976327/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838486175055882/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/834838635357667328/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/881899944871592026/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/904068241566093362/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/925455660584411146/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/925455784698085426/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/925455887475290172/sleep.gif",
                "https://cdn.discordapp.com/attachments/736261025166524498/925456007658881044/sleep.gif"
        );
    }

}
