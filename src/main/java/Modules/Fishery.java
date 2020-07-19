package Modules;

import Commands.FisherySettingsCategory.FisheryCommand;
import Constants.Category;
import Constants.FisheryCategoryInterface;
import Constants.Settings;
import Core.EmbedFactory;
import Core.TextManager;
import MySQL.Modules.FisheryUsers.FisheryUserBean;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class Fishery {

    public static long getFisheryRolePrice(Server server, List<Long> roleIds, int n) throws ExecutionException {
        ServerBean serverBean = DBServer.getInstance().getBean(server.getId());

        double priceIdealMin = serverBean.getFisheryRoleMin();
        double priceIdealMax = serverBean.getFisheryRoleMax();

        if (roleIds.size() == 1) return (long) priceIdealMin;

        double power = Math.pow(priceIdealMax / priceIdealMin, 1 / (double)(roleIds.size() - 1));

        double price = Math.pow(power, n);
        double priceMax = Math.pow(power, roleIds.size() - 1);

        return Math.round(price * (priceIdealMax / priceMax));
    }

    public static long getClaimValue(FisheryUserBean userBean) {
        return Math.round(userBean.getPowerUp(FisheryCategoryInterface.PER_DAY).getEffect() * 0.25);
    }

    public static void spawnTreasureChest(long serverId, ServerTextChannel channel) throws ExecutionException, InterruptedException {
        ServerBean serverBean = DBServer.getInstance().getBean(serverId);
        Locale locale = serverBean.getLocale();
        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setTitle(FisheryCommand.treasureEmoji + " " + TextManager.getString(locale, Category.FISHERY_SETTINGS, "fishery_treasure_title") + Settings.EMPTY_EMOJI)
                .setDescription(TextManager.getString(locale, Category.FISHERY_SETTINGS, "fishery_treasure_desription", FisheryCommand.keyEmoji))
                .setImage("https://cdn.discordapp.com/attachments/711665837114654781/711665915355201576/treasure_closed.png");

        Message message = channel.sendMessage(eb).get();
        message.addReaction(FisheryCommand.keyEmoji);
    }

}
