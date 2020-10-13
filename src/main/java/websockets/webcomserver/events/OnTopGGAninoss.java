package websockets.webcomserver.events;

import constants.AssetIds;
import constants.Locales;
import core.DiscordApiCollection;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import modules.Fishery;
import mysql.modules.bannedusers.DBBannedUsers;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryUserBean;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websockets.webcomserver.EventAbstract;
import websockets.webcomserver.WebComServer;

import java.util.Locale;

public class OnTopGGAninoss extends EventAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnTopGGAninoss.class);

    public OnTopGGAninoss(WebComServer webComServer, String event) {
        super(webComServer, event);
    }

    @Override
    protected JSONObject processData(JSONObject requestJSON, WebComServer webComServer) throws Exception {
        long userId = requestJSON.getLong("user");
        if (DBBannedUsers.getInstance().getBean().getUserIds().contains(userId))
            return null;

        String type = requestJSON.getString("type");

        if (type.equals("upvote")) {
            final Server server = DiscordApiCollection.getInstance().getServerById(AssetIds.ANINOSS_SERVER_ID).get();
            final Locale locale = new Locale(Locales.DE);
            server.getMemberById(userId).ifPresent(user -> {
                try {
                    final ServerTextChannel bumpChannel = server.getTextChannelById(713849992611102781L).get();

                    FisheryUserBean userBean = DBFishery.getInstance().getBean(server.getId()).getUserBean(userId);
                    long add = Fishery.getClaimValue(userBean);

                    String desc = TextManager.getString(locale, TextManager.GENERAL, "topgg_aninoss", user.getMentionTag(), server.getName(), StringUtil.numToString(add), "https://top.gg/servers/462405241955155979/vote");
                    bumpChannel.sendMessage(EmbedFactory.getEmbed().setDescription(desc)).get();
                    bumpChannel.sendMessage(userBean.changeValues(add, 0)).get();
                } catch (Throwable e) {
                    LOGGER.error("Exception in top.gg for Aninoss", e);
                }
            });
        } else {
            LOGGER.error("Wrong type: " + type);
        }

        return new JSONObject();
    }
}