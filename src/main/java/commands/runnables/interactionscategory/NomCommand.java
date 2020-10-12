package commands.runnables.interactionscategory;
import commands.listeners.CommandProperties;
import commands.runnables.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "nom",
        emoji = "\uD83E\uDD62",
        executableWithoutArgs = true,
        aliases = {"eat"}
)
public class NomCommand extends InteractionAbstract {

    public NomCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736273366784278558/736273374858182757/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273380373954730/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273386904485951/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273394412159106/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273405405560892/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273410484731954/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273419229855844/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273427224330270/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273437730930778/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273443191914597/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273448375943258/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273452323045466/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273460975894548/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273463857381376/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273472153714719/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273475823468654/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273478596034650/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273489387978792/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273496098865293/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273501094281256/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273507494789150/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273511827505182/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273518135738368/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273530110476409/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273535105892372/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273537450508318/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273541296816178/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273545809756226/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273550746451988/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273553388732436/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273556052115496/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273564315025599/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273567125340250/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273569201389638/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273575379730490/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273588960755782/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273602902622278/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273607927529542/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273613703086150/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273616211017769/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273620275298434/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273624427659284/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273630614519898/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273635941023764/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273640248574063/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273650537463918/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273655889133699/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273665213333544/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273672775663616/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273681994612866/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736273689569394688/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/736646632791081020/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/739030221096747069/nom.gif",
                "https://media.discordapp.net/attachments/736273366784278558/755840757901820054/nom.gif"
        };
    }

}
