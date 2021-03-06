package websockets.syncserver.events;

import constants.AssetIds;
import constants.Locales;
import core.ShardManager;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import modules.Fishery;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryUserBean;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import websockets.syncserver.SyncServerEvent;

import java.util.Locale;
import java.util.Optional;

@SyncServerEvent(event = "TOPGG_ANICORD")
public class OnTopGGAnicord extends OnTopGG {

    @Override
    protected void processUpvote(long userId, boolean isWeekend) {
        Guild guild = ShardManager.getInstance().getLocalGuildById(AssetIds.ANICORD_SERVER_ID).get();
        Locale locale = new Locale(Locales.DE);
        Optional.ofNullable(guild.getMemberById(userId)).ifPresent(user -> {
            TextChannel bumpChannel = guild.getTextChannelById(713849992611102781L);

            FisheryUserBean userBean = DBFishery.getInstance().retrieve(guild.getIdLong()).getUserBean(userId);
            long add = Fishery.getClaimValue(userBean);

            String desc = TextManager.getString(locale, TextManager.GENERAL, "topgg_aninoss", user.getAsMention(), guild.getName(), StringUtil.numToString(add), String.format("https://top.gg/servers/%d/vote", AssetIds.ANICORD_SERVER_ID));
            bumpChannel.sendMessage(EmbedFactory.getEmbedDefault().setDescription(desc).build()).queue();
            bumpChannel.sendMessage(userBean.changeValues(add, 0).build()).queue();
        });
    }

}
