package ServerStuff.WebCommunicationServer;

import CommandSupporters.Command;
import Constants.Locales;
import Core.*;
import Core.Utils.StringUtil;
import ServerStuff.WebCommunicationServer.Events.*;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WebComServer {

    final static Logger LOGGER = LoggerFactory.getLogger(WebComServer.class);

    public static final String EVENT_COMMANDLIST = "command_list";
    public static final String EVENT_FAQLIST = "faq_list";
    public static final String EVENT_SERVERLIST = "server_list";
    public static final String EVENT_SERVERMEMBERS = "server_members";
    public static final String EVENT_TOPGG = "topgg";
    public static final String EVENT_DONATEBOT_IO = "donatebot.io";
    public static final String EVENT_FEEDBACK = "feedback";

    public WebComServer(int port) {
        new CustomThread(() -> {
            while(true) {
                try {
                    Configuration config = new Configuration();
                    config.setHostname("127.0.0.1");
                    config.setPort(port);

                    final SocketIOServer webComServer = new SocketIOServer(config);

                    webComServer.addConnectListener(new OnCommandList(this));
                    webComServer.addConnectListener(new OnFAQList(this));

                    webComServer.addEventListener(EVENT_COMMANDLIST, JSONObject.class, new OnCommandList(this));
                    webComServer.addEventListener(EVENT_FAQLIST, JSONObject.class, new OnFAQList(this));
                    webComServer.addEventListener(EVENT_SERVERLIST, JSONObject.class, new OnEventServerList());
                    webComServer.addEventListener(EVENT_SERVERMEMBERS, JSONObject.class, new OnEventServerMembers());
                    webComServer.addEventListener(EVENT_TOPGG, JSONObject.class, new OnTopGG());
                    webComServer.addEventListener(EVENT_DONATEBOT_IO, JSONObject.class, new OnDonatebotIO());
                    webComServer.addEventListener(EVENT_FEEDBACK, JSONObject.class, new OnFeedback());

                    webComServer.start();
                    LOGGER.info("WebCom Server started");
                    return;
                } catch (Throwable e) {
                    LOGGER.error("Exception in WebCom starter", e);
                    try {
                        Thread.sleep(10 * 1000);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
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
            String permissionsList = new ListGen<Integer>().getList(PermissionCheck.permissionsToNumberList(command.getUserPermissions()), "", ListGen.SLOT_TYPE_BULLET,
                    i -> TextManager.getString(locale, TextManager.PERMISSIONS, String.valueOf(i))
            );
            jsonObject.put(locale.getDisplayName(), permissionsList);
        }

        return jsonObject;
    }

    public JSONObject getCommandSpecs(String key, String commandTrigger) {
        JSONObject jsonObject = new JSONObject();

        for(String localeString: Locales.LIST) {
            Locale locale = new Locale(localeString);
            String str = StringUtil.solveVariablesOfCommandText(TextManager.getString(locale, TextManager.COMMANDS, key));
            if (!str.isEmpty())
                str = ("\n" + str).replace("\n", "\n• L." + commandTrigger + " ").substring(1);

            jsonObject.put(locale.getDisplayName(), str);
        }

        return jsonObject;
    }
}
