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
    protected synchronized JSONObject processData(JSONObject requestJSON, WebComServer webComServer) throws Exception {
        LOGGER.info("### COMMAND LIST DEBUG ###"); //TODO
        JSONObject mainJSON = new JSONObject();
        LOGGER.info("0");
        JSONArray arrayJSON = new JSONArray();
        LOGGER.info("1");
        HashMap<String, JSONObject> categories = new HashMap<>();
        LOGGER.info("2");

        //Add every command category
        for (String categoryId : Category.LIST) {
            LOGGER.info("3");
            JSONObject categoryJSON = new JSONObject();
            categoryJSON.put("id", categoryId);
            categoryJSON.put("name", webComServer.getLanguagePack(TextManager.COMMANDS, categoryId));
            categoryJSON.put("commands", new JSONArray());
            categories.put(categoryId, categoryJSON);
            arrayJSON.put(categoryJSON);
        }

        LOGGER.info("4");
        //Add every command
        for (Class<? extends Command> clazz : CommandContainer.getInstance().getFullCommandList()) {
            LOGGER.info("5");
            try {
                Command command = CommandManager.createCommandByClass(clazz, Locale.US, "L.");
                LOGGER.info("6");
                String trigger = command.getTrigger();
                LOGGER.info("7");
                JSONObject commandJSON = new JSONObject();
                commandJSON.put("trigger", trigger);
                commandJSON.put("emoji", command.getEmoji());
                commandJSON.put("title", webComServer.getLanguagePack(command.getCategory(), trigger + "_title"));
                commandJSON.put("desc_short", webComServer.getLanguagePack(command.getCategory(), trigger + "_description"));
                commandJSON.put("desc_long", webComServer.getLanguagePack(command.getCategory(), trigger + "_helptext"));
                commandJSON.put("usage", webComServer.getCommandSpecs(command.getCategory(), trigger + "_usage", trigger));
                commandJSON.put("examples", webComServer.getCommandSpecs(command.getCategory(), trigger + "_examples", trigger));
                commandJSON.put("user_permissions", webComServer.getCommandPermissions(command));
                commandJSON.put("nsfw", command.isNsfw());
                commandJSON.put("requires_user_permissions", command.isModCommand());
                commandJSON.put("can_be_tracked", command instanceof OnTrackerRequestListener);
                commandJSON.put("patron_only", command.isPatreonRequired());

                LOGGER.info("7");
                categories.get(command.getCategory()).getJSONArray("commands").put(commandJSON);
                LOGGER.info("8");
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                LOGGER.error("Could not create class", e);
            }
        }

        LOGGER.info("9");
        mainJSON.put("categories", arrayJSON);
        LOGGER.info("10");
        return mainJSON;
    }

}
