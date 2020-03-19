package Commands.EmotesCategory;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import Commands.EmoteAbstract;

@CommandProperties(
    trigger = "yes",
    emoji = "\uD83D\uDC4D",
    executable = true
)
public class YesCommand extends EmoteAbstract implements onRecievedListener {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/ecad438e52beccce29765710049c375a/tenor.gif?itemid=4505671",
                "https://media1.tenor.com/images/a3d3d51c8ca4598f2fb39da51ec9584f/tenor.gif?itemid=4361784",
                "https://media1.tenor.com/images/96d6ca2e172c28b10edccdb38bdacaa5/tenor.gif?itemid=5881788",
                "https://media1.tenor.com/images/217f30b2f7d415fe4a011b5b5b952e9f/tenor.gif?itemid=12763295",
                "https://media1.tenor.com/images/9473f9895e85141ffb82edb040a5547a/tenor.gif?itemid=5298950",
                "https://media1.tenor.com/images/5e2fabce26e30ed9d379d430bf0e716e/tenor.gif?itemid=8822335",
                "https://media1.tenor.com/images/86f6b5ac2d58b9956435f5aeb1e2ac5b/tenor.gif?itemid=10853262",
                "https://media1.tenor.com/images/13d12906ab6c24e688a1144f85199e98/tenor.gif?itemid=10627589",
                "https://media1.tenor.com/images/0a3589d141f2a18f78c62db3dc950112/tenor.gif?itemid=10780533",
                "https://media1.tenor.com/images/6ff2c294909c6b9b981a9011fc8435e8/tenor.gif?itemid=7564927",
                "https://media1.tenor.com/images/96378f911d4057b56a947db06b7eec2d/tenor.gif?itemid=9685848",
                "https://media1.tenor.com/images/0b673f57807af5ac91e68091ed1549ab/tenor.gif?itemid=10883460",
                "https://media1.tenor.com/images/692274404c366b49bcb0c9636f7601d1/tenor.gif?itemid=7467617",
                "https://media1.tenor.com/images/dd070dad2d442c01170258f2e86e7453/tenor.gif?itemid=10616938",
                "https://media1.tenor.com/images/f51caa47e76864709e597242f8ecfddd/tenor.gif?itemid=14074198",
                "https://media1.tenor.com/images/46f92f5d96b952af2f7fd8dfa98e94a4/tenor.gif?itemid=6181819",
                "https://media1.tenor.com/images/e0f3c081f41859553539e401046f3626/tenor.gif?itemid=4493276",
                "https://media1.tenor.com/images/f6831eeb7c2ba1984b3e5a41b047f737/tenor.gif?itemid=13451534",
                "https://media1.tenor.com/images/081ee762032da039e6c74984f55bdb36/tenor.gif?itemid=13649649"
        };
    }

}
