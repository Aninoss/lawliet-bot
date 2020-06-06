package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;

import Commands.InteractionAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "hug",
        emoji = "\uD83D\uDC50",
        executable = true
)
public class HugCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/6db54c4d6dad5f1f2863d878cfb2d8df/tenor.gif?itemid=7324587",
                "https://media1.tenor.com/images/d6510db0a868cfbff697d7279aa89b61/tenor.gif?itemid=10989534",
                "https://media.giphy.com/media/qscdhWs5o3yb6/giphy.gif",
                "https://media.giphy.com/media/wnsgren9NtITS/giphy.gif",
                "https://media.giphy.com/media/HaC1WdpkL3W00/giphy.gif",
                "https://media.giphy.com/media/kvKFM3UWg2P04/giphy.gif",
                "https://media.giphy.com/media/od5H3PmEG5EVq/giphy.gif",
                "https://media1.tenor.com/images/1069921ddcf38ff722125c8f65401c28/tenor.gif?itemid=11074788",
                "https://media1.tenor.com/images/e58eb2794ff1a12315665c28d5bc3f5e/tenor.gif?itemid=10195705",
                "https://media1.tenor.com/images/40aed63f5bc795ed7a980d0ad5c387f2/tenor.gif?itemid=11098589",
                "https://media1.tenor.com/images/074d69c5afcc89f3f879ca473e003af2/tenor.gif?itemid=4898650",
                "https://media1.tenor.com/images/7e30687977c5db417e8424979c0dfa99/tenor.gif?itemid=10522729",
                "https://media1.tenor.com/images/b0de026a12e20137a654b5e2e65e2aed/tenor.gif?itemid=7552093",
                "https://media1.tenor.com/images/b4ba20e6cb49d8f8bae81d86e45e4dcc/tenor.gif?itemid=5634582",
                "https://media1.tenor.com/images/42922e87b3ec288b11f59ba7f3cc6393/tenor.gif?itemid=5634630",
                "https://media1.tenor.com/images/44b4b9d5e6b4d806b6bcde2fd28a75ff/tenor.gif?itemid=9383138",
                "https://media1.tenor.com/images/11889c4c994c0634cfcedc8adba9dd6c/tenor.gif?itemid=5634578",
                "https://media1.tenor.com/images/49a21e182fcdfb3e96cc9d9421f8ee3f/tenor.gif?itemid=3532079",
                "https://media1.tenor.com/images/3c83525781dc1732171d414077114bc8/tenor.gif?itemid=7830142",
                "https://media1.tenor.com/images/d3dca2dec335e5707e668b2f9813fde5/tenor.gif?itemid=12668677",
                "https://media1.tenor.com/images/4ebdcd44de0042eb416345a50c3f80c7/tenor.gif?itemid=6155660",
                "https://media1.tenor.com/images/f2805f274471676c96aff2bc9fbedd70/tenor.gif?itemid=7552077",
                "https://media1.tenor.com/images/bb841fad2c0e549c38d8ae15f4ef1209/tenor.gif?itemid=10307432",
                "https://media1.tenor.com/images/4be3396644e87d3c201f8965104e57b7/tenor.gif?itemid=7539851",
                "https://media1.tenor.com/images/eb592ac3aff619edfa67dbc9b2ce216e/tenor.gif?itemid=5069970",
                "https://media1.tenor.com/images/460c80d4423b0ba75ed9592b05599592/tenor.gif?itemid=5044460",
                "https://media1.tenor.com/images/af76e9a0652575b414251b6490509a36/tenor.gif?itemid=5640885",
                "https://media1.tenor.com/images/79c461726e53ee8f9a5a36521f69d737/tenor.gif?itemid=13221416",
                "https://media1.tenor.com/images/b45619f5c109890d894eae5132dbd809/tenor.gif?itemid=14162538",
                "https://media1.tenor.com/images/24ac13447f9409d41c1aecb923aedf81/tenor.gif?itemid=5026057",
                "https://cdn.discordapp.com/attachments/499629904380297226/582632584748466186/hug-S1qhfy2cz-1.gif",
                "https://media1.tenor.com/images/3ee30e7a472efe430502d08b993dc79b/tenor.gif?itemid=12668673",
                "https://media1.tenor.com/images/4e9c3a6736d667bea00300721cff45ec/tenor.gif?itemid=14539121",
                "https://media1.tenor.com/images/b7487d45af7950bfb3f7027c93aa49b1/tenor.gif?itemid=9882931",
                "https://media1.tenor.com/images/aeb42019b0409b98aed663f35b613828/tenor.gif?itemid=14108949",
                "https://media1.tenor.com/images/71ed0a487f9fd98d18117c80ca518d18/tenor.gif?itemid=14566838",
                "https://media1.tenor.com/images/cff7e22303e99c24e46e5444a38e8018/tenor.gif?itemid=13135984",
                "https://media.giphy.com/media/143v0Z4767T15e/giphy.gif",
                "https://media.giphy.com/media/13YrHUvPzUUmkM/giphy.gif",
                "https://media.giphy.com/media/BXrwTdoho6hkQ/giphy.gif",
                "https://media1.tenor.com/images/78d3f21a608a4ff0c8a09ec12ffe763d/tenor.gif?itemid=16509980",
                "https://media.giphy.com/media/ShAchOHe38Aak/giphy.gif",
                "https://media.giphy.com/media/h2FLpIm9NBIkM/giphy.gif",
                "https://i.pinimg.com/originals/3d/4f/8a/3d4f8a927b7333a98513ea9382a1adbc.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709434451406487562/8572a1d1ebaa45fae290e6760b59caac.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709434450639061022/rlOJqHL.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709434448801824798/tumblr_inline_ph2cccm8ey1ucmb6h_540.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709434447505784883/1461001265-bd6ca0c499ca9c6065249953dfcf81c3.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709434446549352548/tumblr_n59vn0kIdh1sn5ot7o1_500.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709434442522951751/lolsda.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709434442992844960/giphy_1.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709434442388602941/giphy.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709434440480456864/Anime-hug-GIF-Image-Download-4.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709434435367338004/AlienatedUnawareArcherfish-size_restricted.gif",
                "https://static.zerochan.net/Mahou.Tsukai.no.Yome.full.2295534.gif",
                "https://static.zerochan.net/Mahou.Tsukai.no.Yome.full.2230099.gif",
                "https://static.zerochan.net/Sword.Art.Online%3A.Alicization.-War.Of.Underworld-.full.2805619.gif",
                "https://static.zerochan.net/Fruits.Basket.full.2700026.gif",
                "https://static.zerochan.net/Lady.Momona.full.2640371.gif",
                "https://tenor.com/view/coco-go-play-goku-krillin-dbz-dragon-ball-z-gif-15161811"
        };
    }

}
