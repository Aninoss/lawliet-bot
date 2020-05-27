package Commands.InteractionsCategory;

import CommandListeners.CommandProperties;
import Commands.InteractionAbstract;

@CommandProperties(
    trigger = "spank",
    emoji = "\uD83C\uDF51",
    executable = false
)
public class SpankCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://cdn.discordapp.com/attachments/499629904380297226/710186716396519515/0a9.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/710186715851259954/LIRChWS.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/710186711942037627/tenor_12.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/710186711790911588/tenor_4.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/710186710780215448/tenor_11.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/710186709559542374/tenor_8.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/710186709714731129/tenor_10.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/710186708800635070/d18733e5e1f78279ada838161a29f404e152f70f_hq.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/710186707349274724/anime-spanking-gif-3.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/710186703297445888/1230464402072.gif",
                "https://media1.tenor.com/images/6f5c1f380b4cb313f412f57f4508c7e9/tenor.gif?itemid=12409765",
                "https://media1.tenor.com/images/35c1ecae2168c49be997871adc2a5d75/tenor.gif"
        };
    }

}
