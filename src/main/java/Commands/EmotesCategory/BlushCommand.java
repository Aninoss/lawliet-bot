package Commands.EmotesCategory;
import CommandListeners.CommandProperties;
import Commands.EmoteAbstract;

@CommandProperties(
        trigger = "blush",
        emoji = "\uD83D\uDE0A",
        executable = true
)
public class BlushCommand extends EmoteAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/639036f3c2f138e87a679f7e121482bf/tenor.gif?itemid=15338269",
                "https://media1.tenor.com/images/4f270d2727e514056ae63f155ba0cef2/tenor.gif?itemid=13045709",
                "https://media1.tenor.com/images/274fc34d3add3ce4cbb5716cb4f94f4f/tenor.gif?itemid=5841198",
                "https://media1.tenor.com/images/84307582253a96e4552d20e3ecef3a33/tenor.gif?itemid=5531498",
                "https://media1.tenor.com/images/ea8977dd6dc918c2d0fc253d714105f4/tenor.gif?itemid=10750489",
                "https://media1.tenor.com/images/e0843ec7653352f5317ca4f279aa1f79/tenor.gif?itemid=15796764",
                "https://media1.tenor.com/images/b00fe041997afa8fff0734a1fb8dd2a4/tenor.gif?itemid=13768377",
                "https://media1.tenor.com/images/981ee5030a18a779e899b2c307e65f7a/tenor.gif?itemid=13159552",
                "https://media1.tenor.com/images/ea8977dd6dc918c2d0fc253d714105f4/tenor.gif?itemid=10750489",
                "https://media1.tenor.com/images/ad0fbaa8dd27994a8fbab2ab86122c7f/tenor.gif?itemid=13658383",
                "https://media1.tenor.com/images/8f76f034ccc458bd09675c0380f59cb7/tenor.gif?itemid=5634589",
                "https://media1.tenor.com/images/29ab83ef501b53273cdb9489819225ff/tenor.gif?itemid=5522297",
                "https://media1.tenor.com/images/09d75740089598b54342df3641dbbffc/tenor.gif?itemid=5615361",
                "https://media1.tenor.com/images/71015cf10d2bc6ddc6c2dd0d7b294277/tenor.gif?itemid=9096269",
                "https://media1.tenor.com/images/c0a9bb579552da7fbf71d105b9676f3d/tenor.gif?itemid=12550758",
                "https://media1.tenor.com/images/9eba52d0506b552b7ef6a1981c0cfcff/tenor.gif?itemid=8680309",
                "https://media1.tenor.com/images/95d627e71466ebfb2a168a041c96f122/tenor.gif?itemid=13720542",
                "https://media1.tenor.com/images/f62cae32b30d364bf0a8a1e7432c2ddf/tenor.gif?itemid=10198325",
                "https://media1.tenor.com/images/85be0c08818f1faa7cffbbec9cf7c02e/tenor.gif?itemid=4957566",
                "https://media1.tenor.com/images/a7e87466022015e036c06c3927c251f9/tenor.gif?itemid=8971744",
                "https://media1.tenor.com/images/9af8d8afab3b509a97f2440562845682/tenor.gif?itemid=13978385",
                "https://media1.tenor.com/images/7ae3f6f1c48b01549b855cb0f6b1c4d7/tenor.gif?itemid=5658972",
                "https://media1.tenor.com/images/b4ebe6c9c4786dd32b51dd346135b625/tenor.gif?itemid=5881549",
                "https://media1.tenor.com/images/ac2f1f727d4d96a6a7c4fb5ae5a41cf0/tenor.gif?itemid=12297830",
                "https://media1.tenor.com/images/5ea40ca0d6544dbf9c0074542810e149/tenor.gif?itemid=14841901",
                "https://media1.tenor.com/images/0c36e794bfdfcaf837fb114d46e5c8fc/tenor.gif?itemid=7275601",
                "https://media1.tenor.com/images/d9b08d9984e694111ba7107c198f85b7/tenor.gif?itemid=5634600",
                "https://media1.tenor.com/images/5838f2442696636b1ee57dc4063e697b/tenor.gif?itemid=8861474",
                "https://media1.tenor.com/images/5eea16dacd36b7080e83bd14d8ecac81/tenor.gif?itemid=13931357",
                "https://media1.tenor.com/images/c406e252b5818bf6f858d031342347f8/tenor.gif?itemid=9766379",
                "https://media1.tenor.com/images/7556c6d333bd05b8585dfb2f1cb7abbf/tenor.gif?itemid=8680312",
                "https://media1.tenor.com/images/e0dddee577a142b91941d6642f98321c/tenor.gif?itemid=5001273",
                "https://media1.tenor.com/images/82a2dbffedfa319a54bca726313fc8e0/tenor.gif?itemid=12252522",
                "https://media1.tenor.com/images/cc187b06f246e71b07613e3957d87e00/tenor.gif?itemid=5102126",
                "https://media1.tenor.com/images/dd96da2dd884e3d3c684633914d99a14/tenor.gif?itemid=5674419",
                "https://media1.tenor.com/images/cf3b6541b4c6d36e41aa3ae8427ea3bf/tenor.gif?itemid=7866278",
                "https://media1.tenor.com/images/26c547df1049d14aacd5f581c6f46896/tenor.gif?itemid=13937658",
                "https://media1.tenor.com/images/cbfd2a06c6d350e19a0c173dec8dccde/tenor.gif?itemid=15727535",
                "https://media1.tenor.com/images/fbcbdbff72bde829a29347bf162e870c/tenor.gif?itemid=3478341",
                "https://media1.tenor.com/images/b0c9cdfd4d568bbec2707ac6f8ec8cdf/tenor.gif?itemid=12912497",
                "https://media1.tenor.com/images/6a76217dec160b439b7ce6ae5d379a85/tenor.gif?itemid=5934172",
                "https://media1.tenor.com/images/7fddbaa08668ce16a00e5a81d09610d8/tenor.gif?itemid=11034207",
                "https://media1.tenor.com/images/f4a5b1f67f07256ee5334ae3f8a79132/tenor.gif?itemid=13576400",
                "https://media1.tenor.com/images/eb0d2648508a0f1ec66a305316944bee/tenor.gif?itemid=13045710",
                "https://media1.tenor.com/images/0755c3fa29e61807a74e5f875827a6e5/tenor.gif?itemid=11034209"
        };
    }

}
