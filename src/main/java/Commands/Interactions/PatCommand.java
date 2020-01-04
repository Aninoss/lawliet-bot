package Commands.Interactions;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "pat",
        emoji = "\uD83E\uDD1A",
        executable = false,
        aliases = {"praise"}
)
public class PatCommand extends InteractionCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public PatCommand() {
        super("https://media1.tenor.com/images/c0bcaeaa785a6bdf1fae82ecac65d0cc/tenor.gif?itemid=7453915",
                "https://media1.tenor.com/images/116fe7ede5b7976920fac3bf8067d42b/tenor.gif?itemid=9200932",
                "https://media1.tenor.com/images/da8f0e8dd1a7f7db5298bda9cc648a9a/tenor.gif?itemid=12018819",
                "https://media1.tenor.com/images/183ff4514cbe90609e3f286adaa3d0b4/tenor.gif?itemid=5518321",
                "https://media1.tenor.com/images/f5176d4c5cbb776e85af5dcc5eea59be/tenor.gif?itemid=5081286",
                "https://media1.tenor.com/images/153e9bdd80008e8c0f94110450fcbf98/tenor.gif?itemid=10534102",
                "https://media1.tenor.com/images/28f4f29de42f03f66fb17c5621e7bedf/tenor.gif?itemid=8659513",
                "https://media1.tenor.com/images/291ea37382e1d6cd33349c50a398b6b9/tenor.gif?itemid=10204936",
                "https://media1.tenor.com/images/71e74263a48a6e9a2c53f3bc1439c3ac/tenor.gif?itemid=12434286",
                "https://media1.tenor.com/images/c0c1c5d15f8ad65a9f0aaf6c91a3811e/tenor.gif?itemid=13410974",
                "https://media1.tenor.com/images/bf646b7164b76efe82502993ee530c78/tenor.gif?itemid=7394758",
                "https://media1.tenor.com/images/857aef7553857b812808a355f31bbd1f/tenor.gif?itemid=13576017",
                "https://media1.tenor.com/images/9bf3e710f33cae1eed1962e7520f9cf3/tenor.gif?itemid=13236885",
                "https://media1.tenor.com/images/005e8df693c0f59e442b0bf95c22d0f5/tenor.gif?itemid=10947495",
                "https://media1.tenor.com/images/54722063c802bac30d928db3bf3cc3b4/tenor.gif?itemid=8841561",
                "https://media1.tenor.com/images/a9d0fd4bb985bb38a82a9af843fa2480/tenor.gif?itemid=6211183",
                "https://media1.tenor.com/images/daa885ec8a9cfa4107eb966df05ba260/tenor.gif?itemid=13792462",
                "https://media1.tenor.com/images/ae7a21f434cee8762d127b8aed889296/tenor.gif?itemid=9058328",
                "https://media1.tenor.com/images/70960e87fb9454df6a1d15c96c9ad955/tenor.gif?itemid=10092582",
                "https://media1.tenor.com/images/64d45ee51ea8d55760c81a93353ffdb3/tenor.gif?itemid=11179299",
                "https://media1.tenor.com/images/7e89ab34fd2c59698479a740f056e325/tenor.gif?itemid=11993003",
                "https://media1.tenor.com/images/13f385a3442ac5b513a0fa8e8d805567/tenor.gif?itemid=13857199",
                "https://media1.tenor.com/images/098a45951c569edc25ea744135f97ccf/tenor.gif?itemid=10895868",
                "https://media1.tenor.com/images/5466adf348239fba04c838639525c28a/tenor.gif?itemid=13284057",
                "https://media1.tenor.com/images/68d981347bf6ee8c7d6b78f8a7fe3ccb/tenor.gif?itemid=5155410",
                "https://media1.tenor.com/images/dadf2f2aa8efc7b0856b6af26e72a8cb/tenor.gif?itemid=6005361",
                "https://media1.tenor.com/images/d9e575861bb2f6389cec93da6cbdfa1f/tenor.gif?itemid=9720876",
                "https://media1.tenor.com/images/398c9c832335a13be124914c23e88fdf/tenor.gif?itemid=9939761",
                "https://tenor.com/view/kitten-kittens-cute-pet-give-me-attention-gif-12816949"
        );
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
