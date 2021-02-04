package commands.runnables;

import commands.Command;
import core.EmbedFactory;
import core.TextManager;
import core.internet.HttpProperty;
import core.internet.HttpRequest;
import core.utils.MentionUtil;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public abstract class DeepAIAbstract extends Command {

    public DeepAIAbstract(Locale locale, String prefix) {
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

            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("success", result));
            eb.setImage(result);

            event.getChannel().sendMessage(eb).get();
            return true;
        }

        EmbedBuilder notFound = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "imagenotfound"));
        event.getChannel().sendMessage(notFound).get();
        return false;
    }


    private String processImage(URL url) throws ExecutionException, InterruptedException {
        for (DeepAIExample deepAiExample : getDeepAiExamples()) {
            if (url.toString().equals(deepAiExample.imageUrl)) {
                return deepAiExample.resultUrl;
            }
        }

        String query = "image=" + url.toString();

        HttpProperty[] properties = new HttpProperty[]{
                new HttpProperty("Api-Key", System.getenv("DEEPAI_TOKEN")),
                new HttpProperty("Content-Type", "application/x-www-form-urlencoded")
        };

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
