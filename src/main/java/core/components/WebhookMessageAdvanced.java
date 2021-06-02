package core.components;

import java.util.List;
import club.minnced.discord.webhook.IOUtil;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.MessageAttachment;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookMessage;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

public class WebhookMessageAdvanced extends WebhookMessage {

    private final List<ActionRow> components;

    public WebhookMessageAdvanced(String username, String avatarUrl, String content, List<WebhookEmbed> embeds,
                                  boolean isTTS, MessageAttachment[] files, AllowedMentions allowedMentions,
                                  List<ActionRow> components) {
        super(username, avatarUrl, content, embeds, isTTS, files, allowedMentions);
        this.components = components;
    }

    @Override
    public @NotNull RequestBody getBody() {
        final JSONObject payload = new JSONObject();
        payload.put("content", content);
        if (embeds != null && !embeds.isEmpty()) {
            final JSONArray array = new JSONArray();
            for (WebhookEmbed embed : embeds) {
                array.put(embed.reduced());
            }
            payload.put("embeds", array);
        } else {
            payload.put("embeds", new JSONArray());
        }
        if (avatarUrl != null)
            payload.put("avatar_url", avatarUrl);
        if (username != null)
            payload.put("username", username);
        payload.put("tts", isTTS);
        payload.put("allowed_mentions", allowedMentions);
        if (components.size() > 0) {
            JSONArray components = new JSONArray();
            for (ActionRow row : this.components) {
                JSONObject component = new JSONObject(row.toData().toString());
                components.put(component);
            }
            payload.put("components", components);
        }
        String json = payload.toString();
        if (isFile()) {
            final MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

            for (int i = 0; i < attachments.length; i++) {
                final MessageAttachment attachment = attachments[i];
                if (attachment == null)
                    break;
                builder.addFormDataPart("file" + i, attachment.getName(), new IOUtil.OctetBody(attachment.getData()));
            }
            return builder.addFormDataPart("payload_json", json).build();
        }
        return RequestBody.create(IOUtil.JSON, json);
    }

}
