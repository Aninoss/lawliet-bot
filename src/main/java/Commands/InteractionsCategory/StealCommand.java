package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;

import Commands.InteractionAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "steal",
        emoji = "❔",
        executable = false
)
public class StealCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/f2be72f3aa513baa5782aa768f27c860/tenor.gif?itemid=10194720",
                "https://media1.tenor.com/images/e80defe5972b050095d4b592128ad282/tenor.gif?itemid=9705636",
                "https://media1.tenor.com/images/bef3dcc27e3cce3fec27ec15d91d2ba7/tenor.gif?itemid=13888906",
                "https://media1.tenor.com/images/e85c83d4b84fbf60f76164a692b16210/tenor.gif?itemid=11273898",
                "https://media1.tenor.com/images/d367e30bef5386927ab0ef566197cbe1/tenor.gif?itemid=4885181",
                "https://media1.tenor.com/images/618714d7365ac78a9206c6429a6db8ab/tenor.gif?itemid=13894357",
                "https://media1.tenor.com/images/c4c413f322b3adfd7efd4fb92ea11e9e/tenor.gif?itemid=11744811",
                "https://media1.tenor.com/images/b0b6dcbacc72a89a6d4077a8bf1df7b7/tenor.gif?itemid=12390213",
                "https://media1.tenor.com/images/6fcaee6c288d418f4c15b99406240860/tenor.gif?itemid=5666314"
        };
    }

}
