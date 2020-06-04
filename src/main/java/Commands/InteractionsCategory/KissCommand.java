package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;
import Commands.InteractionAbstract;

import java.util.ArrayList;

@CommandProperties(
        trigger = "kiss",
        emoji = "\uD83D\uDC8B",
        executable = true
)
public class KissCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media.giphy.com/media/KH1CTZtw1iP3W/giphy.gif",
                "https://media1.tenor.com/images/78095c007974aceb72b91aeb7ee54a71/tenor.gif?itemid=5095865",
                "https://media1.tenor.com/images/f102a57842e7325873dd980327d39b39/tenor.gif?itemid=12392648",
                "https://media1.tenor.com/images/f5167c56b1cca2814f9eca99c4f4fab8/tenor.gif?itemid=6155657",
                "https://media1.tenor.com/images/68a37a5a1b86f227b8e1169f33a6a6bb/tenor.gif?itemid=13344389",
                "https://media1.tenor.com/images/ea9a07318bd8400fbfbd658e9f5ecd5d/tenor.gif?itemid=12612515",
                "https://media1.tenor.com/images/02d9cae34993e48ab5bb27763d5ca2fa/tenor.gif?itemid=4874618",
                "https://media1.tenor.com/images/7fd98defeb5fd901afe6ace0dffce96e/tenor.gif?itemid=9670722",
                "https://media1.tenor.com/images/bc5e143ab33084961904240f431ca0b1/tenor.gif?itemid=9838409",
                "https://media1.tenor.com/images/4b5d5afd747fe053ed79317628aac106/tenor.gif?itemid=5649376",
                "https://media1.tenor.com/images/e76e640bbbd4161345f551bb42e6eb13/tenor.gif?itemid=4829336",
                "https://media1.tenor.com/images/daf7b144c7caceee3d90dca791a4c790/tenor.gif?itemid=7572438",
                "https://media1.tenor.com/images/621ceac89636fc46ecaf81824f9fee0e/tenor.gif?itemid=4958649",
                "https://media1.tenor.com/images/ef4a0bcb6e42189dc12ee55e0d479c54/tenor.gif?itemid=12143127",
                "https://media1.tenor.com/images/933632688c082ad6b67506c392e7648c/tenor.gif?itemid=12922778",
                "https://media1.tenor.com/images/9fac3eab2f619789b88fdf9aa5ca7b8f/tenor.gif?itemid=12925177",
                "https://media1.tenor.com/images/a1f7d43752168b3c1dbdfb925bda8a33/tenor.gif?itemid=10356314",
                "https://media1.tenor.com/images/693602b39a071644cebebdce7c459142/tenor.gif?itemid=6206552",
                "https://media1.tenor.com/images/e4fcb11bc3f6585ecc70276cc325aa1c/tenor.gif?itemid=7386341",
                "https://media1.tenor.com/images/b8d0152fbe9ecc061f9ad7ff74533396/tenor.gif?itemid=5372258",
                "https://media1.tenor.com/images/558f63303a303abfdddaa71dc7b3d6ae/tenor.gif?itemid=12879850",
                "https://media1.tenor.com/images/2182d81bc459732fdf9bf94d1dd068c4/tenor.gif?itemid=6155634",
                "https://media1.tenor.com/images/8cab4f4c73547d077c56066461c40a5e/tenor.gif?itemid=12873196",
                "https://media1.tenor.com/images/a390476cc2773898ae75090429fb1d3b/tenor.gif?itemid=12837192",
                "https://media1.tenor.com/images/d9115cb8f24162cf70428d8cb8d96558/tenor.gif?itemid=9382690",
                "https://media1.tenor.com/images/6fb9420ec2d12fb099e109b52a233eac/tenor.gif?itemid=13327149",
                "https://media1.tenor.com/images/df692538bbf513f7bd94709435e96342/tenor.gif?itemid=10358839",
                "https://media1.tenor.com/images/896519dafbd82b9b924b575e3076708d/tenor.gif?itemid=8811697",
                "https://media1.tenor.com/images/37633f0b8d39daf70a50f69293e303fc/tenor.gif?itemid=13344412",
                "https://media1.tenor.com/images/89dc8a31006407943db4cb2ed7a37651/tenor.gif?itemid=3556102",
                "https://media1.tenor.com/images/5654c7b35e067553e99bb996535c0a75/tenor.gif?itemid=10358833",
                "https://media1.tenor.com/images/0be6297b653edf561c5810ec547a9802/tenor.gif?itemid=6203355",
                "https://media1.tenor.com/images/af1216d35f8ec076b593401b19ddd0bf/tenor.gif?itemid=13188942",
                "https://media1.tenor.com/images/ea51c3a083c73bf81a3c5ee6d4165115/tenor.gif?itemid=11794176",
                "https://media1.tenor.com/images/1306732d3351afe642c9a7f6d46f548e/tenor.gif?itemid=6155670",
                "https://pa1.narvii.com/6802/50c2e4dd354f2957a38f080f5c97b294575a941a_hq.gif",
                "https://data.whicdn.com/images/311865675/original.gif",
                "https://i.imgur.com/xH3g4nQ.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709435126697820180/nice.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709435126219669584/hajimexyue.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709435125573746688/1501599674_tumblr_o5zzj8dcT61sfel8wo1_r3_500.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709435121840947260/kiss.gif",
                "https://data.whicdn.com/images/187675545/original.gif"
        };
    }

}
