package Commands.InformationCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Constants.Settings;
import Core.DiscordApiCollection;
import Core.EmbedFactory;
import Core.TextManager;
import MySQL.Modules.Donators.DBDonators;
import MySQL.Modules.Donators.DonatorBeanSlot;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.stream.Collectors;

@CommandProperties(
        trigger = "donate",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/flat-finance/128/dollar-icon.png",
        emoji = "\uD83D\uDCB8",
        executable = true
)
public class DonateCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        StringBuilder donators = new StringBuilder();

        for(DonatorBeanSlot donatorBean: DBDonators.getInstance().getBean().getMap().values().stream().filter(DonatorBeanSlot::isValid).collect(Collectors.toList())) {
            DiscordApiCollection.getInstance().getUserById(donatorBean.getUserId()).ifPresent(user ->
                    donators.append(user.getDiscriminatedName()).append("\n")
            );
        }

        if (donators.length() == 0) donators.append(TextManager.getString(getLocale(), TextManager.GENERAL, "empty"));
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("template", Settings.SERVER_INVITE_URL, Settings.DONATION_URL, donators.toString()));
        eb.setImage("https://cdn.discordapp.com/attachments/499629904380297226/589143402851991552/donate.png");

        event.getChannel().sendMessage(eb).get();
        return true;
    }

}
