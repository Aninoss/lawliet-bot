package events.sync.events;

import java.util.HashMap;
import java.util.Locale;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.OnAlertListener;
import commands.Category;
import core.TextManager;
import org.json.JSONArray;
import org.json.JSONObject;
import events.sync.SyncLocaleUtil;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;

@SyncServerEvent(event = "COMMAND_LIST")
public class OnCommandList implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        JSONObject mainJSON = new JSONObject();
        JSONArray arrayJSON = new JSONArray();
        HashMap<Category, JSONObject> categories = new HashMap<>();

        //Add every command category
        for (Category category : Category.independentValues()) {
            JSONObject categoryJSON = new JSONObject();
            categoryJSON.put("id", category.getId());
            categoryJSON.put("name", SyncLocaleUtil.getLanguagePack(TextManager.COMMANDS, category.getId()));
            categoryJSON.put("commands", new JSONArray());
            categories.put(category, categoryJSON);
            arrayJSON.put(categoryJSON);
        }

        //Add every command
        for (Class<? extends Command> clazz : CommandContainer.getFullCommandList()) {
            Command command = CommandManager.createCommandByClass(clazz, Locale.US, "L.");
            String trigger = command.getTrigger();
            JSONObject commandJSON = new JSONObject();
            commandJSON.put("trigger", trigger);
            commandJSON.put("emoji", command.getCommandProperties().emoji());
            commandJSON.put("title", SyncLocaleUtil.getLanguagePack(command.getCategory(), trigger + "_title"));
            commandJSON.put("desc_short", SyncLocaleUtil.getLanguagePack(command.getCategory(), trigger + "_description"));
            commandJSON.put("desc_long", SyncLocaleUtil.getLanguagePack(command.getCategory(), trigger + "_helptext"));
            commandJSON.put("usage", SyncLocaleUtil.getCommandSpecs(command.getCategory(), trigger + "_usage", trigger));
            commandJSON.put("examples", SyncLocaleUtil.getCommandSpecs(command.getCategory(), trigger + "_examples", trigger));
            commandJSON.put("user_permissions", SyncLocaleUtil.getCommandPermissions(command));
            commandJSON.put("nsfw", command.getCommandProperties().nsfw());
            commandJSON.put("requires_user_permissions", command.isModCommand());
            commandJSON.put("can_be_tracked", command instanceof OnAlertListener);
            commandJSON.put("patron_only", command.getCommandProperties().patreonRequired());

            categories.get(command.getCategory()).getJSONArray("commands").put(commandJSON);
        }

        mainJSON.put("categories", arrayJSON);
        return mainJSON;
    }

}
