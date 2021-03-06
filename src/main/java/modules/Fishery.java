package modules;

import commands.runnables.fisherysettingscategory.FisheryCommand;
import constants.Category;
import constants.Emojis;
import constants.FisheryCategoryInterface;
import core.EmbedFactory;
import core.TextManager;
import mysql.modules.fisheryusers.FisheryUserBean;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class Fishery {

    public static long getFisheryRolePrice(Guild guild, List<Long> roleIds, int n) throws ExecutionException {
        ServerBean serverBean = DBServer.getInstance().retrieve(guild.getIdLong());

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

    public static void spawnTreasureChest(long serverId, TextChannel channel) throws ExecutionException, InterruptedException {
        ServerBean serverBean = DBServer.getInstance().retrieve(serverId);
        Locale locale = serverBean.getLocale();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(FisheryCommand.treasureEmoji + " " + TextManager.getString(locale, Category.FISHERY_SETTINGS, "fishery_treasure_title") + Emojis.EMPTY_EMOJI)
                .setDescription(TextManager.getString(locale, Category.FISHERY_SETTINGS, "fishery_treasure_desription", FisheryCommand.keyEmoji))
                .setImage("https://cdn.discordapp.com/attachments/711665837114654781/711665915355201576/treasure_closed.png");

        channel.sendMessage(eb.build())
                .flatMap(m -> m.addReaction(FisheryCommand.keyEmoji)).queue();
    }

}
