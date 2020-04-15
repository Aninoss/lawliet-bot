package Core.BotResources;

import ServerStuff.WebCommunicationServer.Events.OnTopGG;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class ResourceManager {

    final static Logger LOGGER = LoggerFactory.getLogger(ResourceManager.class);
    public static final int RESOURCES = 0, SPAM = 1;
    private static ArrayList<ResourceFolder> folderList;

    public static String getFile(int folder, String fileName) throws ExecutionException, InterruptedException {
        return folderList.get(folder).getFile(fileName).getURL();
    }

    public static ResourceFolder getFolder(int folder) throws ExecutionException, InterruptedException {
        return folderList.get(folder);
    }

    public static void setUp(Server server) throws ExecutionException, InterruptedException {
        folderList = new ArrayList<>();
        folderList.add(new ResourceFolder(server.getTextChannelById(499629904380297226L).get()));
        folderList.add(new ResourceFolder(server.getTextChannelById(499640076150636555L).get()));
        LOGGER.debug("Resources loaded");
    }

}
