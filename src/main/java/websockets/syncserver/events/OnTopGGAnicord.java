package websockets.syncserver.events;

import constants.AssetIds;
import constants.Locales;
import core.DiscordApiManager;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import modules.Fishery;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryUserBean;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.util.logging.ExceptionLogger;
import websockets.syncserver.SyncServerEvent;

import java.util.Locale;

@SyncServerEvent(event = "TOPGG_ANICORD")
public class OnTopGGAnicord extends OnTopGG {

    @Override
    protected void processUpvote(long userId, boolean isWeekend) {
        Server server = DiscordApiManager.getInstance().getLocalServerById(AssetIds.ANICORD_SERVER_ID).get();
        Locale locale = new Locale(Locales.DE);
        server.getMemberById(userId).ifPresent(user -> {
            final ServerTextChannel bumpChannel = server.getTextChannelById(713849992611102781L).get();

            FisheryUserBean userBean = DBFishery.getInstance().getBean(server.getId()).getUserBean(userId);
            long add = Fishery.getClaimValue(userBean);

            String desc = TextManager.getString(locale, TextManager.GENERAL, "topgg_aninoss", user.getMentionTag(), server.getName(), StringUtil.numToString(add), String.format("https://top.gg/servers/%d/vote", AssetIds.ANICORD_SERVER_ID));
            bumpChannel.sendMessage(EmbedFactory.getEmbedDefault().setDescription(desc)).exceptionally(ExceptionLogger.get());
            bumpChannel.sendMessage(userBean.changeValues(add, 0)).exceptionally(ExceptionLogger.get());
        });
    }

}
