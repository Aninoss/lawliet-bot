package commands.commandrunnables.externalcategory;

import commands.commandlisteners.CommandProperties;

import commands.Command;
import core.EmbedFactory;
import core.internet.HttpRequest;
import core.internet.HttpProperty;
import core.mention.MentionUtil;
import core.SecretManager;
import core.TextManager;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "waifu2x",
        withLoadingBar = true,
        emoji = "\uD83D\uDCC8",
        executable = false,
        aliases = {"waifu4x"}
)
public class Waifu2xCommand extends Command {

    public Waifu2xCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        URL url = null;
        List<MessageAttachment> attachmentList = event.getMessage().getAttachments();

        if (attachmentList.size() > 0 && attachmentList.get(0).isImage()) {
            MessageAttachment messageAttachment = attachmentList.get(0);
            url = messageAttachment.getProxyUrl();
        } else {
            ArrayList<URL> imageList = MentionUtil.getImages(followedString).getList();
            if (imageList.size() > 0) {
                url = imageList.get(0);
            }
        }

        if (url != null) {
            String result = processImage(url);

            EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("success", result));
            eb.setImage(result);

            event.getChannel().sendMessage(eb).get();
            return true;
        }

        EmbedBuilder notFound = EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "imagenotfound"));
        event.getChannel().sendMessage(notFound).get();
        return false;
    }


    private String processImage(URL url) throws IOException, ExecutionException, InterruptedException {
        if (url.toString().equals("https://i.pinimg.com/236x/a4/a6/43/a4a6430b557982c69b50bcf174c6077f.jpg")) return "https://cdn.discordapp.com/attachments/499629904380297226/611959216038477825/waifu2x.jpg";
        if (url.toString().equals("https://avatarfiles.alphacoders.com/699/thumb-69905.png")) return "https://cdn.discordapp.com/attachments/499629904380297226/611960284239626241/waifu2x2.jpg";

        String query = "image=" + url.toString();

        HttpProperty[] properties = new HttpProperty[]{
                new HttpProperty("Api-Key", SecretManager.getString("deepai.token")),
                new HttpProperty("Content-Type", "application/x-www-form-urlencoded")
        };

        String data = HttpRequest.getData("https://api.deepai.org/api/waifu2x", query, properties).get().getContent().get();
        JSONObject jsonObject = new JSONObject(data);
        return jsonObject.getString("output_url");
    }
}
