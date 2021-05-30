package core.buttons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import org.jetbrains.annotations.NotNull;

public class WebhookMessageBuilderAdvanced extends WebhookMessageBuilder {

    private final List<MessageRow> rows = new ArrayList<>();

    public WebhookMessageBuilderAdvanced appendButtons(MessageButton... buttons) {
        return appendButtons(List.of(buttons));
    }

    public WebhookMessageBuilderAdvanced appendButtons(Collection<MessageButton> buttons) {
        List<MessageButton> buttonList = new ArrayList<>(buttons);
        int rowIndex = 0;

        while(buttonList.size() > 0) {
            MessageRow row;
            if (rowIndex >= rows.size()) {
                row = new MessageRow();
                rows.add(row);
            } else {
                row = rows.get(rowIndex);
            }

            if (row.getButtons().size() < 5) {
                row.addButton(buttonList.remove(0));
            } else {
                rowIndex++;
            }
        }

        return this;
    }

    public WebhookMessageBuilderAdvanced addRow(MessageRow row) {
        this.rows.add(row);
        return this;
    }

    public WebhookMessageBuilderAdvanced removeRow(MessageRow row) {
        this.rows.remove(row);
        return this;
    }

    @Override
    public @NotNull WebhookMessage build() {
        int fileIndex = getFileAmount();
        if (isEmpty())
            throw new IllegalStateException("Cannot build an empty message!");
        return new WebhookMessageAdvanced(username, avatarUrl, content.toString(), embeds, isTTS,
                fileIndex == 0 ? null : Arrays.copyOf(files, fileIndex), allowedMentions, rows);
    }

}
