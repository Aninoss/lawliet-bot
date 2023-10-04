package modules.txt2img;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import constants.ExternalLinks;
import core.utils.StringUtil;
import net.dv8tion.jda.api.entities.User;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Txt2ImgLogger {

    private static final WebhookClient client = WebhookClient.withUrl(System.getenv("TXT2IMG_LOG_WEBHOOK"));

    public static void log(String prompt, User user, String model, List<String> imageUrls) throws ExecutionException, InterruptedException {
        ArrayList<WebhookEmbed> webhookEmbeds = new ArrayList<>();
        for (String imageUrl : imageUrls) {
            WebhookEmbed webhookEmbed = new WebhookEmbedBuilder()
                    .setTitle(new WebhookEmbed.EmbedTitle(model, ExternalLinks.LAWLIET_WEBSITE))
                    .setAuthor(new WebhookEmbed.EmbedAuthor(user.getName() + " - " + user.getId(), user.getEffectiveAvatarUrl(), null))
                    .setColor(new Color(254, 254, 254).getRGB())
                    .setDescription("```" + StringUtil.escapeMarkdownInField(prompt) + "```")
                    .setImageUrl(imageUrl)
                    .build();
            webhookEmbeds.add(webhookEmbed);
        }
        client.send(webhookEmbeds).get();
    }

}
