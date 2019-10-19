package Commands.BotManagement;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Settings;
import General.EmbedFactory;
import General.TextManager;
import MySQL.DBUser;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "donate",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/flat-finance/128/dollar-icon.png",
        emoji = "\uD83D\uDCB8",
        executable = true
)
public class DonateCommand extends Command implements onRecievedListener {

    public DonateCommand() { super(); }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        StringBuilder donators = new StringBuilder();

        for(long userId: DBUser.getActiveDonators()) {
            try {
                User user = event.getApi().getUserById(userId).get();
                if (user != null) {
                    donators.append(user.getDiscriminatedName()).append("\n");
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        if (donators.length() == 0) donators.append(TextManager.getString(getLocale(), TextManager.GENERAL, "empty"));
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("template", Settings.SERVER_INVITE_URL, Settings.DONATION_URL, donators.toString()));
        eb.setImage("https://cdn.discordapp.com/attachments/499629904380297226/589143402851991552/donate.png");

        event.getChannel().sendMessage(eb).get();
        return true;
    }

}
