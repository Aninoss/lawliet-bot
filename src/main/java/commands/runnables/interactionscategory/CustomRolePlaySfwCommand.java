package commands.runnables.interactionscategory;

import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;
import core.EmbedFactory;
import core.RandomPicker;
import core.TextManager;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.mention.Mention;
import core.utils.EmbedUtil;
import modules.CustomRolePlay;
import mysql.hibernate.entity.CustomRolePlayEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "customrp_sfw",
        emoji = "🧩",
        executableWithoutArgs = false,
        requiresFullMemberCache = true
)
public class CustomRolePlaySfwCommand extends RolePlayAbstract {

    private String trigger;
    private CustomRolePlayEntity customRolePlayEntity;

    public CustomRolePlaySfwCommand(Locale locale, String prefix) {
        super(locale, prefix, true);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        trigger = args.split(" ")[0];
        customRolePlayEntity = getGuildEntity().getCustomRolePlayCommands().get(trigger);
        return onTriggerInteractive(event, args.substring(trigger.length()).trim());
    }

    @Override
    protected EmbedBuilder generateEmbed(long guildId, boolean mentionPresent, Mention mention, String authorString, String quote) throws ExecutionException, InterruptedException {
        FeatureLogger.inc(PremiumFeature.CUSTOM_ROLE_PLAY, guildId);
        String gifUrl = customRolePlayEntity.getImageAttachmentUrls().get(RandomPicker.pick(trigger, guildId, customRolePlayEntity.getImageAttachments().size()).get());

        String text;
        if (mentionPresent) {
            if (mention.isMultiple()) {
                text = Objects.requireNonNullElse(customRolePlayEntity.getTextMultiMembers(), "");
            } else {
                text = Objects.requireNonNullElse(customRolePlayEntity.getTextSingleMember(), "");
            }
        } else {
            text = Objects.requireNonNullElse(customRolePlayEntity.getTextNoMembers(), "");
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(customRolePlayEntity.getEmojiFormatted() + " " + customRolePlayEntity.getTitle())
                .setDescription(CustomRolePlay.resolveVariables(text, authorString, mention.getMentionText()) + quote)
                .setImage(gifUrl);
        EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.INTERACTIONS, "customroleplay_footer").replace("{PREFIX}", getPrefix()));
        return eb;
    }

}
