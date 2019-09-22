package Commands.Interactions;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "hug",
        emoji = "\uD83D\uDC50",
        executable = false
)
public class HugCommand extends InteractionCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public HugCommand() {
        super("https://media1.tenor.com/images/72627a21fc298313f647306e6594553f/tenor.gif?itemid=9096291",
                "https://media1.tenor.com/images/6db54c4d6dad5f1f2863d878cfb2d8df/tenor.gif?itemid=7324587",
                "https://media1.tenor.com/images/45b1dd9eaace572a65a305807cfaec9f/tenor.gif?itemid=6238016",
                "https://media1.tenor.com/images/d6510db0a868cfbff697d7279aa89b61/tenor.gif?itemid=10989534",
                "https://media.giphy.com/media/qscdhWs5o3yb6/giphy.gif",
                "https://media.giphy.com/media/wnsgren9NtITS/giphy.gif",
                "https://media.giphy.com/media/HaC1WdpkL3W00/giphy.gif",
                "https://media.giphy.com/media/kvKFM3UWg2P04/giphy.gif",
                "https://media.giphy.com/media/yziFo5qYAOgY8/giphy.gif",
                "https://media.giphy.com/media/od5H3PmEG5EVq/giphy.gif",
                "https://media1.tenor.com/images/1069921ddcf38ff722125c8f65401c28/tenor.gif?itemid=11074788",
                "https://media1.tenor.com/images/e58eb2794ff1a12315665c28d5bc3f5e/tenor.gif?itemid=10195705",
                "https://media1.tenor.com/images/40aed63f5bc795ed7a980d0ad5c387f2/tenor.gif?itemid=11098589",
                "https://media1.tenor.com/images/074d69c5afcc89f3f879ca473e003af2/tenor.gif?itemid=4898650",
                "https://media1.tenor.com/images/7e30687977c5db417e8424979c0dfa99/tenor.gif?itemid=10522729",
                "https://media1.tenor.com/images/f9c540c2b5cdb52f22ed835478b0a36f/tenor.gif?itemid=10751424",
                "https://media1.tenor.com/images/b0de026a12e20137a654b5e2e65e2aed/tenor.gif?itemid=7552093",
                "https://media1.tenor.com/images/b4ba20e6cb49d8f8bae81d86e45e4dcc/tenor.gif?itemid=5634582",
                "https://media1.tenor.com/images/42922e87b3ec288b11f59ba7f3cc6393/tenor.gif?itemid=5634630",
                "https://media1.tenor.com/images/44b4b9d5e6b4d806b6bcde2fd28a75ff/tenor.gif?itemid=9383138",
                "https://media1.tenor.com/images/11889c4c994c0634cfcedc8adba9dd6c/tenor.gif?itemid=5634578",
                "https://media1.tenor.com/images/949d3eb3f689fea42258a88fa171d4fc/tenor.gif?itemid=4900166",
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
                "https://media1.tenor.com/images/81f693db5e5265c9ae21052d55ab7b3d/tenor.gif?itemid=13576354",
                "https://media1.tenor.com/images/b45619f5c109890d894eae5132dbd809/tenor.gif?itemid=14162538",
                "https://media1.tenor.com/images/c9172e2fc667d4d279e831f00533c3e6/tenor.gif?itemid=13455099",
                "https://media1.tenor.com/images/1f44c379b43bc4efb6d227a2e20b6b50/tenor.gif?itemid=13331088",
                "https://media1.tenor.com/images/1e2195e1244de5b98f50929872b4265c/tenor.gif?itemid=7614634",
                "https://media1.tenor.com/images/ce6b3a9fd4ad07f5aa84b914b6dd91d4/tenor.gif?itemid=12668669",
                "https://media1.tenor.com/images/96fc16ffae483fdce54e4f1e7fa6f649/tenor.gif?itemid=8132019",
                "https://media1.tenor.com/images/9b8e9fa95f4799f8940062c2a879a790/tenor.gif?itemid=12668679",
                "https://media1.tenor.com/images/0a3cac5657b5e9b82f64d7d4bc5d45db/tenor.gif?itemid=7898031",
                "https://media1.tenor.com/images/19cf84b7a56e9a64fe7fd5559ad287bf/tenor.gif?itemid=10243168",
                "https://media1.tenor.com/images/eccacd077d0ac499e1a5cc76c6172ad4/tenor.gif?itemid=4854611",
                "https://tenor.com/view/virtual-hug-gif-5026057",
                "https://cdn.discordapp.com/attachments/499629904380297226/582632584748466186/hug-S1qhfy2cz-1.gif"
        );
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
