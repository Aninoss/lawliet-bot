package Commands.Interactions;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

public class YaoiKissCommand extends InteractionCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public YaoiKissCommand() {
        super();
        trigger = "yaoikiss";
        emoji = "\uD83D\uDC68\u200D❤️\u200D\uD83D\uDC68";
        nsfw = false;
        gifs = new String[]{
                "https://media.giphy.com/media/dbY5Cq2rnbLNK/giphy.gif",
                "https://media.giphy.com/media/uJLxLIhd8pnX2/giphy.gif",
                "https://media.giphy.com/media/zTUTG5hsRwl0s/giphy.gif",
                "https://media.giphy.com/media/75toITBY1d24o/giphy.gif",
                "https://media.giphy.com/media/ulYHTVDYD6hUI/giphy.gif",
                "https://media.giphy.com/media/7z1xs4Fl9Kb8A/giphy.gif",
                "https://media.giphy.com/media/XthH8c95G5GpO/giphy.gif",
                "https://media.giphy.com/media/RX4ZhIdVvNkty/giphy.gif",
                "https://media.giphy.com/media/7j5fVzUhE2NH2/giphy.gif",
                "https://media.giphy.com/media/9coScZJ5G3ULm/giphy.gif",
                "https://media.giphy.com/media/OVVDhwkEbAkSc/giphy.gif",
                "https://media.giphy.com/media/aIoQynMLx3uF2/giphy.gif",
                "https://media.giphy.com/media/6vJEqAaOVA1X2/giphy.gif",
                "https://media.giphy.com/media/bJDin9kCsznpK/giphy.gif",
                "https://media.giphy.com/media/lfHSS824l0KVG/giphy.gif",
                "https://media.giphy.com/media/QK6tJPzmiNJG8/giphy.gif",
                "https://media.giphy.com/media/117UiFNyf7lLyw/giphy.gif",
                "https://media.giphy.com/media/AeyqSQLnaSCvm/giphy.gif",
                "https://media.giphy.com/media/EeEx2C4tA4f9m/giphy.gif",
                "https://media.giphy.com/media/nISHppsUAzosM/giphy.gif",
                "https://i.pinimg.com/originals/6c/e4/a4/6ce4a4d66c05a0c97f7d7948a597c0cf.gif",
                "http://data.whicdn.com/images/29771922/nezumi-shion-yaoi-Favim.com-372782_large.gif",
                "https://data.whicdn.com/images/247721120/original.gif",
                "https://data.whicdn.com/images/212056054/original.gif",
                "https://i.pinimg.com/originals/55/33/f6/5533f6796eae4c5c19d552d37e16343b.gif",
                "https://data.whicdn.com/images/231810449/original.gif",
                "http://sakuu.s.a.pic.centerblog.net/ca6c1dd5.gif",
                "http://coquelico.c.o.pic.centerblog.net/319c14c5ff11e76ed8913e98cc020a15.gif",
                "https://static.tumblr.com/847657741cb58516e429cfc8259b5f91/mt6r6jm/PwGna9yh1/tumblr_static_7jj007qzango0g480kw8ocsw0.gif",
                "http://pa1.narvii.com/6325/747d53e7bc5ea7373901b2dc9fbabb5ca472305f_00.gif",
                "https://78.media.tumblr.com/11365936b99a3ccd49d30b1d30e7a2ab/tumblr_noh8l0iQS21uto38ho1_400.gif",
                "https://pa1.narvii.com/5794/e0fcdf597e3e4237fc170fa98fd11cbd311a656c_hq.gif",
                "https://i.pinimg.com/originals/78/66/08/7866086e00ad06d39708de8f628761c3.gif",
                "https://img2.gelbooru.com//images/88/f0/88f0e29bb95a4c6d4bb0bce5af1d3884.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/581188913234706460/tumblr_o8m4vc9Lj81vxpanzo1_500.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/581188915503956006/e1208176-91ef-46e6-b8ef-3c9b4d21ba4b.gif",
                "https://media1.tenor.com/images/77c21f541916b0f8f673cdd70a6b78cf/tenor.gif?itemid=12992435",
                "https://media1.tenor.com/images/3e659c681c779d15d597f84ab5bfc1e3/tenor.gif?itemid=5539915"
        };
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
