package commands.runnables.externalcategory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import constants.ExternalLinks;
import constants.Settings;
import core.ExceptionLogger;
import core.TextManager;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.internet.HttpRequest;
import core.utils.*;
import modules.PostBundle;
import modules.pixiv.PixivDownloader;
import modules.pixiv.PixivImage;
import modules.schedulers.AlertResponse;
import mysql.modules.nsfwfilter.DBNSFWFilters;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "pixiv",
        emoji = "🅿️",
        patreonRequired = true,
        executableWithoutArgs = false
)
public class PixivCommand extends Command implements OnAlertListener {

    private static final PixivDownloader pixivDownloader = new PixivDownloader();

    public PixivCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException, JsonProcessingException {
        if (args.isEmpty()) {
            drawMessageNew(ComponentsUtil.createErrorNoArgs(this))
                    .exceptionally(ExceptionLogger.get());
            return false;
        } else {
            HashSet<String> filterSet = getFilterSet(event.getGuild().getIdLong());
            if (NSFWUtil.containsFilterTags(NSFWUtil.expandTags(args), filterSet)) {
                MessageComponentTree components = ComponentsUtil.createCommandComponentTreeError(this, TextDisplay.of(TextManager.getString(getLocale(), Category.NSFW, "porn_illegal_tag_desc")));
                drawMessageNew(components)
                        .exceptionally(ExceptionLogger.get());
                return false;
            }

            FeatureLogger.inc(PremiumFeature.PIXIV, event.getGuild().getIdLong());
            deferReply();
            try {
                return pixivDownloader.retrieveImage(event.getGuild().getIdLong(), args, JDAUtil.channelIsNsfw(event.getChannel()), filterSet).get()
                        .map(image -> {
                            if (image.isNsfw() && !JDAUtil.channelIsNsfw(event.getChannel())) {
                                drawMessageNew(ComponentsUtil.createErrorNsfwBlock(this))
                                        .exceptionally(ExceptionLogger.get());
                                return false;
                            }

                            Container container = toContainer(image);
                            MessageComponentTree components = MessageComponentTree.of(container);
                            components = ComponentsUtil.addTrackerNoteLog(this, event.getMember(), components);
                            drawMessageNew(components).exceptionally(ExceptionLogger.get());
                            return true;
                        }).orElseGet(() -> {
                            drawMessageNew(ComponentsUtil.createErrorNoResults(this, args))
                                    .exceptionally(ExceptionLogger.get());
                            return false;
                        });
            } catch (ExecutionException e) {
                drawMessageNew(ComponentsUtil.createErrorApiDown(this, "pixiv.net"))
                        .exceptionally(ExceptionLogger.get());
                return false;
            }
        }
    }

