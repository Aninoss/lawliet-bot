package commands.runnables;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.CommandEvent;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.MentionUtil;
import modules.DeepAI;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public abstract class DeepAIAbstract extends Command {

    public DeepAIAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        String url = null;
        List<Message.Attachment> attachmentList = event.isMessageReceivedEvent()
                ? event.getMessageReceivedEvent().getMessage().getAttachments()
                : Collections.emptyList();

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
            String result = processImage(event, url);
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("success", result))
                    .setImage(result);

            Button button = Button.of(ButtonStyle.LINK, result, TextManager.getString(getLocale(), TextManager.GENERAL, "download_image"));
            setComponents(button);
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return true;
        }

        EmbedBuilder notFound = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "imagenotfound"));
        drawMessageNew(notFound).exceptionally(ExceptionLogger.get());
        return false;
    }


    private String processImage(CommandEvent event, String imageUrl) throws ExecutionException, InterruptedException {
        for (DeepAIExample deepAiExample : getDeepAiExamples()) {
            if (imageUrl.equals(deepAiExample.imageUrl)) {
                return deepAiExample.resultUrl;
            }
        }

        event.deferReply();
        return new JSONObject(DeepAI.request(getUrl(), imageUrl).get().getBody())
                .getString("output_url");
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
