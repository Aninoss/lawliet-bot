package modules.txt2img;

import constants.ExternalLinks;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.IncomingWebhookClient;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.WebhookClient;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Txt2ImgLogger {

    public static void log(String prompt, User user, String model, List<String> imageUrls) throws ExecutionException, InterruptedException {
        IncomingWebhookClient webhookClient = WebhookClient.createClient(user.getJDA(), System.getenv("TXT2IMG_LOG_WEBHOOK"));

        ArrayList<MessageEmbed> embeds = new ArrayList<>();
        for (String imageUrl : imageUrls) {
            MessageEmbed webhookEmbed = new EmbedBuilder()
                    .setTitle(model, ExternalLinks.LAWLIET_WEBSITE)
                    .setAuthor(user.getName() + " - " + user.getId(), null, user.getEffectiveAvatarUrl())
                    .setColor(new Color(254, 254, 254).getRGB())
                    .setDescription("```" + StringUtil.escapeMarkdownInField(prompt) + "```")
                    .setImage(imageUrl)
                    .build();
            embeds.add(webhookEmbed);
        }
        webhookClient.sendMessageEmbeds(embeds).queue(m -> {
            m.delete().queueAfter(1, TimeUnit.SECONDS);
            webhookClient.sendMessageEmbeds(embeds).queueAfter(1, TimeUnit.SECONDS); // weird Discord bug workaround
        });
    }

}
