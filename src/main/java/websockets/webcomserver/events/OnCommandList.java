package websockets.webcomserver.events;

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

public class OnCommandList extends EventAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(OnCommandList.class);

    public OnCommandList(WebComServer webComServer, String event) {
        super(webComServer, event);
    }

    @Override
    protected JSONObject processData(JSONObject requestJSON, WebComServer webComServer) throws Exception {
        //TODO DEBUG
        LOGGER.info("#" + 0);

        JSONObject mainJSON = new JSONObject();
        JSONArray arrayJSON = new JSONArray();
        HashMap<String, JSONObject> categories = new HashMap<>();

        LOGGER.info("#" + 1);

        //Add every command category
        for(String categoryId : Category.LIST) {
            LOGGER.info("#" + 2 + categoryId);
            JSONObject categoryJSON = new JSONObject();
            categoryJSON.put("id", categoryId);
            categoryJSON.put("name", webComServer.getLanguagePack(TextManager.COMMANDS, categoryId));
            categoryJSON.put("commands", new JSONArray());
            categories.put(categoryId, categoryJSON);
            arrayJSON.put(categoryJSON);
        }

        LOGGER.info("#" + 3);
        //Add every command
        CommandContainer.getInstance().getFullCommandList()
                .forEach(clazz -> {
                    LOGGER.info("#" + 4 + clazz.getName());
                    try {
                        Command command = CommandManager.createCommandByClass(clazz, Locale.US, "L.");
                        LOGGER.info("#" + 5 + clazz.getName());
                        String trigger = command.getTrigger();
                        LOGGER.info("#" + 6 + clazz.getName());

                        if (!trigger.equals("help")) {
                            LOGGER.info("#" + 7 + clazz.getName());
                            JSONObject commandJSON = new JSONObject();
                            commandJSON.put("trigger", trigger);
                            commandJSON.put("emoji", command.getEmoji());
                            LOGGER.info("#" + 8 + clazz.getName());
                            commandJSON.put("title", webComServer.getLanguagePack(command.getCategory(), trigger + "_title"));
                            LOGGER.info("#" + 9 + clazz.getName());
                            commandJSON.put("desc_short", webComServer.getLanguagePack(command.getCategory(), trigger + "_description"));
                            LOGGER.info("#" + 10 + clazz.getName());
                            commandJSON.put("desc_long", webComServer.getLanguagePack(command.getCategory(), trigger + "_helptext"));
                            LOGGER.info("#" + 11 + clazz.getName());
                            commandJSON.put("usage", webComServer.getCommandSpecs(command.getCategory(), trigger + "_usage", trigger));
                            LOGGER.info("#" + 12 + clazz.getName());
                            commandJSON.put("examples", webComServer.getCommandSpecs(command.getCategory(), trigger + "_examples", trigger));
                            LOGGER.info("#" + 13 + clazz.getName());
                            commandJSON.put("user_permissions", webComServer.getCommandPermissions(command));
                            LOGGER.info("#" + 14 + clazz.getName());
                            commandJSON.put("nsfw", command.isNsfw());
                            LOGGER.info("#" + 15 + clazz.getName());
                            commandJSON.put("requires_user_permissions", command.isModCommand());
                            LOGGER.info("#" + 16 + clazz.getName());
                            commandJSON.put("can_be_tracked", command instanceof OnTrackerRequestListener);
                            LOGGER.info("#" + 17 + clazz.getName());
                            commandJSON.put("patron_only", command.isPatreonRequired());
                            LOGGER.info("#" + 18 + clazz.getName());

                            categories.get(command.getCategory()).getJSONArray("commands").put(commandJSON);
                            LOGGER.info("#" + 19 + clazz.getName());
                        }
                    } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                        LOGGER.error("Could not create class", e);
                    }
                });

        LOGGER.info("#" + 20);
        mainJSON.put("categories", arrayJSON);
        return mainJSON;
    }

}
