package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;

import Commands.InteractionAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "kill",
        emoji = "☠️",
        executable = true,
        aliases = {"die"}
)
public class KillCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/28c19622e8d7362ccc140bb24e4089ec/tenor.gif?itemid=9363668",
                "https://media1.tenor.com/images/2c945adbbc31699861f411f86ce8058f/tenor.gif?itemid=5459053",
                "https://media1.tenor.com/images/eb7fc71c616347e556ab2b4c813700d1/tenor.gif?itemid=5840101",
                "https://media1.tenor.com/images/db1136b19969ca0809daffc3d93fc848/tenor.gif?itemid=9983954",
                "https://media1.tenor.com/images/25f853a32137e24b11cd13bc2142f63a/tenor.gif?itemid=7172028",
                "https://media1.tenor.com/images/405efa8099e014cfeb8178c5b3801322/tenor.gif?itemid=13843226",
                "https://media1.tenor.com/images/364e3dcc7ce079c06e79d110eb85f4cf/tenor.gif?itemid=4885036",
                "https://media1.tenor.com/images/e1a8a560a7d532442f6d4e00d6f131a4/tenor.gif?itemid=14424096",
                "https://media1.tenor.com/images/1b189d99ba29bc7b4aa8f24f4827c12e/tenor.gif?itemid=13726342",
                "https://media1.tenor.com/images/4776a4baa6eb9813ecfde2a16071d96e/tenor.gif?itemid=4775517",
                "https://media1.tenor.com/images/2706b52a7bf7b34cfe43d7f49381ee85/tenor.gif?itemid=13617665",
                "https://media.giphy.com/media/eYAYL9QMjZyE0/giphy.gif",
                "https://image.ibb.co/grE8d0/608343a4d81191cfd2c620e0adb79a9203dd14b5.gif",
                "https://media1.tenor.com/images/c56b3226a771460fce710c60bae65723/tenor.gif?itemid=12647658",
                "https://media1.tenor.com/images/62c6fd1108b5bd5bb457715b7e414939/tenor.gif?itemid=16665054",
                "https://media1.tenor.com/images/3447f0676529e8a4965569c8d02bd083/tenor.gif?itemid=16950128",
                "https://cdn.discordapp.com/attachments/686251876391583843/703974097079763004/source.gif",
                "https://i.pinimg.com/originals/31/c0/77/31c0776b109b76143605887067803464.gif",
                "https://media1.tenor.com/images/cbc573cc5f8aade501c6b3b461aa7f70/tenor.gif",
                "https://media1.tenor.com/images/52c4d55c27725df1b0a35178ad7cbc08/tenor.gif",
                "https://i.pinimg.com/originals/56/22/23/5622238b635ce9b23ff7254130653b05.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/708657617295376384/OK6W_koKDTOqqqLDbIoPAmEZWmUcb5kvBmbyOpGHL-s.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709429751072161822/Black_Clover_Demon_Kill.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/710877980536340530/unnamed.gif",
                "https://cdn.discordapp.com/attachments/708252321221443625/713734195754827846/Black_Clover_Devil_Kill.gif",
                "https://cdn.discordapp.com/attachments/708252321221443625/713734600416821349/Mob_Dimple_Kill.gif"
        };
    }

}
