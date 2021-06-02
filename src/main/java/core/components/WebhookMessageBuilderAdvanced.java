package core.components;

import java.util.*;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;

public class WebhookMessageBuilderAdvanced extends WebhookMessageBuilder {

    private List<ActionRow> components;

    @Nonnull
    @CheckReturnValue
    public WebhookMessageBuilderAdvanced setActionRows(@Nonnull Collection<? extends ActionRow> rows) {
        Checks.noneNull(rows, "ActionRows");
        return setActionRows(rows.toArray(new ActionRow[0]));
    }

    @Nonnull
    @CheckReturnValue
    public WebhookMessageBuilderAdvanced setActionRows(@Nonnull ActionRow... rows) {
        Checks.noneNull(rows, "ActionRows");
        if (components == null)
            components = new ArrayList<>();
        Checks.check(rows.length <= 5, "Can only have 5 action rows per message!");
        this.components.clear();
        Collections.addAll(this.components, rows);
        return this;
    }

    @Override
    public @NotNull WebhookMessage build() {
        int fileIndex = getFileAmount();
        if (isEmpty())
            throw new IllegalStateException("Cannot build an empty message!");
        return new WebhookMessageAdvanced(username, avatarUrl, content.toString(), embeds, isTTS,
                fileIndex == 0 ? null : Arrays.copyOf(files, fileIndex), allowedMentions, components);
    }

}
