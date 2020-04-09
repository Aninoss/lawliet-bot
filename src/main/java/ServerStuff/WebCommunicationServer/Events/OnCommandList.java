package ServerStuff.WebCommunicationServer.Events;

import CommandListeners.OnTrackerRequestListener;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import CommandSupporters.CommandManager;
import Constants.Category;
import Core.TextManager;
import ServerStuff.WebCommunicationServer.WebComServer;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

public class OnCommandList implements ConnectListener, DataListener<JSONObject> {

    private WebComServer webComServer;

    public OnCommandList(WebComServer webComServer) {
        this.webComServer = webComServer;
    }

    @Override
    public void onConnect(SocketIOClient socketIOClient) {
        run(socketIOClient);
    }

    @Override
    public void onData(SocketIOClient socketIOClient, JSONObject jsonObject, AckRequest ackRequest) throws Exception {
        run(socketIOClient);
    }

    private void run(SocketIOClient socketIOClient) {
        JSONArray mainJSON = new JSONArray();
        HashMap<String, JSONObject> categories = new HashMap<>();

        //Add every command category
        for(String categoryId: Category.LIST) {
            JSONObject categoryJSON = new JSONObject();
            categoryJSON.put("id", categoryId);
            categoryJSON.put("name", webComServer.getLanguagePack(TextManager.COMMANDS, categoryId));
            categoryJSON.put("commands", new JSONArray());
            categories.put(categoryId, categoryJSON);
            mainJSON.put(categoryJSON);
        }

        //Add every command
        for(Class c: CommandContainer.getInstance().getCommandList()) {
            try {
                Command command = CommandManager.createCommandByClass(c);
                String trigger = command.getTrigger();

                if (!trigger.equals("help")) {
                    JSONObject commandJSON = new JSONObject();
                    commandJSON.put("trigger", trigger);
                    commandJSON.put("emoji", command.getEmoji());
                    commandJSON.put("title", webComServer.getLanguagePack(TextManager.COMMANDS, trigger + "_title"));
                    commandJSON.put("desc_short", webComServer.getLanguagePack(TextManager.COMMANDS, trigger + "_description"));
                    commandJSON.put("desc_long", webComServer.getLanguagePack(TextManager.COMMANDS, trigger + "_helptext"));
                    commandJSON.put("usage", webComServer.getCommandSpecs(trigger + "_usage", trigger));
                    commandJSON.put("examples", webComServer.getCommandSpecs(trigger + "_examples", trigger));
                    commandJSON.put("user_permissions", webComServer.getCommandPermissions(command));
                    commandJSON.put("nsfw", command.isNsfw());
                    commandJSON.put("requires_user_permissions", command.getUserPermissions() != 0);
                    commandJSON.put("can_be_tracked", command instanceof OnTrackerRequestListener);

                    categories.get(command.getCategory()).getJSONArray("commands").put(commandJSON);
                }
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }

        //Send data
        socketIOClient.sendEvent(WebComServer.EVENT_COMMANDLIST, mainJSON.toString());
    }

}
