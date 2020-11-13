package websockets.webcomserver;

import commands.Command;
import constants.Locales;
import core.ListGen;
import core.TextManager;
import core.utils.PermissionUtil;
import core.utils.StringUtil;
import org.java_websocket.WebSocket;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websockets.webcomserver.events.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Locale;

public class WebComServer {

    private static final WebComServer ourInstance = new WebComServer();
    public static WebComServer getInstance() { return ourInstance; }
    private WebComServer() { }

    private final static Logger LOGGER = LoggerFactory.getLogger(WebComServer.class);

    private static final String EVENT_COMMANDLIST = "command_list";
    private static final String EVENT_FAQLIST = "faq_list";
    private static final String EVENT_FR_FETCH = "fr_fetch";
    private static final String EVENT_FR_BOOST = "fr_boost";
    private static final String EVENT_FR_CAN_POST = "fr_can_post";
    private static final String EVENT_FR_POST = "fr_post";
    private static final String EVENT_TOPGG = "topgg";
    private static final String EVENT_TOPGG_ANINOSS = "topgg_aninoss";
    private static final String EVENT_DONATEBOT_IO = "donatebot.io";
    private static final String EVENT_INVITE = "invite";
    private static final String EVENT_SERVERSTATS = "serverstats";

    private CustomWebSocketServer server;
    private boolean started = false;

    public void start(int port) {
        if (started) return;
        started = true;

        server = new CustomWebSocketServer(new InetSocketAddress("localhost", port));

        server.addEventHandler(EVENT_COMMANDLIST, new OnCommandList(this, EVENT_COMMANDLIST));
        server.addEventHandler(EVENT_FAQLIST, new OnFAQList(this, EVENT_FAQLIST));
        server.addEventHandler(EVENT_FR_FETCH, new OnFRFetch(this, EVENT_FR_FETCH));
        server.addEventHandler(EVENT_FR_BOOST, new OnFRBoost(this, EVENT_FR_BOOST));
        server.addEventHandler(EVENT_FR_CAN_POST, new OnFRCanPost(this, EVENT_FR_CAN_POST));
        server.addEventHandler(EVENT_FR_POST, new OnFRPost(this, EVENT_FR_POST));
        server.addEventHandler(EVENT_SERVERSTATS, new OnServerStats(this, EVENT_SERVERSTATS));

        server.addEventHandler(EVENT_TOPGG, new OnTopGG(this, EVENT_TOPGG));
        server.addEventHandler(EVENT_TOPGG_ANINOSS, new OnTopGGAninoss(this, EVENT_TOPGG_ANINOSS));
        server.addEventHandler(EVENT_DONATEBOT_IO, new OnDonatebotIO(this, EVENT_DONATEBOT_IO));
        server.addEventHandler(EVENT_INVITE, new OnInvite(this, EVENT_INVITE));

        server.start();
        LOGGER.info("WebCom server started");
    }

    public JSONObject getLanguagePack(String category, String key) {
        JSONObject jsonObject = new JSONObject();

        for(String localeString: Locales.LIST) {
            Locale locale = new Locale(localeString);
            jsonObject.put(locale.getDisplayName(), TextManager.getString(locale, category, key).replace("%PREFIX", "L."));
        }

        return jsonObject;
    }

    public JSONObject getCommandPermissions(Command command) {
        JSONObject jsonObject = new JSONObject();

        for(String localeString: Locales.LIST) {
            Locale locale = new Locale(localeString);
            String permissionsList = new ListGen<Integer>().getList(PermissionUtil.permissionsToNumberList(command.getUserPermissions()), "", ListGen.SLOT_TYPE_NONE,
                    i -> TextManager.getString(locale, TextManager.PERMISSIONS, String.valueOf(i))
            );
            jsonObject.put(locale.getDisplayName(), permissionsList);
        }

        return jsonObject;
    }

    public JSONObject getCommandSpecs(String commandCategory, String key, String commandTrigger) {
        JSONObject jsonObject = new JSONObject();

        for(String localeString: Locales.LIST) {
            Locale locale = new Locale(localeString);
            String str = StringUtil.solveVariablesOfCommandText(TextManager.getString(locale, commandCategory, key));
            if (!str.isEmpty())
                str = ("\n" + str).replace("\n", "\nL." + commandTrigger + " ").substring(1);

            jsonObject.put(locale.getDisplayName(), str);
        }

        return jsonObject;
    }

    public void send(WebSocket webSocket, String event, JSONObject mainJSON) {
        server.send(webSocket, event, mainJSON);
    }

    public boolean isConnected() {
        if (server == null)
            return false;

        return server.getConnections().size() > 0;
    }

    public void stop() {
        if (server != null) {
            try {
                server.stop();
                started = false;
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Could not stop server", e);
            }
        }
    }

}
