package commands.runnables.externalcategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import commands.listeners.OnButtonListener;
import constants.Emojis;
import constants.LogStatus;
import core.ExceptionLogger;
import core.utils.ComponentsUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import modules.anilist.AnilistDownloader;
import modules.anilist.AnilistMedia;
import modules.schedulers.AlertResponse;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "anilist",
        emoji = "⛩️",
        executableWithoutArgs = false,
        releaseDate = { 2024, 11, 25 },
        aliases = { "anime" }
)
public class AnilistCommand extends Command implements OnAlertListener, OnButtonListener {

    private AnilistMedia media;
    private boolean showRecommendations = false;

    public AnilistCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        deferReply();
        media = AnilistDownloader.getMediaBySearch(args, JDAUtil.channelIsNsfw(event.getChannel()));
        if (media != null) {
            registerButtonListener(event.getMember());
            return true;
        } else {
            drawMessageNew(ComponentsUtil.createErrorNoResults(this, args))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        showRecommendations = !showRecommendations;
        return true;
    }

    @Override
    @Nullable
    public MessageComponentTree draw(@Nullable Member member) throws Throwable {
        MessageComponentTree components = MessageComponentTree.of(toContainer(media, member));
        components = ComponentsUtil.addTrackerNoteLog(this, member, components);
        return components;
    }

    private Container toContainer(AnilistMedia media, Member member) {
        StringBuilder episodesStringBuilder = new StringBuilder();
        if (media.getCurrentEpisode() != null) {
            episodesStringBuilder.append(media.getCurrentEpisode())
                    .append("/");
        }
        if (media.getTotalEpisodes() != null) {
            episodesStringBuilder.append(media.getTotalEpisodes());
        } else {
            episodesStringBuilder.append("?");
        }
        if (media.getNextEpisode() != null) {
            episodesStringBuilder.append("\n")
                    .append(getString("nextepisode", TimeFormat.DATE_TIME_SHORT.atInstant(media.getNextEpisode()).toString()));
        }

        ArrayList<ContainerChildComponent> components = new ArrayList<>();

        String title = "**" + StringUtil.maskedLink(StringUtil.sanitizeMarkdown(StringUtil.shortenString(media.getTitle(), 250)), media.getAnilistUrl()) + "**";
        String desc = StringUtil.shortenString(StringUtil.escapeMarkdown(media.getDescription()), 500);
        components.add(Section.of(
                Thumbnail.fromUrl(media.getCoverImage()),
                TextDisplay.of(title),
                TextDisplay.of(desc)
        ));
        components.add(Separator.createInvisible(Separator.Spacing.SMALL));

        components.add(TextDisplay.of(getString("genres") + "\n> " + String.join(", ", media.getGenres())));
        components.add(TextDisplay.of(getString("status") + "\n> " + getString("status_" + media.getStatus().name())));
        components.add(TextDisplay.of(getString("episodes") + "\n> " + episodesStringBuilder));

        if (media.getAverageScore() != null) {
            components.add(TextDisplay.of(getString("score") + "\n> " + media.getAverageScore() + "%"));
        }

        if (member != null && !media.getRecommendations().isEmpty()) {
            components.add(Separator.createDivider(Separator.Spacing.LARGE));

            if (showRecommendations) {
                components.add(TextDisplay.of("-# " + getString("recommendations")));

                for (int i = 0; i < media.getRecommendations().size(); i++) {
                    AnilistMedia recommendationMedia =  media.getRecommendations().get(i);
                    String recommendationTitle = "**" + StringUtil.maskedLink(StringUtil.sanitizeMarkdown(StringUtil.shortenString(recommendationMedia.getTitle(), 250)), recommendationMedia.getAnilistUrl()) + "**";
                    String recommendationBody = ">>> **" + getString("genres") + "**: " + String.join(", ", recommendationMedia.getGenres());
                    if (recommendationMedia.getAverageScore() != null) {
                        recommendationBody += "\n**" + getString("score") + "**: " + recommendationMedia.getAverageScore() + "%";
                    }

                    components.add(Section.of(
                            Thumbnail.fromUrl(recommendationMedia.getCoverImage()),
                            TextDisplay.of(recommendationTitle + "\n" + recommendationBody)
                    ));
                }
            }

            Button recommendationButton = Button.of(ButtonStyle.PRIMARY, "recommendations", getString("recommendations"))
                    .withEmoji(showRecommendations ? Emojis.MENU_SHORT_ARROW_UP_GRAY : Emojis.MENU_SHORT_ARROW_DOWN_GRAY);
            components.add(ActionRow.of(recommendationButton));
        }

        return Container.of(components)
                .withAccentColor(ComponentsUtil.DEFAULT_CONTAINER_COLOR);
    }

    @NotNull
    @Override
    public AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        slot.setNextRequest(Instant.now().plus(1, ChronoUnit.HOURS));
        AnilistMedia media;
        Integer previousHash = null;

        if (slot.getArgs().isEmpty()) {
            media = AnilistDownloader.getMediaBySearch(slot.getCommandKey(), JDAUtil.channelIsNsfw(slot.getGuildMessageChannel().get()));
        } else {
            String[] argsSplit = slot.getArgs().get().split("_");
            previousHash = Integer.parseInt(argsSplit[1]);
            media = AnilistDownloader.getMediaById(Integer.parseInt(argsSplit[0]), JDAUtil.channelIsNsfw(slot.getGuildMessageChannel().get()));
        }

        if (media == null) {
            if (slot.getArgs().isEmpty()) {
                MessageComponentTree components = ComponentsUtil.createErrorNoResults(this, slot.getCommandKey());
                components = ComponentsUtil.addTrackerRemoveLog(getLocale(), components);
                slot.sendMessageComponentTree(getLocale(), false, components);
                return AlertResponse.STOP_AND_DELETE;
            } else {
                return AlertResponse.CONTINUE_AND_SAVE;
            }
        }

        MessageComponentTree components = MessageComponentTree.of(toContainer(media, null));
        if (media.getStatus() == AnilistMedia.Status.NOT_YET_RELEASED || media.getStatus() == AnilistMedia.Status.RELEASING) {
            if (previousHash == null || media.hashCode() != previousHash) {
                slot.sendMessageComponentTree(getLocale(), true, components);
            }
            slot.setArgs(media.getId() + "_" + media.hashCode());
            return AlertResponse.CONTINUE_AND_SAVE;
        } else {
            components = ComponentsUtil.addLog(components, LogStatus.WARNING, getString("alertcompleted"));
            slot.sendMessageComponentTree(getLocale(), true, components);
            return AlertResponse.STOP_AND_DELETE;
        }
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

}
