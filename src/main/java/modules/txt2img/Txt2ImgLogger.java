package modules.txt2img;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import core.utils.StringUtil;

import java.awt.*;
import java.util.concurrent.ExecutionException;

public class Txt2ImgLogger {

    private static final WebhookClient client = WebhookClient.withUrl(System.getenv("TXT2IMG_LOG_WEBHOOK"));

    public static void log(String prompt, long userId) throws ExecutionException, InterruptedException {
        WebhookEmbed webhookEmbed = new WebhookEmbedBuilder()
                .setColor(new Color(254, 254, 254).getRGB())
                .setDescription("```" + StringUtil.escapeMarkdownInField(prompt) + "```\n- User ID: " + userId)
                .build();
        client.send(webhookEmbed).get();
    }

}
