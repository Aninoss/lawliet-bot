package Commands.EmotesCategory;
import CommandListeners.CommandProperties;
import Commands.EmoteAbstract;

@CommandProperties(
        trigger = "laugh",
        emoji = "\uD83D\uDE06",
        executable = true,
        aliases = {"lol", "funny"}
)
public class LaughCommand extends EmoteAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/ad4804e880c2edcecbb79217b479610a/tenor.gif?itemid=10903422",
                "https://media1.tenor.com/images/615dc6190b6438d911f366944a917ede/tenor.gif?itemid=9388677",
                "https://media1.tenor.com/images/ccf51fb7683192a9b909d7c8116cc6da/tenor.gif?itemid=11115623",
                "https://media1.tenor.com/images/2775948586d6a24811726ce4dc681d47/tenor.gif?itemid=13786657",
                "https://media1.tenor.com/images/40e753474322b8bdac0bfb29d8f6c5fe/tenor.gif?itemid=16663096",
                "https://media1.tenor.com/images/26df2182fc943676dc6cc51371efc04b/tenor.gif?itemid=8932912",
                "https://media1.tenor.com/images/fb80a2dd4fdb86c6eeee94125f23c161/tenor.gif?itemid=5060962",
                "https://media1.tenor.com/images/3be8aa0228169cf5748e21eb972ffa1d/tenor.gif?itemid=12252557",
                "https://media1.tenor.com/images/46f0022ac9c2cfdd0766f89b5b19a4f2/tenor.gif?itemid=15454829",
                "https://media1.tenor.com/images/03ef76af9dce8026e93284ff28b79411/tenor.gif?itemid=7198753",
                "https://media1.tenor.com/images/68672bb560fecf599416b0b231c3f313/tenor.gif?itemid=14274229",
                "https://media1.tenor.com/images/f7d0b534e95c24a53b9767b480e76df3/tenor.gif?itemid=11203476",
                "https://media1.tenor.com/images/60752436c762fd710643cffec01f6cbd/tenor.gif?itemid=9051310",
                "https://media1.tenor.com/images/9eca9dd2e3c19d8bdb1f27cb10ba86c6/tenor.gif?itemid=9428262",
                "https://media1.tenor.com/images/6260e2c9e5fa0178dcd612f8aaf30c57/tenor.gif?itemid=5337170",
                "https://media1.tenor.com/images/7104e82aad1e8f2d058540f9c8f08eab/tenor.gif?itemid=11576547",
                "https://media1.tenor.com/images/88be9e067283f0f9248665d6f45efbcb/tenor.gif?itemid=11858198",
                "https://media1.tenor.com/images/e2f2cbf005feb5d595884a7055cb0f6b/tenor.gif?itemid=15530797",
                "https://media1.tenor.com/images/0944ac9bc62026c81078217f68b77c19/tenor.gif?itemid=5292401",
                "https://media1.tenor.com/images/f0932ca986b587bfa0c1dc3aed530b45/tenor.gif?itemid=7601299",
                "https://media1.tenor.com/images/75977c5a1c1a26f4ba7d00a43165218e/tenor.gif?itemid=13266327",
                "https://media1.tenor.com/images/c468ca0162b2757b45a751870e753c64/tenor.gif?itemid=8453319",
                "https://media1.tenor.com/images/dbe3f98915c2d1928135c55598b4b6db/tenor.gif?itemid=5268103",
                "https://media1.tenor.com/images/c7f5be01b26a4af8db38b2d27c27264e/tenor.gif?itemid=16268185",
                "https://media1.tenor.com/images/b3e0ecd965e324aa328a0137c38a44f1/tenor.gif?itemid=5566554",
                "https://media1.tenor.com/images/faebec11a5be81a95f69d7b38f4b8171/tenor.gif?itemid=14132796",
                "https://media1.tenor.com/images/6c2243fcf5eec62d6c43e5078c30b1ca/tenor.gif?itemid=10120660",
                "https://media1.tenor.com/images/dc587792a39149095b65d419d0ba2f14/tenor.gif?itemid=10351493",
                "https://media1.tenor.com/images/7f2ff46ef02b847848d13dca529a26a7/tenor.gif?itemid=15338276",
                "https://media1.tenor.com/images/d47b270c91ee8d97b8499c9c5a864e38/tenor.gif?itemid=14064857",
                "https://media1.tenor.com/images/7ec99eb7a57d021d7e8758d1af465930/tenor.gif?itemid=13300641",
                "https://media1.tenor.com/images/e62fc9360b184a7dd8a0e786d5d594fc/tenor.gif?itemid=12381387",
                "https://media1.tenor.com/images/36e3ec5f704b0c9182ee3a6c687b702b/tenor.gif?itemid=16600431",
                "https://media1.tenor.com/images/6ee35e7ad417b6fe567064dddcab4d1d/tenor.gif?itemid=14705797",
                "https://media1.tenor.com/images/d330b334bee3ae9cf88727fb60a0dc6e/tenor.gif?itemid=15915892",
                "https://media1.tenor.com/images/e07a36575478d8dbac2a752c3caebbe4/tenor.gif?itemid=12050954",
                "https://media1.tenor.com/images/d02af95a77338cfa77a9a5c3305b3295/tenor.gif?itemid=15917449",
                "https://cdn.discordapp.com/attachments/499629904380297226/709435369267134594/TQkHERF.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709435375373779075/Sks8eqV.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709440301793542154/laugh.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709442099988398140/ImaginativeTepidDormouse-size_restricted.gif",
                "http://mrwgifs.com/wp-content/uploads/2014/10/Vegeta-Laughing-On-Earth-After-The-Battle-With-Frieza-On-Dragon-Ball-Z.gif",
                "https://tenor.com/view/dragon-ball-super-vegetablue-dbz-vegeta-dbs-gif-12462077",
                "https://thumbs.gfycat.com/KeyUnsightlyEwe-size_restricted.gif"
        };
    }

}
