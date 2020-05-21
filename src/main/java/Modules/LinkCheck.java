package Modules;

import Constants.FisheryCategoryInterface;
import Core.Utils.InternetUtil;
import MySQL.Modules.FisheryUsers.DBFishery;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.util.concurrent.ExecutionException;

public class LinkCheck {

    public static boolean check(Message message) throws ExecutionException, InterruptedException {
        Server server = message.getServer().get();
        ServerTextChannel channel = message.getServerTextChannel().get();
        User author = message.getUserAuthor().get();

        if (server.getId() == 462405241955155979L && channel.getId() != 709477711512600596L && InternetUtil.stringHasURL(message.getContent())) {
            int level = DBFishery.getInstance().getBean(server.getId()).getUserBean(author.getId()).getPowerUp(FisheryCategoryInterface.ROLE).getLevel();
            if (level == 0) {
                author.sendMessage("Bevor du Links posten darfst, musst du erstmal den ersten Server-Rang erwerben!\nMehr Infos hier: <#608455541978824739>");
                server.getOwner().sendMessage(author.getMentionTag() + " hat Links gepostet!");
                message.delete().get();

                return false;
            }
        }

        return true;
    }

}
