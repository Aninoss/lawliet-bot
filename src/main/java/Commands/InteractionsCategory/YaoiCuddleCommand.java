package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;

import Commands.InteractionAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "yaoicuddle",
        emoji = "\uD83D\uDC68\uD83D\uDC50\uD83D\uDC68",
        executable = false,
        aliases = {"yaoisnuggle"}
)
public class YaoiCuddleCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/a8cbc11ee331c62aaf03420d99696da0/tenor.gif?itemid=9556155",
                "https://media1.tenor.com/images/5dbb6d29ac9f63d7815a95997ecbae56/tenor.gif?itemid=13356108",
                "https://media1.tenor.com/images/2ef3e594cc380567b53ae2f670c54ef9/tenor.gif?itemid=7287693",
                "https://media1.tenor.com/images/d55fdb878455106bb8d75ac9c4531f6c/tenor.gif?itemid=10416703",
                "https://media1.tenor.com/images/58cd629d688d826cf3fb39e949637169/tenor.gif?itemid=7250489",
                "https://media1.tenor.com/images/38f02dffb8ebd86327da22a3d912f3c7/tenor.gif?itemid=5982174",
                "https://media1.tenor.com/images/4525f85b1e81a8f0e7bafea1295b152d/tenor.gif?itemid=7281060",
                "https://media1.tenor.com/images/4710c7b3463f914432ff43aad0e48bf5/tenor.gif?itemid=9124287",
                "https://media1.tenor.com/images/682569ce53d4aeae22a649fba091eb5d/tenor.gif?itemid=7791282",
                "https://media1.tenor.com/images/8cf8eafae079be517f61d7f65e3c813f/tenor.gif?itemid=9554711",
                "https://media1.tenor.com/images/778d789b9c6e8e624c8650f1b988204f/tenor.gif?itemid=14839882",
                "https://media1.tenor.com/images/8eaecab4e8e5e2b2de7f8ee294079175/tenor.gif?itemid=12669021"
        };
    }

}
