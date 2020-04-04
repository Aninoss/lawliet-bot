package General.BotResources;

import General.Tools.RandomTools;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAttachment;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class ResourceFolder {
    private ArrayList<ResourceFile> fileList;
    private int fileNumber;
    private ArrayList<Integer> picked;
    private ServerTextChannel channel;

    public ResourceFolder(ServerTextChannel channel) throws ExecutionException, InterruptedException {
        this.channel = channel;
        updateFiles();
    }

    public ResourceFile getFile(String fileName) {
        for (ResourceFile file : fileList) {
            if (file.getFileName().equals(fileName)) return file;
        }
        return null;
    }

    public ResourceFile getRandomFile() {
        int n = RandomTools.pickFullRandom(picked, fileNumber);
        return fileList.get(n);
    }

    public int getSize() {
        return fileNumber;
    }

    public ServerTextChannel getServerTextChannel() {
        return channel;
    }

    public void updateFiles() throws ExecutionException, InterruptedException {
        fileList = new ArrayList<>();
        picked = new ArrayList<>();
        fileNumber = 0;
        for (Message message : channel.getMessages(100).get()) {
            for (MessageAttachment attachment : message.getAttachments()) {
                fileList.add(new ResourceFile(attachment.getFileName(), attachment.getUrl().toString()));
                fileNumber++;
            }
        }
    }
}
