package Commands.EmotesCategory;
import CommandListeners.CommandProperties;
import Commands.EmoteAbstract;

@CommandProperties(
        trigger = "facepalm",
        emoji = "\uD83E\uDD26",
        executable = true
)
public class FacepalmCommand extends EmoteAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/480cdeb59d3d5d50dd206283a944b8e1/tenor.gif?itemid=16327659",
                "https://media1.tenor.com/images/5e7f44432181df2ba18f27b9f078545f/tenor.gif?itemid=5897489",
                "https://media1.tenor.com/images/04ce28c62c8cfeb102b3ac2a9bf28050/tenor.gif?itemid=12411417",
                "https://media1.tenor.com/images/12774c8dabc004bd4914a730d42b5134/tenor.gif?itemid=15256888",
                "https://media1.tenor.com/images/fce5aa9f4825a2adabfc9c91686167bc/tenor.gif?itemid=16842960",
                "https://media1.tenor.com/images/b8e234ac4aa6aa64b582895911de2046/tenor.gif?itemid=12411488",
                "https://media1.tenor.com/images/142d74bbd13fc305aed5a4894c0c3f7f/tenor.gif?itemid=16642818",
                "https://media1.tenor.com/images/be96db9b9acfd04fd2f5d890e2c51781/tenor.gif?itemid=14355381",
                "https://media1.tenor.com/images/76d2ec47ec76fa36b2fce913331ba7e3/tenor.gif?itemid=5533025",
                "https://media1.tenor.com/images/5e29a1db9149211728b22bfd01f88771/tenor.gif?itemid=10336271",
                "https://cdn.discordapp.com/attachments/499629904380297226/709431036953624606/Unbekannt.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709431038975279175/kyon.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709431040405405766/anime-facepalm-gif-10.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709431042829844480/P0ojXUp.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709431045367529512/WIdO2WZ.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709431048613920868/anime-facepalm-gif-2.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709431048496218192/lAXh93V.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709431050207756288/f64710e361b0115384252546a81c4438.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709431050459283456/bqRkOZM.gif"
        };
    }

}
