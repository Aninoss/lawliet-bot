package commands.runnables.interactionscategory;

import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;
import core.RandomPicker;
import core.TextManager;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.mention.Mention;
import core.utils.ComponentsUtil;
import core.utils.MentionUtil;
import modules.CustomRolePlay;
import mysql.hibernate.entity.CustomRolePlayEntity;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static commands.runnables.informationcategory.HelpCommand.RP_SUBCATEGORY_INTERACTIVE;

@CommandProperties(
        trigger = "customrp_sfw",
        emoji = "🧩",
        executableWithoutArgs = false,
        requiresFullMemberCache = true,
        subCategory = RP_SUBCATEGORY_INTERACTIVE
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
        Mention mention = MentionUtil.getMentionedString(getLocale(), event.getGuild(), args, event.getMember(), event.getRepliedMember());
        return onTriggerInteractive(event, args.substring(trigger.length()).trim(), mention);
    }

    @Override
    protected MessageComponentTree createComponents(Member member, Mention mention, boolean onlySelfReference, ActionRow actionRow) throws ExecutionException, InterruptedException {
        return super.createComponents(member, mention, false, actionRow);
    }

    @Override
    protected MessageComponentTree createComponentsSuccessfully(Member member, Mention mention, String authorString, String quote, ActionRow actionRow) {
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

        MediaGalleryItem image = MediaGalleryItem.fromUrl(gifUrl)
                .withSpoiler(getGuildEntity().getNsfwSpoilers() && getCommandProperties().nsfw());
        String title = customRolePlayEntity.getEmojiFormatted() + " " + customRolePlayEntity.getTitle();
        String desc = CustomRolePlay.resolveVariables(text, authorString, mention.getMentionText()) + quote;
        TextDisplay footer = TextDisplay.of("-# " + TextManager.getString(getLocale(), Category.INTERACTIONS, "customroleplay_footer").replace("{PREFIX}", getPrefix()));
        return ComponentsUtil.createCommandComponentTree(title, Arrays.asList(desc.isEmpty() ? null : TextDisplay.of(desc), MediaGallery.of(image), actionRow, footer), ComponentsUtil.DEFAULT_CONTAINER_COLOR);
    }

}
