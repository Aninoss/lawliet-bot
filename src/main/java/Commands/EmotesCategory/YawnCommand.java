package Commands.EmotesCategory;
import CommandListeners.CommandProperties;
import Commands.EmoteAbstract;

@CommandProperties(
        trigger = "yawn",
        emoji = "\uD83E\uDD71",
        executable = true,
        aliases = {"wakeup", "tired", "sleepy"}
)
public class YawnCommand extends EmoteAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/90eb96a0257938f9a8d42846e6226571/tenor.gif?itemid=8472935",
                "https://media1.tenor.com/images/d7176899990b3be56b89cbd76d4f6b49/tenor.gif?itemid=16309858",
                "https://media1.tenor.com/images/9cef52ce27ab97e0fa9cfac1cdc1007f/tenor.gif?itemid=9525859",
                "https://media1.tenor.com/images/6230c6a994f25608ebc28e769ed95c58/tenor.gif?itemid=12003902",
                "https://media1.tenor.com/images/0806130ea6389b76a397655d30833777/tenor.gif?itemid=16378676",
                "https://media1.tenor.com/images/8b2e4e737362a02f1d3f28d3e47a0eb7/tenor.gif?itemid=8642369",
                "https://media1.tenor.com/images/0981aae731d5bd80bdcb40b7982e391e/tenor.gif?itemid=5604306",
                "https://media1.tenor.com/images/bf6b91c51ce7901ab308631bf9487687/tenor.gif?itemid=15157910",
                "https://media1.tenor.com/images/259a11c7639b5fb5e77886c8f91c9137/tenor.gif?itemid=14818724",
                "https://media1.tenor.com/images/855cee1f678ef2c95e0cad492881d7f1/tenor.gif?itemid=5706339",
                "https://media1.tenor.com/images/5ed94bb8281393167afe93dcd60086eb/tenor.gif?itemid=16898926",
                "https://media1.tenor.com/images/1df48a2d012ca195de0db0e0c2568d0b/tenor.gif?itemid=16339410",
                "https://media1.tenor.com/images/35b7e7989f0bb9ec8cdb25ee2b06d973/tenor.gif?itemid=11115658",
                "https://media1.tenor.com/images/84d907b9ce470d7237d8222deb8ef209/tenor.gif?itemid=5418281",
                "https://media1.tenor.com/images/f2b8241683836fe5a66c09f64abb2a21/tenor.gif?itemid=16458632",
                "https://media1.tenor.com/images/bc8bf0e8cd0588e570b1093619f35274/tenor.gif?itemid=7643676",
                "https://media1.tenor.com/images/46cff5a47ebcb25816ea41bb0b6f497f/tenor.gif?itemid=13451600",
                "https://media1.tenor.com/images/6af2530d5876974dd90c504c912fad8f/tenor.gif?itemid=12513791",
                "https://media1.tenor.com/images/756e5ee1b24dfb7e0c4976974af175f8/tenor.gif?itemid=17034115",
                "https://media1.tenor.com/images/9e931007781592f3041f830c390338aa/tenor.gif?itemid=14356648",
                "https://media1.tenor.com/images/2927a0bdbb036afd3e6cfed4e776f834/tenor.gif?itemid=14088490",
                "https://media1.tenor.com/images/8d87fab5f46c83fe3aa02878cc1aa7c3/tenor.gif?itemid=16507581",
                "https://media1.tenor.com/images/a72c94f07c99762064c45ff74d805286/tenor.gif?itemid=14350734"
        };
    }

}
