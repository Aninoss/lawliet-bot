package ServerStuff.WebCommunicationServer;

import CommandSupporters.Command;
import Constants.Locales;
import General.*;
import ServerStuff.WebCommunicationServer.Events.OnConnection;
import ServerStuff.WebCommunicationServer.Events.OnEventServerList;
import ServerStuff.WebCommunicationServer.Events.OnEventServerMembers;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.user.UserStatus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

public class WebComServer {

    public static final String EVENT_COMMANDLIST = "command_list";
    public static final String EVENT_SERVERLIST = "server_list";
    public static final String EVENT_SERVERMEMBERS = "server_members";

    public WebComServer(int port) {
        Configuration config = new Configuration();
        config.setHostname("127.0.0.1");
        config.setPort(port);

        final SocketIOServer webComServer = new SocketIOServer(config);

        webComServer.addConnectListener(new OnConnection(this));
        webComServer.addEventListener(EVENT_COMMANDLIST, JSONObject.class, new OnConnection(this));
        webComServer.addEventListener(EVENT_SERVERLIST, JSONObject.class, new OnEventServerList(this));
        webComServer.addEventListener(EVENT_SERVERMEMBERS, JSONObject.class, new OnEventServerMembers(this));

        webComServer.start();
        Runtime.getRuntime().addShutdownHook(new Thread(webComServer::stop));

        System.out.println("The WebCom server has been started!");
    }

    public JSONObject getLanguagePack(String key) {
        JSONObject jsonObject = new JSONObject();

        for(String localeString: Locales.LIST) {
            Locale locale = new Locale(localeString);
            jsonObject.put(locale.getDisplayName(), TextManager.getString(locale, TextManager.COMMANDS, key));
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

    public JSONObject getLanguagePackSpecs(String key, String commandTrigger) {
        JSONObject jsonObject = new JSONObject();

        for(String localeString: Locales.LIST) {
            Locale locale = new Locale(localeString);
            String str = Tools.solveVariablesOfCommandText(TextManager.getString(locale, TextManager.COMMANDS, key));
            if (!str.isEmpty())
                str = ("\n" + str).replace("\n", "\n• L." + commandTrigger + " ").substring(1);

            jsonObject.put(locale.getDisplayName(), str);
        }

        return jsonObject;
    }
}
