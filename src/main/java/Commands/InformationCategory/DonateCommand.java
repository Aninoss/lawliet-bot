package Commands.InformationCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Settings;
import General.DiscordApiCollection;
import General.EmbedFactory;
import General.TextManager;
import MySQL.DBUser;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Optional;

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
            Optional<User> userOptional = DiscordApiCollection.getInstance().getUserById(userId);
            userOptional.ifPresent(user -> donators.append(user.getDiscriminatedName()).append("\n"));
        }

        if (donators.length() == 0) donators.append(TextManager.getString(getLocale(), TextManager.GENERAL, "empty"));
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("template", Settings.SERVER_INVITE_URL, Settings.DONATION_URL, donators.toString()));
        eb.setImage("https://cdn.discordapp.com/attachments/499629904380297226/589143402851991552/donate.png");

        event.getChannel().sendMessage(eb).get();
        return true;
    }

}
