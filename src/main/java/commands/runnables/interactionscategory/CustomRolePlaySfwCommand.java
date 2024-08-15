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
import net.dv8tion.jda.api.entities.Member;
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

    private CustomRolePlayEntity customRolePlayEntity;
    private String gifUrl;

    public CustomRolePlaySfwCommand(Locale locale, String prefix) {
        super(locale, prefix, true);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        String trigger = args.split(" ")[0];
        customRolePlayEntity = getGuildEntity().getCustomRolePlayCommands().get(trigger);
        gifUrl = customRolePlayEntity.getImageUrls().get(RandomPicker.pick(trigger, event.getGuild().getIdLong(), customRolePlayEntity.getImageFilenames().size()).get());
        return onTriggerInteractive(event, args.substring(trigger.length()).trim());
    }

    @Override
    protected EmbedBuilder generateEmbed(Member member, Mention mention, boolean onlySelfReference) throws ExecutionException, InterruptedException {
        return super.generateEmbed(member, mention, false);
    }

    @Override
    protected EmbedBuilder generateEmbedSunshineCase(Member member, Mention mention, String authorString, String quote) throws ExecutionException, InterruptedException {
        FeatureLogger.inc(PremiumFeature.CUSTOM_ROLE_PLAY, member.getGuild().getIdLong());

        String text;
        if (mention.getElementList().isEmpty()) {
            text = Objects.requireNonNullElse(customRolePlayEntity.getTextNoMembers(), "");
        } else {
            if (mention.isMultiple()) {
                text = Objects.requireNonNullElse(customRolePlayEntity.getTextMultiMembers(), "");
            } else {
                text = Objects.requireNonNullElse(customRolePlayEntity.getTextSingleMember(), "");
            }
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(customRolePlayEntity.getEmojiFormatted() + " " + customRolePlayEntity.getTitle())
                .setDescription(CustomRolePlay.resolveVariables(text, authorString, mention.getMentionText()) + quote)
                .setImage(gifUrl);
        EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.INTERACTIONS, "customroleplay_footer").replace("{PREFIX}", getPrefix()));
        return eb;
    }

}