    @Override
    public @NotNull AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        String key = slot.getCommandKey();
        if (key.isEmpty()) {
            MessageComponentTree components = ComponentsUtil.createErrorNoArgs(this);
            components = ComponentsUtil.addTrackerRemoveLog(getLocale(), components);
            slot.sendMessageComponentTree(getLocale(), false, components);
            return AlertResponse.STOP_AND_DELETE;
        } else {
            HashSet<String> filterSet = getFilterSet(slot.getGuildId());
            if (NSFWUtil.containsFilterTags(NSFWUtil.expandTags(key), filterSet)) {
                MessageComponentTree components = ComponentsUtil.createCommandComponentTreeError(this, TextDisplay.of(TextManager.getString(getLocale(), Category.NSFW, "porn_illegal_tag_desc")));
                components = ComponentsUtil.addTrackerRemoveLog(getLocale(), components);
                slot.sendMessageComponentTree(getLocale(), false, components);
                return AlertResponse.STOP_AND_DELETE;
            }

            slot.setNextRequest(Instant.now().plus(15, ChronoUnit.MINUTES));
            FeatureLogger.inc(PremiumFeature.PIXIV, slot.getGuildId());
            Optional<PostBundle<PixivImage>> postBundleOpt;
            try {
                postBundleOpt = pixivDownloader.retrieveImagesBulk(key, slot.getArgs().orElse(null), filterSet).get();
            } catch (ExecutionException e) {
                slot.setNextRequest(Instant.now().plus(15, ChronoUnit.MINUTES));
                return AlertResponse.CONTINUE;
            }

            GuildMessageChannel channel = slot.getGuildMessageChannel().get();
            boolean containsOnlyNsfw = true;

            if (postBundleOpt.isPresent()) {
                PostBundle<PixivImage> postBundle = postBundleOpt.get();
                ArrayList<Container> containers = new ArrayList<>();

                for (int i = 0; i < Math.min(5, postBundle.getPosts().size()); i++) {
                    PixivImage image = postBundle.getPosts().get(i);
                    if (!image.isNsfw() || JDAUtil.channelIsNsfw(channel)) {
                        Container container = toContainer(image);
                        containers.add(0, container);

                        containsOnlyNsfw = false;
                        if (slot.getArgs().isEmpty()) {
                            break;
                        }
                    }
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

                slot.setArgs(postBundle.getNewestPost());
                return AlertResponse.CONTINUE_AND_SAVE;
            } else {
                if (slot.getArgs().isEmpty()) {
                    MessageComponentTree components = ComponentsUtil.createErrorNoResults(this, key);
                    components = ComponentsUtil.addTrackerRemoveLog(getLocale(), components);
                    slot.sendMessageComponentTree(getLocale(), false, components);
                    return AlertResponse.STOP_AND_DELETE;
                } else {
                    return AlertResponse.CONTINUE;
                }
            }
        }
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

    private Container toContainer(PixivImage image) {
        ArrayList<ContainerChildComponent> components = new ArrayList<>();
        String desc = StringUtil.shortenString(StringUtil.unescapeHtml(image.getDescription()), 2048);
        String nsfwString = image.isNsfw() ? getString("nsfw") : "";
        boolean spoiler = image.isNsfw() && getGuildEntity().getNsfwSpoilers();

        String author = "-# " + StringUtil.maskedLink(StringUtil.sanitizeMarkdown(image.getAuthor()), image.getAuthorUrl());
        String title = "**" + StringUtil.maskedLink(StringUtil.sanitizeMarkdown(StringUtil.shortenString(image.getTitle(), 256)), image.getUrl()) + "**";
        components.add(TextDisplay.of(author + "\n" + title));
        if (!desc.isEmpty()) {
            components.add(TextDisplay.of(desc));
        }

        List<String> images = downloadAndProxyImages(image);
        for (List<String> imagesPartitioned : Lists.partition(images, 10)) {
            List<MediaGalleryItem> mediaGalleryItems = imagesPartitioned.stream()
                    .map(url -> MediaGalleryItem.fromUrl(url).withSpoiler(spoiler))
                    .collect(Collectors.toList());
            MediaGallery mediaGallery = MediaGallery.of(mediaGalleryItems);
            components.add(mediaGallery);
        }

        String footer = "-# " + getString("footer",
                StringUtil.numToString(image.getBookmarks()),
                StringUtil.numToString(image.getViews()),
                TimeFormat.DATE_TIME_SHORT.format(image.getInstant())
        ) + nsfwString;
        components.add(TextDisplay.of(footer));
        components.add(ActionRow.of(generateReportButton(images.get(0))));
        return Container.of(components)
                .withAccentColor(ComponentsUtil.DEFAULT_CONTAINER_COLOR);
    }

    private List<String> downloadAndProxyImages(PixivImage image) {
        ArrayList<String> proxyImageUrls = new ArrayList<>();
        for (int i = 0; i < Math.min(image.getImageUrls().size(), 30); i++) {
            String proxyImageUrl = downloadAndProxyImage(image.getId(), image.getImageUrls().get(i), i);
            proxyImageUrls.add(proxyImageUrl);
        }
        return proxyImageUrls;
    }

    private String downloadAndProxyImage(String id, String imageUrl, int imageIndex) {
        if (imageUrl.startsWith("https://api.pixiv.moe")) {
            return imageUrl;
        }

        String processUrl = "https://media-cdn.lawlietbot.xyz/pixiv_download/" + URLEncoder.encode(imageUrl, StandardCharsets.UTF_8) + "/" + id + "_" + imageIndex + "/" + URLEncoder.encode(System.getenv("MS_PROXY_AUTH"), StandardCharsets.UTF_8);
        String imageProxyUrl = "https://media-cdn.lawlietbot.xyz/pixiv/" + id + "_" + imageIndex + FileUtil.getUriExt(imageUrl);
        int code = HttpRequest.get(processUrl).join().getCode();
        if (HttpRequest.get(processUrl).join().getCode() != 200) {
            throw new RuntimeException(new IOException("Pixiv downloader returned response code " + code));
        }

        return imageProxyUrl;
    }

    private Button generateReportButton(String imageUrl) {
        String encodedArgs = Base64.getEncoder().encodeToString(imageUrl.replace("https://", "").getBytes());
        String url = ExternalLinks.REPORT_URL + URLEncoder.encode(encodedArgs, StandardCharsets.UTF_8);
        return Button.of(ButtonStyle.LINK, url, TextManager.getString(getLocale(), Category.NSFW, "porn_report"));
    }

    private HashSet<String> getFilterSet(long guildId) {
        List<String> nsfwFiltersList = DBNSFWFilters.getInstance().retrieve(guildId).getKeywords();
        HashSet<String> filters = new HashSet<>(Set.of(Settings.NSFW_FILTERS));
        nsfwFiltersList.forEach(filter -> filters.add(filter.toLowerCase()));
        return filters;
    }

}
