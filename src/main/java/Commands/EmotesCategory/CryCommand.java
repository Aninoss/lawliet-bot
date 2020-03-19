package Commands.EmotesCategory;
import CommandListeners.CommandProperties;
import Commands.EmoteAbstract;

@CommandProperties(
        trigger = "cry",
        emoji = "\uD83D\uDE2D",
        executable = true,
        aliases = {"sad"}
)
public class CryCommand extends EmoteAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/ce52606293142a2bd11cda1d3f0dc12c/tenor.gif?itemid=5184314",
                "https://media1.tenor.com/images/4b5e9867209d7b1712607958e01a80f1/tenor.gif?itemid=5298257",
                "https://media1.tenor.com/images/de730b51400ed4dfb66d04141ea79a2d/tenor.gif?itemid=7353410",
                "https://media1.tenor.com/images/4f22255d60f3f19edf9296992b4e3483/tenor.gif?itemid=4772697",
                "https://media1.tenor.com/images/2fb2965acbf3ed573e8b63080b947fe5/tenor.gif?itemid=5091716",
                "https://media1.tenor.com/images/b88fa314f0f172832a5f41fce111f359/tenor.gif?itemid=13356071",
                "https://media1.tenor.com/images/09b085a6b0b33a9a9c8529a3d2ee1914/tenor.gif?itemid=5648908",
                "https://media1.tenor.com/images/e69ebde3631408c200777ebe10f84367/tenor.gif?itemid=5081296",
                "https://media1.tenor.com/images/213ec50caaf02d27d358363016204d1d/tenor.gif?itemid=4553386",
                "https://media1.tenor.com/images/26e7564bfb4408f9f7ff9518d4f87308/tenor.gif?itemid=8199739",
                "https://media1.tenor.com/images/49e4248f18b359dd46f7b60b01d1a4a0/tenor.gif?itemid=5652241",
                "https://media1.tenor.com/images/051cf0932320cdfbb4be560cf8f3eae7/tenor.gif?itemid=9772379",
                "https://media1.tenor.com/images/8f6da405119d24f7f86ff036d02c2fd4/tenor.gif?itemid=5378935",
                "https://media1.tenor.com/images/7443eb36be27659fc4d3effbaa766db5/tenor.gif?itemid=11358249",
                "https://media1.tenor.com/images/031c7c348d3b86296976e2407723d4a8/tenor.gif?itemid=5014031",
                "https://media1.tenor.com/images/f5ec64b40d2adf7deb84e3c0e192ff32/tenor.gif?itemid=6194053",
                "https://media1.tenor.com/images/d5668af606ca4d0332a6507418cabbce/tenor.gif?itemid=4952249",
                "https://media1.tenor.com/images/75edc9882e5175f86c2af777ffbb14a6/tenor.gif?itemid=5755232",
                "https://media1.tenor.com/images/c09cfb56ca4311502f8713712f6a96d1/tenor.gif?itemid=9052471",
                "https://media1.tenor.com/images/f4efbb0911cb0d6d3e8b1d1d4bdb83e1/tenor.gif?itemid=8934978",
                "https://media1.tenor.com/images/4bb996f5c99d48faf8590d8c66396065/tenor.gif?itemid=7552065",
                "https://media1.tenor.com/images/67df1dca3260e0032f40048759a967a5/tenor.gif?itemid=5415917",
                "https://media1.tenor.com/images/b0f4b5f158e8a964adbabd048fb9e556/tenor.gif?itemid=13949015",
                "https://media1.tenor.com/images/7618310e4332bd3303acb414348e475c/tenor.gif?itemid=5755226",
                "https://media1.tenor.com/images/a53f4017a15753ff10e42770e89ce1d0/tenor.gif?itemid=4555995",
                "https://media1.tenor.com/images/6502ef6a9c10a1913e880b285aabcb04/tenor.gif?itemid=5619724",
                "https://media1.tenor.com/images/180ece0e4a1656131513bcc60afeec81/tenor.gif?itemid=5081292",
                "https://media1.tenor.com/images/dac529ebc72771b9d40373f0c4e10eff/tenor.gif?itemid=3532071",
                "https://media1.tenor.com/images/98466bf4ae57b70548f19863ca7ea2b4/tenor.gif?itemid=14682297",
                "https://media1.tenor.com/images/3caea37ad3d608fc57231050f1d52a4c/tenor.gif?itemid=5336156",
                "https://media1.tenor.com/images/12db9a45e63977bdc9b82f04c57549ca/tenor.gif?itemid=5157556",
                "https://media1.tenor.com/images/ea48e3187d17eb474b563d64e1e4a97f/tenor.gif?itemid=11422018",
                "https://media1.tenor.com/images/68ad8d043be200ca3c3dd5e6c98b547b/tenor.gif?itemid=13765432",
                "https://media1.tenor.com/images/98570a0137beae4f8a216d5cda1d6eee/tenor.gif?itemid=13925433",
                "https://media1.tenor.com/images/8cd7401eb525f50779f271a9eb29719c/tenor.gif?itemid=9916411",
                "https://media1.tenor.com/images/850aa061274be29c44ede086a52b341e/tenor.gif?itemid=10349221",
                "https://media1.tenor.com/images/38dab3a80f57de74359633fd313342e8/tenor.gif?itemid=4854830",
                "https://media1.tenor.com/images/ecf674c5e0ed2fdf0260ade4fad2146f/tenor.gif?itemid=5580602",
                "https://media1.tenor.com/images/e59bd255f933ab786de2de0eb9b49cb9/tenor.gif?itemid=5012100",
                "https://media1.tenor.com/images/0fa6df4ee525e5ee0acedd307d34c334/tenor.gif?itemid=5895906",
                "https://media1.tenor.com/images/9663e89dcde6a147536aa6ab0bd59083/tenor.gif?itemid=5001399",
                "https://media1.tenor.com/images/04b0feb0e2e6861d5e57c1cb2cdb4dd9/tenor.gif?itemid=10810456",
                "https://media1.tenor.com/images/9d216f884c5c44e8e3c4ddb7227caf1b/tenor.gif?itemid=11384663"
        };
    }

}
