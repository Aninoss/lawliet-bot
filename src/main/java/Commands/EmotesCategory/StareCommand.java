package Commands.EmotesCategory;
import CommandListeners.CommandProperties;
import Commands.EmoteAbstract;

@CommandProperties(
        trigger = "stare",
        emoji = "\uD83D\uDC40",
        executable = true,
        aliases = {"see"}
)
public class StareCommand extends EmoteAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/48d3b956a09c51b4ec3018d39412d38a/tenor.gif?itemid=12390475",
                "https://media1.tenor.com/images/ef41ebe133bcf3d9dc35f260b1f40506/tenor.gif?itemid=12003886",
                "https://media1.tenor.com/images/e3ed0701f3d0e6a794afd2c035461876/tenor.gif?itemid=9902957",
                "https://media1.tenor.com/images/58adf5859001fe3f1586e87b5d86cebd/tenor.gif?itemid=7025500",
                "https://media1.tenor.com/images/a25e701fd5140252c86140ecbee57cfb/tenor.gif?itemid=14043188",
                "https://media1.tenor.com/images/80bf471a41460d13225e7192f2548a76/tenor.gif?itemid=15802926",
                "https://media1.tenor.com/images/6a805dfc86d63fc48677398f3e980ac5/tenor.gif?itemid=4604175",
                "https://media1.tenor.com/images/7c2db155a54db23b29d4856e0bcd86d3/tenor.gif?itemid=14878621",
                "https://media1.tenor.com/images/0c677087fd486e25bfecabfc51e6a418/tenor.gif?itemid=9721775",
                "https://media1.tenor.com/images/ad4684854b2b82d065aa5844033a79d1/tenor.gif?itemid=12003923",
                "https://media1.tenor.com/images/6db16173c29293e2c0f63db13601a85d/tenor.gif?itemid=15313333",
                "https://media1.tenor.com/images/7665926b8492d1cae9a3709f5f4d8501/tenor.gif?itemid=5934174",
                "https://media1.tenor.com/images/7e3f3bd07f6e642c2e14ea3b4808539f/tenor.gif?itemid=4748113",
                "https://media1.tenor.com/images/a8452b5d62312574b27617d3646b3372/tenor.gif?itemid=15516776",
                "https://media1.tenor.com/images/ecdaa49c440c69f275a9db751ec3f514/tenor.gif?itemid=5246753",
                "https://media1.tenor.com/images/5860413f24a162df8ff2a1e364582fe1/tenor.gif?itemid=14342172",
                "https://media1.tenor.com/images/7c6fdbb98e0dcf565431f87ed7014994/tenor.gif?itemid=5634640",
                "https://media1.tenor.com/images/4908729f7fb720d3b1e3655348cb3345/tenor.gif?itemid=15060978",
                "https://media1.tenor.com/images/ab357ce2a3d5283142d6a6add1b2d4a0/tenor.gif?itemid=12811249",
                "https://media1.tenor.com/images/f1c44287b3b59b4c1bceacf5f42627f1/tenor.gif?itemid=12373653",
                "https://media1.tenor.com/images/bc60d3e7cda9fd168a96f575843a8b02/tenor.gif?itemid=14319673",
                "https://media1.tenor.com/images/afd4282d996325f5da7be3c2c963df41/tenor.gif?itemid=4686978",
                "https://media1.tenor.com/images/a7bc98cd8bdb7cdcf4ef10efc8ba6d85/tenor.gif?itemid=15031115",
                "https://media1.tenor.com/images/88cfd6397444e29f0053d37217184090/tenor.gif?itemid=12740566",
                "https://media1.tenor.com/images/54149a533b933e23923059fbb9ead5a7/tenor.gif?itemid=12950085",
                "https://media1.tenor.com/images/98e1dde6d86d67c1404b05e59db3d714/tenor.gif?itemid=9588641",
                "https://media1.tenor.com/images/0c4661335710fe2a9b94fb6b8f61f2a6/tenor.gif?itemid=5758538",
                "https://media1.tenor.com/images/4fb5bba925afda35aa16c5d838694165/tenor.gif?itemid=14172299",
                "https://media1.tenor.com/images/010782907b4ff7f04098a8f47a7dd0e8/tenor.gif?itemid=11701069"
        };
    }

}
