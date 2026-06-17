package commands.runnables.externalcategory;

import com.fasterxml.jackson.core.JsonProcessingException;
import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.ComponentsUtil;
import core.utils.InternetUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import modules.reddit.RedditDownloader;
import modules.reddit.RedditPost;
import modules.schedulers.AlertResponse;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "reddit",
        emoji = "\uD83E\uDD16",
        executableWithoutArgs = false
)
public class RedditCommand extends Command implements OnAlertListener {

    private static final RedditDownloader redditDownloader = new RedditDownloader();

    private final String forceSubreddit;

    public RedditCommand(Locale locale, String prefix) {
        this(locale, prefix, null);
    }

    public RedditCommand(Locale locale, String prefix, String forceSubreddit) {
        super(locale, prefix);
        this.forceSubreddit = forceSubreddit;
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException, JsonProcessingException {
        if (forceSubreddit != null) {
            args = forceSubreddit;
        }

        if (args.isEmpty() || args.equalsIgnoreCase("r/")) {
            drawMessageNew(ComponentsUtil.createErrorNoArgs(this))
                    .exceptionally(ExceptionLogger.get());
            return false;
        } else {
            String finalArgs = args;
            deferReply();
            try {
                return redditDownloader.retrievePost(event.getGuild().getIdLong(), args, JDAUtil.channelIsNsfw(event.getChannel())).get()
                        .map(post -> {
                            if (post.isNsfw() && !JDAUtil.channelIsNsfw(event.getChannel())) {
                                drawMessageNew(ComponentsUtil.createErrorNsfwBlock(this))
                                        .exceptionally(ExceptionLogger.get());
                                return false;
                            }

                            MessageComponentTree components = MessageComponentTree.of(toContainer(post));
                            components = ComponentsUtil.addTrackerNoteLog(this, event.getMember(), components);
                            drawMessageNew(components).exceptionally(ExceptionLogger.get());
                            return true;
                        }).orElseGet(() -> {
                            drawMessageNew(ComponentsUtil.createErrorNoResults(this, finalArgs))
                                    .exceptionally(ExceptionLogger.get());
                            return false;
                        });
            } catch (ExecutionException e) {
                drawMessageNew(ComponentsUtil.createErrorApiDown(this, "reddit.com"))
                        .exceptionally(ExceptionLogger.get());
                return false;
            }
        }
    }

    private Container toContainer(RedditPost post) {
        ArrayList<ContainerChildComponent> components = new ArrayList<>();
        boolean spoiler = post.isNsfw() && getGuildEntity().getNsfwSpoilers();

        String author = "-# " + StringUtil.maskedLink(StringUtil.sanitizeMarkdown(post.getAuthor()), "https://www.reddit.com/user/" + post.getAuthor());
        String title;
        if (InternetUtil.stringIsURL(post.getRedditUrl())) {
            title = "**" + StringUtil.maskedLink(StringUtil.sanitizeMarkdown(StringUtil.shortenString(post.getTitle(), 256)), post.getRedditUrl()) + "**";
        } else {
            title = "**" + StringUtil.escapeMarkdown(StringUtil.shortenString(post.getTitle(), 256)) + "**";
        }

        ArrayList<TextDisplay> titleComponents = new ArrayList<>();
        titleComponents.add(TextDisplay.of(author + "\n" + title));

        if (post.getDescription() != null && !post.getDescription().isEmpty()) {
            String desc = StringUtil.shortenString(StringUtil.escapeMarkdown(post.getDescription()), 2048);
            titleComponents.add(TextDisplay.of(desc));
        }

        if ((post.getMediaUrls() == null || post.getMediaUrls().isEmpty()) && InternetUtil.stringIsURL(post.getThumbnail())) {
            Section section = Section.of(Thumbnail.fromUrl(post.getThumbnail()).withSpoiler(spoiler), titleComponents);
            components.add(section);
        } else {
            components.addAll(titleComponents);
        }

        if (post.getContentUrl() != null) {
            String label = TextManager.getString(getLocale(), Category.EXTERNAL, "reddit_linktext");
            Button button = Button.of(ButtonStyle.LINK, post.getContentUrl(), label);
            components.add(ActionRow.of(button));
        }

        if (post.getMediaUrls() != null) {
            List<MediaGalleryItem> mediaGalleryItems = post.getMediaUrls().stream()
                    .filter(InternetUtil::stringIsURL)
                    .map(imageUrl -> {
                        return MediaGalleryItem.fromUrl(imageUrl)
                                .withSpoiler(spoiler);
                    })
                    .limit(MediaGallery.MAX_ITEMS)
                    .collect(Collectors.toList());
            if (!mediaGalleryItems.isEmpty()) {
                MediaGallery mediaGallery = MediaGallery.of(mediaGalleryItems);
                components.add(mediaGallery);
            }
        }

        String flairText = "";
        String flair = StringUtil.escapeMarkdown(post.getFlair());
        if (flair != null && !flair.equals("null") && !flair.isBlank()) {
            flairText = flair + "｜";
        }

        String nsfwString = "";
        if (post.isNsfw()) {
            nsfwString = " " + TextManager.getString(getLocale(), Category.EXTERNAL, "reddit_nsfw");
        }

        String footer = "-# " + TextManager.getString(getLocale(), Category.EXTERNAL, "reddit_footer",
                flairText,
                StringUtil.numToString(post.getScore()),
                StringUtil.numToString(post.getComments()),
                TimeFormat.DATE_TIME_SHORT.format(post.getInstant())
        ) + nsfwString;
        components.add(TextDisplay.of(footer));
        return Container.of(components)
                .withAccentColor(ComponentsUtil.DEFAULT_CONTAINER_COLOR);
    }

    @Override
    public @NotNull AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        String key = forceSubreddit != null ? forceSubreddit : slot.getCommandKey();
        Instant nextRequestPreviously = slot.getNextRequest();

        if (key.isEmpty()) {
            MessageComponentTree components = ComponentsUtil.createErrorNoArgs(this);
            components = ComponentsUtil.addTrackerRemoveLog(getLocale(), components);
            slot.sendMessageComponentTree(getLocale(), false, components);
            return AlertResponse.STOP_AND_DELETE;
        }

        List<RedditPost> redditPosts;
        try {
            redditPosts = redditDownloader.retrievePostsBulk(key).get();
        } catch (ExecutionException e) {
            slot.setNextRequest(Instant.now().plus(15, ChronoUnit.MINUTES));
            return AlertResponse.CONTINUE;
        }

        slot.setNextRequest(Instant.now().plus(120, ChronoUnit.MINUTES));
        if (redditPosts.isEmpty()) {
            if (slot.getArgs().isEmpty() || nextRequestPreviously.isBefore(Instant.now().minus(Duration.ofDays(30)))) {
                MessageComponentTree components = ComponentsUtil.createErrorNoResults(this, key);
                components = ComponentsUtil.addTrackerRemoveLog(getLocale(), components);
                slot.sendMessageComponentTree(getLocale(), false, components);
                return AlertResponse.STOP_AND_DELETE;
            } else {
                return AlertResponse.CONTINUE_AND_SAVE;
            }
        }

        ArrayList<String> idList = slot.getArgs().map(args -> new ArrayList<>(List.of(args.split("\\|"))))
                .orElse(new ArrayList<>());
        ArrayList<String> newIdList = new ArrayList<>();
        boolean containsOnlyNsfw = true;
        ArrayList<Container> containers = new ArrayList<>();

        if (slot.getArgs().isEmpty()) {
            for (int i = 1; i < Math.min(50, redditPosts.size()); i++) {
                idList.add(0, redditPosts.get(i).getId());
            }
        }
        for (int i = 0; i < Math.min(50, redditPosts.size()) && containers.size() < 5; i++) {
            RedditPost post = redditPosts.get(i);
            if (post.isNsfw() && !JDAUtil.channelIsNsfw(slot.getGuildMessageChannel().get())) {
                continue;
            }
            containsOnlyNsfw = false;
            if (!idList.contains(post.getId())) {
                Container container = toContainer(post);
                containers.add(0, container);
                newIdList.add(0, post.getId());
            }
        }

        idList.addAll(newIdList);
        while (idList.size() > 250) {
            idList.remove(0);
        }

        if (containsOnlyNsfw && slot.getArgs().isEmpty()) {
            MessageComponentTree components = ComponentsUtil.createErrorNsfwBlock(this);
            components = ComponentsUtil.addTrackerRemoveLog(getLocale(), components);
            slot.sendMessageComponentTree(getLocale(), false, components);
            return AlertResponse.STOP_AND_DELETE;
        }

        if (!containers.isEmpty()) {
            slot.sendMessageContainers(getLocale(), true, containers);
        }

        slot.setArgs(Strings.join(idList, '|'));
        return AlertResponse.CONTINUE_AND_SAVE;
    }

    @Override
    public boolean trackerUsesKey() {
        return forceSubreddit == null;
    }

}
