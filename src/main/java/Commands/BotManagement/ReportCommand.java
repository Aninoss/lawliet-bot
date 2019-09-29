package Commands.BotManagement;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Permission;
import General.EmbedFactory;
import General.TextManager;
import General.Tools;
import MySQL.DBServer;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.awt.*;
import java.util.ArrayList;

@CommandProperties(
    trigger = "report",
    thumbnail = "http://icons.iconarchive.com/icons/designbolts/free-multimedia/128/Studio-Mic-icon.png",
    emoji = "\uD83C\uDFA4",
    executable = false
)
public class ReportCommand extends Command implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public ReportCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        Server server = event.getServer().get();
        if (followedString.length() > 0) {
            if (followedString.length() <= 500) {
                User user = event.getMessage().getUserAuthor().get();
                sendReport(user, event.getServerTextChannel().get(), followedString);
                return true;
            } else {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                        TextManager.getString(getLocale(), TextManager.GENERAL, "args_too_long", "500"))).get();
                return false;
            }
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    getString("no_arg"))).get();
            return false;
        }
    }

    public void sendReport(User user, TextChannel reactionChannel, String content) throws Throwable {
        user.getApi().getOwner().get().sendMessage(EmbedFactory.getEmbed()
                .setAuthor(user.getName() + " (" + user.getIdAsString() + ")", "", user.getAvatar())
                .setDescription(content)).get();
        reactionChannel.sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("template"))).get();
    }
}
