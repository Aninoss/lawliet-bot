package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;

import Commands.InteractionAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "slap",
        emoji = "\uD83D\uDC4F",
        executable = false
)
public class SlapCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/35c1ecae2168c49be997871adc2a5d75/tenor.gif?itemid=3412059",
                "https://media1.tenor.com/images/0860d681fbe7ad04a2f39735ab939176/tenor.gif?itemid=13642334",
                "https://media1.tenor.com/images/fd14f63a93796ed26bd385c015df57b8/tenor.gif?itemid=4665506",
                "https://media1.tenor.com/images/153b2f1bfd3c595c920ce60f1553c5f7/tenor.gif?itemid=10936993",
                "https://media1.tenor.com/images/4a6b15b8d111255c77da57c735c79b44/tenor.gif?itemid=10937039",
                "https://media1.tenor.com/images/d14969a21a96ec46f61770c50fccf24f/tenor.gif?itemid=5509136",
                "https://media1.tenor.com/images/1cf84bf514d2abd2810588caf7d9fd08/tenor.gif?itemid=7679403",
                "https://media1.tenor.com/images/4fa82be21ffd18c99a9708ba209d56ad/tenor.gif?itemid=5318916",
                "https://media1.tenor.com/images/477821d58203a6786abea01d8cf1030e/tenor.gif?itemid=7958720",
                "https://media1.tenor.com/images/f9f121a46229ea904209a07cae362b3e/tenor.gif?itemid=7859254",
                "https://media1.tenor.com/images/7437caf9fb0bea289a5bb163b90163c7/tenor.gif?itemid=13595529",
                "https://media1.tenor.com/images/6885c7676d8645bf2891138564159713/tenor.gif?itemid=4436362",
                "https://media1.tenor.com/images/c159cd1d7e7424cf9fd6fbdb09919146/tenor.gif?itemid=14179570",
                "https://media1.tenor.com/images/b221fb3f50f0e15b3ace6a2b87ad0ffa/tenor.gif?itemid=8576304",
                "https://media1.tenor.com/images/741cad565c0e08a5e03b33e52a500b9a/tenor.gif?itemid=5417104",
                "https://media1.tenor.com/images/b6d8a83eb652a30b95e87cf96a21e007/tenor.gif?itemid=10426943",
                "https://media1.tenor.com/images/fb17a25b86d80e55ceb5153f08e79385/tenor.gif?itemid=7919028",
                "https://media1.tenor.com/images/5e5f33fd48aaaa0a116df3bd8ebb7a53/tenor.gif?itemid=12858541",
                "https://media1.tenor.com/images/6f5c1f380b4cb313f412f57f4508c7e9/tenor.gif?itemid=12409765",
                "https://media1.tenor.com/images/358986720d4b533a49bdb67cbc4fe3e5/tenor.gif?itemid=14179582",
                "https://media1.tenor.com/images/9d907ed56fa1c8c011791e494b1d6ce0/tenor.gif?itemid=14179587",
                "https://media1.tenor.com/images/6a2cf4394afc9f60aa302be43a566dd6/tenor.gif?itemid=12342551",
                "https://media1.tenor.com/images/c1246556aa5726ad6c0ee50f2c3998ce/tenor.gif?itemid=7864657",
                "https://media1.tenor.com/images/4a5025fef68e651ba91e86bd09bdd911/tenor.gif?itemid=13652760"
        };
    }

}
