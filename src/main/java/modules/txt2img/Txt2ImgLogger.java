package modules.txt2img;

import constants.ExternalLinks;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Txt2ImgLogger {

    public static void log(String prompt, String negativePrompt, Member member, String model, List<String> imageUrls) throws ExecutionException, InterruptedException {
        Guild guild = member.getGuild();
        User user = member.getUser();
        IncomingWebhookClient webhookClient = WebhookClient.createClient(user.getJDA(), System.getenv("TXT2IMG_LOG_WEBHOOK"));

        ArrayList<MessageEmbed> embeds = new ArrayList<>();
        for (String imageUrl : imageUrls) {
            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle(model, ExternalLinks.LAWLIET_WEBSITE)
                    .setAuthor(user.getName() + " - " + user.getId(), null, user.getEffectiveAvatarUrl())
                    .setColor(new Color(254, 254, 254).getRGB())
                    .addField("Prompt", "```" + StringUtil.shortenString(StringUtil.escapeMarkdownInField(prompt), MessageEmbed.VALUE_MAX_LENGTH - 6) + "```", false)
                    .setImage(imageUrl)
                    .setFooter(guild.getName() + " - " + guild.getId());
            if (!negativePrompt.isEmpty()) {
                eb.addField("Negative Prompt", "```" + StringUtil.shortenString(StringUtil.escapeMarkdownInField(negativePrompt), MessageEmbed.VALUE_MAX_LENGTH - 6) + "```", false);
            }
            embeds.add(eb.build());
        }
        webhookClient.sendMessageEmbeds(embeds).queue(m -> {
            m.delete().queueAfter(1, TimeUnit.SECONDS);
            webhookClient.sendMessageEmbeds(embeds).queueAfter(1, TimeUnit.SECONDS); // weird Discord bug workaround
        });
    }

}
