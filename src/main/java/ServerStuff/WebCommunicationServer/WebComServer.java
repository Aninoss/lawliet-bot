package ServerStuff.WebCommunicationServer;

import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import CommandSupporters.CommandManager;
import Constants.Category;
import Constants.Locales;
import General.TextManager;
import General.Tools;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import org.javacord.api.DiscordApi;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Locale;

public class WebComServer {

    private static final String EVENT_COMMANDLIST = "command_list";
    private DiscordApi api;

    public WebComServer(int port, DiscordApi api) {
        this.api = api;

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
                        commandJSON.put("emoji", command.getEmoji());
                        commandJSON.put("title", getLanguagePack(trigger + "_title"));
                        commandJSON.put("desc_short", getLanguagePack(trigger + "_description"));
                        commandJSON.put("desc_long", getLanguagePack(trigger + "_helptext"));
                        commandJSON.put("usage", getLanguagePackSpecs(trigger + "_usage", trigger));
                        commandJSON.put("examples", getLanguagePackSpecs(trigger + "_examples", trigger));
                        commandJSON.put("nsfw", command.isNsfw());
                        commandJSON.put("requires_user_permissions", command.getUserPermissions() != 0);

                        categories.get(command.getCategory()).getJSONArray("commands").put(commandJSON);
                    }
                } catch (IllegalAccessException | InstantiationException e) {
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return jsonObject;
    }

    private JSONObject getLanguagePackSpecs(String key, String commandTrigger) {
        JSONObject jsonObject = new JSONObject();

        for(String localeString: Locales.LIST) {
            Locale locale = new Locale(localeString);
            try {
                String str = Tools.solveVariablesOfCommandText(
                        TextManager.getString(locale, TextManager.COMMANDS, key),
                        api
                );
                if (!str.isEmpty())
                    str = ("\n" + str).replace("\n", "\n• L." + commandTrigger + " ").substring(1);

                jsonObject.put(locale.getDisplayName(), str);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return jsonObject;
    }
}
