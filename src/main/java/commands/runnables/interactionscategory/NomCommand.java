package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "nom",
        emoji = "\uD83E\uDD62",
        executableWithoutArgs = true,
        aliases = { "eat", "hunger", "hungry" }
)
public class NomCommand extends RolePlayAbstract {

    public NomCommand(Locale locale, String prefix) {
        super(locale, prefix, false);

        setFtaGifs(
                "https://cdn.discordapp.com/attachments/736273366784278558/736273374858182757/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273380373954730/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273394412159106/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273405405560892/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273427224330270/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273437730930778/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273443191914597/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273448375943258/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273463857381376/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273472153714719/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273478596034650/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273496098865293/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273501094281256/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273507494789150/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273511827505182/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273537450508318/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273541296816178/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273545809756226/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273602902622278/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273607927529542/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273624427659284/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273635941023764/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273655889133699/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273665213333544/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273672775663616/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273681994612866/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736646632791081020/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/739030221096747069/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/755840757901820054/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833551115288576/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833563266449448/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833575647117349/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833624872124476/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833661030432818/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833673865003028/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833715531087932/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833770988568637/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833785068453968/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833799400783882/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833810671534080/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833825132445836/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833839427026954/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833868023529573/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833884004614264/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833901645463552/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833913700548658/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833928002863134/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833943559274566/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833956271554590/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833969153441862/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834009476562994/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834020340334602/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834031115239525/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834044461907988/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834058063511562/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834084705599528/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834095389409320/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834107217084476/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834118977519617/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834132055490640/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834146375368707/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834157611516024/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834180994105364/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834192403398676/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834207070617640/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834277480136764/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834311290159114/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/881895024793640971/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/1015215841211580426/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/1016328051992297502/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833525290565702/nom.gif"
        );
        setMtaGifs(
                "https://cdn.discordapp.com/attachments/736273366784278558/736273475823468654/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273489387978792/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273550746451988/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273553388732436/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273569201389638/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273575379730490/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273588960755782/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273613703086150/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273616211017769/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273620275298434/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273650537463918/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273689569394688/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833612070191144/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833637279137802/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833649949212722/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833687984078908/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833701925421066/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833743000502272/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833853897506816/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833981840425021/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833995070046208/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834072848171148/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834170072399913/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834270659936266/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834288502767616/nom.gif"
        );
        setAtaGifs(
                "https://cdn.discordapp.com/attachments/736273366784278558/736273410484731954/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273419229855844/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273452323045466/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273518135738368/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273556052115496/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273564315025599/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/736273567125340250/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/768163643451375647/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833587437568030/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833599345328169/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833729162969088/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834833757079994388/nom.gif",
                "https://cdn.discordapp.com/attachments/736273366784278558/834834299684519956/nom.gif"
        );
    }

}
