package General.BotResources;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class ResourceManager {
    public static final int RESOURCES = 0, SPAM = 1;
    private static ArrayList<ResourceFolder> folderList;

    public static String getFile(int folder, String fileName) throws ExecutionException, InterruptedException {
        return folderList.get(folder).getFile(fileName).getURL();
    }

    public static ResourceFolder getFolder(int folder) throws ExecutionException, InterruptedException {
        return folderList.get(folder);
    }

    public static void setUp(Server server) throws ExecutionException, InterruptedException {
        System.out.println("Server resources have been loaded!");
        folderList = new ArrayList<>();
        folderList.add(new ResourceFolder(server.getTextChannelById(499629904380297226L).get()));
        folderList.add(new ResourceFolder(server.getTextChannelById(499640076150636555L).get()));
    }

    public static void updateFiles(ServerTextChannel channel) throws ExecutionException, InterruptedException {
        for(ResourceFolder folder: folderList) {
            if (folder.getServerTextChannel().equals(channel)) {
                folder.updateFiles();
                System.out.println("Datei aktualisiert!");
                return;
            }
        }
    }
}
