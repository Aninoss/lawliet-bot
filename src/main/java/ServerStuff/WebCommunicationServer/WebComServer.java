package ServerStuff.WebCommunicationServer;

/**
 * NOTE: This class requires the netty-socketio dependency.
 * But for some reason, it causes conflicts with the javacord dependency.
 * This feature is being postponed until the problems are solved.
 */

/*
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import CommandSupporters.CommandManager;
import Constants.Category;
import Constants.Locales;
import General.TextManager;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Locale;

public class WebComServer {

    private static final String EVENT_COMMANDLIST = "command_list";

    public WebComServer(int port) {
        Configuration config = new Configuration();
        config.setHostname("127.0.0.1");
        config.setPort(port);

        final SocketIOServer server = new SocketIOServer(config);

        //When the Lawliet web server connects with
        server.addConnectListener(socketIOClient -> {
            JSONArray mainJSON = new JSONArray();
            HashMap<String, JSONObject> categories = new HashMap<>();

            //Add every command category
            for(String categoryId: Category.LIST) {
                JSONObject categoryJSON = new JSONObject();
                categoryJSON.put("id", categoryId);
                categoryJSON.put("name", getLanguagePack(categoryId));
                categoryJSON.put("commands", new JSONArray());
                categories.put(categoryId, categoryJSON);
                mainJSON.put(categoryJSON);
            }

            //Add every command
            for(Class c: CommandContainer.getInstance().getCommandList()) {
                try {
                    Command command = CommandManager.createCommandByClass(c);
                    String trigger = command.getTrigger();

                    if (!command.isPrivate() && !trigger.equals("help")) {
                        JSONObject commandJSON = new JSONObject();
                        commandJSON.put("trigger", trigger);
                        commandJSON.put("title", getLanguagePack(trigger + "_title"));
                        commandJSON.put("desc_short", getLanguagePack(trigger + "_description"));
                        commandJSON.put("desc_long", getLanguagePack(trigger + "_helptext"));
                        commandJSON.put("usage", getLanguagePack(trigger + "_usage"));
                        commandJSON.put("examples", getLanguagePack(trigger + "_examples"));

                        categories.get(command.getCategory()).getJSONArray("commands").put(commandJSON);
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                    e.printStackTrace();
                }
            }

            //Send data
            socketIOClient.sendEvent(EVENT_COMMANDLIST, mainJSON.toString());
        });

        server.start();
        System.out.println("The WebCom server has been started!");
    }

    private JSONObject getLanguagePack(String key) {
        JSONObject jsonObject = new JSONObject();

        for(String localeString: Locales.LIST) {
            Locale locale = new Locale(localeString);
            try {
                jsonObject.put(locale.getDisplayName(), TextManager.getString(locale, TextManager.COMMANDS, key));
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        return jsonObject;
    }
}
*/
