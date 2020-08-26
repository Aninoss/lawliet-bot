package ServerStuff.WebCommunicationServer;

import CommandSupporters.Command;
import Constants.Locales;
import Core.*;
import Core.Utils.PermissionUtil;
import Core.Utils.StringUtil;
import ServerStuff.WebCommunicationServer.Events.*;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WebComServer {

    private final static Logger LOGGER = LoggerFactory.getLogger(WebComServer.class);

    private static final String EVENT_COMMANDLIST = "command_list";
    private static final String EVENT_FAQLIST = "faq_list";
    private static final String EVENT_SERVERLIST = "server_list";
    private static final String EVENT_SERVERMEMBERS = "server_members";
    private static final String EVENT_FR_FETCH = "fr_fetch";
    private static final String EVENT_FR_BOOST = "fr_boost";
    private static final String EVENT_FR_CAN_POST = "fr_can_post";
    private static final String EVENT_FR_POST = "fr_post";
    private static final String EVENT_TOPGG = "topgg";
    private static final String EVENT_DONATEBOT_IO = "donatebot.io";
    private static final String EVENT_FEEDBACK = "feedback";
    private static final String EVENT_INVITE = "invite";

    public WebComServer(int port) {
        new CustomThread(() -> {
            while(true) {
                try {
                    Configuration config = new Configuration();
                    config.setHostname("127.0.0.1");
                    config.setPort(port);

                    final SocketIOServer webComServer = new SocketIOServer(config);

                    webComServer.addConnectListener(new OnConnected());

                    webComServer.addEventListener(EVENT_COMMANDLIST, JSONObject.class, new OnCommandList(this, EVENT_COMMANDLIST));
                    webComServer.addEventListener(EVENT_FAQLIST, JSONObject.class, new OnFAQList(this, EVENT_FAQLIST));
                    webComServer.addEventListener(EVENT_SERVERLIST, JSONObject.class, new OnEventServerList(this, EVENT_SERVERLIST));
                    webComServer.addEventListener(EVENT_SERVERMEMBERS, JSONObject.class, new OnEventServerMembers(this, EVENT_SERVERMEMBERS));
                    webComServer.addEventListener(EVENT_FR_FETCH, JSONObject.class, new OnFRFetch(this, EVENT_FR_FETCH));
                    webComServer.addEventListener(EVENT_FR_BOOST, JSONObject.class, new OnFRBoost(this, EVENT_FR_BOOST));
                    webComServer.addEventListener(EVENT_FR_CAN_POST, JSONObject.class, new OnFRCanPost(this, EVENT_FR_CAN_POST));
                    webComServer.addEventListener(EVENT_FR_POST, JSONObject.class, new OnFRPost(this, EVENT_FR_POST));

                    webComServer.addEventListener(EVENT_TOPGG, JSONObject.class, new OnTopGG(this, EVENT_TOPGG));
                    webComServer.addEventListener(EVENT_DONATEBOT_IO, JSONObject.class, new OnDonatebotIO(this, EVENT_DONATEBOT_IO));
                    webComServer.addEventListener(EVENT_FEEDBACK, JSONObject.class, new OnFeedback(this, EVENT_FEEDBACK));
                    webComServer.addEventListener(EVENT_INVITE, JSONObject.class, new OnInvite(this, EVENT_INVITE));

                    webComServer.start();
                    LOGGER.info("WebCom Server started");
                    return;
                } catch (Throwable e) {
                    LOGGER.error("Exception in WebCom starter", e);
                    try {
                        Thread.sleep(10 * 1000);
                    } catch (InterruptedException interruptedException) {
                        LOGGER.error("Interrupted", e);
                    }
                }
            }
        }, "start_server").start();
    }

    public JSONObject getLanguagePack(String category,  String key) {
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
            String permissionsList = new ListGen<Integer>().getList(PermissionUtil.permissionsToNumberList(command.getUserPermissions()), "", ListGen.SLOT_TYPE_BULLET,
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
                str = ("\n" + str).replace("\n", "\n• L." + commandTrigger + " ").substring(1);

            jsonObject.put(locale.getDisplayName(), str);
        }

        return jsonObject;
    }

}
