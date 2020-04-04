package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;

import Commands.InteractionAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "yaoifuck",
        emoji = "\uD83D\uDC68\uD83D\uDECF\uD83D\uDC68️",
        executable = false,
        nsfw = true
)
public class YaoiFuckCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"http://pa1.narvii.com/6296/ec86ad235dfee26a84771c49ad8531887dd5cd71_00.gif",
                "http://pa1.narvii.com/6389/5a5784c7b51571e8c736ed417363c7b42884ad13_00.gif",
                "https://i.pinimg.com/originals/22/58/5d/22585d5a1c9a2d9f094ac9a52471e0aa.gif",
                "https://img2.gelbooru.com//images/9c/f3/9cf335a510bd78b60eee9ddf588007be.gif",
                "https://img2.gelbooru.com//images/ba/6d/ba6d2292efa2a67375b0e8c2831fa744.gif",
                "https://img2.gelbooru.com//images/1c/e4/1ce4612c1db5f13201efa48121af0025.gif",
                "https://img2.gelbooru.com//images/29/d9/29d9b2920b138990827f93a187f7a685.gif",
                "https://img2.gelbooru.com//images/13/4e/134ead446ab66fb76892893a02c2fdd4.gif",
                "https://img2.gelbooru.com//images/64/1f/641f113bf24d70dc5925d243aba2a731.gif",
                "https://img2.gelbooru.com//images/6c/db/6cdb7915b068b4ca40ed0d1b0cb4af8d.gif",
                "https://img2.gelbooru.com//images/2a/6b/2a6b3f239bc01fedb0f5c337823e1921.gif",
                "https://img2.gelbooru.com//images/1b/3b/1b3b2cac1ce76b3185999f10579f89e9.gif",
                "https://img2.gelbooru.com//images/42/ac/42ac4bd3810ee4609d159bc7db0cd7b1.gif",
                "https://img2.gelbooru.com//images/ac/72/ac72d8dba50174e080890a9e87804ba8.gif",
                "https://img2.gelbooru.com//images/31/50/31504ab8b7a8adce589a1c3fa83cc46d.gif",
                "https://img2.gelbooru.com//images/ee/9d/ee9da4cf2b58e05bb8293c2e00a210aa.gif",
                "https://img2.gelbooru.com//images/ea/db/eadb656901af00752d558d26a5e7666e.gif",
                "https://img2.gelbooru.com//images/0c/01/0c01c525dfbe578801377c8b7045de68.gif",
                "https://img2.gelbooru.com//images/7b/40/7b400690057d8949c7afa33e97d24350.gif",
                "https://img2.gelbooru.com//images/0a/c7/0ac7e7ecb838115982a9470767a4f94c.gif",
                "https://img2.gelbooru.com//images/58/ad/58ad005d986a9a110dbec4146b4ff9d9.gif",
                "https://img2.gelbooru.com//images/f8/8f/f88f210baf9b38b0b01cd1a1f6577246.gif",
                "https://img2.gelbooru.com//images/7a/92/7a922faae90a3b28f4d334cadab33d38.gif",
                "https://img2.gelbooru.com//images/58/e7/58e7280c811f4aa84d2db0cf461813a4.gif",
                "https://img2.gelbooru.com//images/9c/e2/9ce27d4d2ad561d2d5b1b87b9fa84b2b.gif",
                "https://img2.gelbooru.com//images/4e/39/4e394caed7171bb7a0f521ed18c6b11b.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/581188907174199296/d620dad9-5c6e-4375-ad06-14b54c749989.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/581188913041899558/tumblr_mv5i7ktyev1sf82gro1_1280.gif"
        };
    }

}
