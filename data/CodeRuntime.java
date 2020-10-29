import commands.listeners.OnTrackerRequestListener;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import constants.Category;
import core.TextManager;
import websockets.webcomserver.EventAbstract;
import websockets.webcomserver.WebComServer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Locale;

public class CodeRuntime {

    public static int run() {
        JSONObject mainJSON = new JSONObject();
        JSONArray arrayJSON = new JSONArray();
        HashMap<String, JSONObject> categories = new HashMap<>();
        System.out.println(0);

        //Add every command category
        for(String categoryId : Category.LIST) {
            JSONObject categoryJSON = new JSONObject();
            categoryJSON.put("id", categoryId);
            categoryJSON.put("name", "YEET");
            categoryJSON.put("commands", new JSONArray());
            categories.put(categoryId, categoryJSON);
            arrayJSON.put(categoryJSON);
        }
        System.out.println(1);

        //Add every command
        CommandContainer.getInstance().getFullCommandList()
                .forEach(clazz -> {
                    try {
                        Command command = CommandManager.createCommandByClass(clazz, Locale.US, "L.");
                        String trigger = command.getTrigger();

                        if (!trigger.equals("help")) {
                            JSONObject commandJSON = new JSONObject();
                            commandJSON.put("trigger", trigger);
                            commandJSON.put("emoji", command.getEmoji());
                            commandJSON.put("title", "YEET");
                            commandJSON.put("desc_short", "YEET");
                            commandJSON.put("desc_long", "YEET");
                            commandJSON.put("usage", "YEET");
                            commandJSON.put("examples", "YEET");
                            commandJSON.put("user_permissions", "YEET");
                            commandJSON.put("nsfw", command.isNsfw());
                            commandJSON.put("requires_user_permissions", command.isModCommand());
                            commandJSON.put("can_be_tracked", command instanceof OnTrackerRequestListener);
                            commandJSON.put("patron_only", command.isPatreonRequired());

                            categories.get(command.getCategory()).getJSONArray("commands").put(commandJSON);
                        }
                    } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                });

        System.out.println(2);
        mainJSON.put("categories", arrayJSON);
        System.out.println(3);
        return 0;
    }

}
