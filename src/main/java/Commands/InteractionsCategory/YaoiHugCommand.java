package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import Commands.InteractionAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "yaoihug",
        emoji = "\uD83D\uDC68\uD83D\uDC50\uD83D\uDC68",
        executable = false
)
public class YaoiHugCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media.giphy.com/media/yziFo5qYAOgY8/giphy.gif",
                "https://media1.tenor.com/images/949d3eb3f689fea42258a88fa171d4fc/tenor.gif?itemid=4900166",
                "https://media1.tenor.com/images/81f693db5e5265c9ae21052d55ab7b3d/tenor.gif?itemid=13576354",
                "https://media1.tenor.com/images/b45619f5c109890d894eae5132dbd809/tenor.gif?itemid=14162538",
                "https://media1.tenor.com/images/1f44c379b43bc4efb6d227a2e20b6b50/tenor.gif?itemid=13331088",
                "https://media1.tenor.com/images/1e2195e1244de5b98f50929872b4265c/tenor.gif?itemid=7614634",
                "https://media1.tenor.com/images/ce6b3a9fd4ad07f5aa84b914b6dd91d4/tenor.gif?itemid=12668669",
                "https://media1.tenor.com/images/96fc16ffae483fdce54e4f1e7fa6f649/tenor.gif?itemid=8132019",
                "https://media1.tenor.com/images/9b8e9fa95f4799f8940062c2a879a790/tenor.gif?itemid=12668679",
                "https://media1.tenor.com/images/0a3cac5657b5e9b82f64d7d4bc5d45db/tenor.gif?itemid=7898031",
                "https://media1.tenor.com/images/19cf84b7a56e9a64fe7fd5559ad287bf/tenor.gif?itemid=10243168",
                "https://media1.tenor.com/images/eccacd077d0ac499e1a5cc76c6172ad4/tenor.gif?itemid=4854611"
        };
    }

}
