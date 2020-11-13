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
        JSONArray arrayJSON = new JSONArray();
        HashMap<String, JSONObject> categories = new HashMap<>();

        //Add every command category
        for (String categoryId : Category.LIST) {
            JSONObject categoryJSON = new JSONObject();
            categoryJSON.put("id", categoryId);
            categoryJSON.put("name", webComServer.getLanguagePack(TextManager.COMMANDS, categoryId));
            categoryJSON.put("commands", new JSONArray());
            categories.put(categoryId, categoryJSON);
            arrayJSON.put(categoryJSON);
        }

        //Add every command
        for (Class<? extends Command> clazz : CommandContainer.getInstance().getFullCommandList()) {
            try {
                Command command = CommandManager.createCommandByClass(clazz, Locale.US, "L.");
                String trigger = command.getTrigger();
                JSONObject commandJSON = new JSONObject();
                LOGGER.info("0" + trigger);
                commandJSON.put("trigger", trigger);
                LOGGER.info("1");
                commandJSON.put("emoji", command.getEmoji());
                LOGGER.info("2");
                commandJSON.put("title", webComServer.getLanguagePack(command.getCategory(), trigger + "_title"));
                LOGGER.info("3");
                commandJSON.put("desc_short", webComServer.getLanguagePack(command.getCategory(), trigger + "_description"));
                LOGGER.info("4");
                commandJSON.put("desc_long", webComServer.getLanguagePack(command.getCategory(), trigger + "_helptext"));
                LOGGER.info("5");
                commandJSON.put("usage", webComServer.getCommandSpecs(command.getCategory(), trigger + "_usage", trigger));
                LOGGER.info("6");
                commandJSON.put("examples", webComServer.getCommandSpecs(command.getCategory(), trigger + "_examples", trigger));
                LOGGER.info("7");
                commandJSON.put("user_permissions", webComServer.getCommandPermissions(command));
                LOGGER.info("8");
                commandJSON.put("nsfw", command.isNsfw());
                LOGGER.info("9");
                commandJSON.put("requires_user_permissions", command.isModCommand());
                LOGGER.info("10");
                commandJSON.put("can_be_tracked", command instanceof OnTrackerRequestListener);
                LOGGER.info("11");
                commandJSON.put("patron_only", command.isPatreonRequired());
                LOGGER.info("12");

                categories.get(command.getCategory()).getJSONArray("commands").put(commandJSON);
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                LOGGER.error("Could not create class", e);
            }
        }

        LOGGER.info("13");
        mainJSON.put("categories", arrayJSON);
        LOGGER.info("14");
        return mainJSON;
    }

}
