package commands.runnables;

import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import core.EmbedFactory;
import core.TextManager;
import core.components.ActionRows;
import core.internet.HttpProperty;
import core.internet.HttpRequest;
import core.utils.MentionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.json.JSONObject;

public abstract class DeepAIAbstract extends Command {

    public DeepAIAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws ExecutionException, InterruptedException {
        String url = null;
        List<Message.Attachment> attachmentList = event.getMessage().getAttachments();

        if (attachmentList.size() > 0 && attachmentList.get(0).isImage()) {
            Message.Attachment messageAttachment = attachmentList.get(0);
            url = messageAttachment.getProxyUrl();
        } else {
            List<URL> imageList = MentionUtil.getImages(args).getList();
            if (imageList.size() > 0) {
                url = imageList.get(0).toString();
            }
        }

        if (url != null) {
            String result = processImage(url);
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("success", result))
                    .setImage(result);

            Button button = Button.of(ButtonStyle.LINK, result, TextManager.getString(getLocale(), TextManager.GENERAL, "download_image"));
            event.getChannel().sendMessageEmbeds(eb.build())
                    .setActionRows(ActionRows.of(button))
                    .queue();
            return true;
        }

        EmbedBuilder notFound = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "imagenotfound"));
        event.getChannel().sendMessageEmbeds(notFound.build()).queue();
        return false;
    }


    private String processImage(String url) throws ExecutionException, InterruptedException {
        for (DeepAIExample deepAiExample : getDeepAiExamples()) {
            if (url.equals(deepAiExample.imageUrl)) {
                return deepAiExample.resultUrl;
            }
        }

        String query = "image=" + url;

        HttpProperty[] properties = new HttpProperty[] {
                new HttpProperty("Api-Key", System.getenv("DEEPAI_TOKEN")),
                new HttpProperty("Content-Type", "application/x-www-form-urlencoded")
        };

        addLoadingReactionInstantly();
        String data = HttpRequest.getData(getUrl(), query, properties).get().getContent().get();
        JSONObject jsonObject = new JSONObject(data);
        return jsonObject.getString("output_url");
    }

    protected abstract String getUrl();

    protected abstract DeepAIExample[] getDeepAiExamples();


    protected static class DeepAIExample {

        private final String imageUrl;
        private final String resultUrl;

        public DeepAIExample(String imageUrl, String resultUrl) {
            this.imageUrl = imageUrl;
            this.resultUrl = resultUrl;
        }

    }

}
