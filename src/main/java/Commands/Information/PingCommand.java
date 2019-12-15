package Commands.Information;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Settings;
import General.DiscordApiCollection;
import General.EmbedFactory;
import MySQL.DBUser;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "ping",
        emoji = "\uD83C\uDFD3",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/128/arrow-refresh-4-icon.png",
        executable = false
)
public class PingCommand extends Command implements onRecievedListener {

    private boolean block = false;
    public PingCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        Instant startTime = event.getMessage().getCreationTimestamp();
        Message message = event.getServerTextChannel().get().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("pong_notime"))).get();
        Instant endTime = Instant.now();

        Duration duration = Duration.between(startTime, endTime);
        message.edit(EmbedFactory.getCommandEmbedStandard(this, getString("pong", String.valueOf((Math.abs(duration.getSeconds()*1000000000) + Math.abs(duration.getNano())) / 1000000)))).get();

        test(event);

        return true;
    }

    private void test(MessageCreateEvent event) throws Throwable {
        String[] gifs = {
                "https://i.gifer.com/Djbt.gif",
                "https://i.gifer.com/Djbt.gif",
                "https://i.gifer.com/KTGr.gif",
                "https://i.gifer.com/J1b0.gif",
                "https://i.gifer.com/HAnw.gif",
                "https://www.wykop.pl/cdn/c3201142/comment_tfROJ3JwtatzGcJxpnnFRiunICfsZsb5.gif",
                "https://media1.tenor.com/images/279c4716a469ace39b15e34d7fa3e7c4/tenor.gif?itemid=11487318",
                "https://data.whicdn.com/images/95252800/original.gif",
                "https://cdn.weeb.sh/images/rJrCj6_w-.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/579494706870747147/0.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/579494710247292929/1.gif"
        };

        System.out.print("super(");

        for(String url: gifs) {
            Message message = event.getChannel().sendMessage(new EmbedBuilder().setImage(url)).get();
            message.addReaction("✅").get();
            message.addReaction("❌").get();
            block = true;

            message.addReactionAddListener(e -> {
                if (!e.getUser().isBot()) {
                    block = false;
                    if (e.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase("✅")) {
                        System.out.print("\"");
                        System.out.print(url);
                        System.out.print("\",\n");
                    }
                    try {
                        message.delete().get();
                    } catch (InterruptedException | ExecutionException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }
    }

}
