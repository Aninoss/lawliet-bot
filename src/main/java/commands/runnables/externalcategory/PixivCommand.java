package commands.runnables.externalcategory;

import com.fasterxml.jackson.core.JsonProcessingException;
import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import commands.listeners.OnButtonListener;
import constants.ExternalLinks;
import constants.Settings;
import core.EmbedFactory;
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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "pixiv",
        emoji = "🅿️",
        patreonRequired = true,
        executableWithoutArgs = false
)
public class PixivCommand extends Command implements OnButtonListener, OnAlertListener {

    private static final String BUTTON_ID_MORE = "more";
    private static final String BUTTON_ID_PREVIOUS = "previous";
    private static final String BUTTON_ID_NEXT = "next";

    private static final PixivDownloader pixivDownloader = new PixivDownloader();

    private String args = null;
    private PixivImage pixivImage;
    private int imageIndex;
    private HashSet<String> processedImageProxyUrls = new HashSet<>();

    public PixivCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException, JsonProcessingException {
        if (args.isEmpty()) {
            drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        } else {
            HashSet<String> filterSet = getFilterSet(event.getGuild().getIdLong());
            if (NSFWUtil.containsFilterTags(NSFWUtil.expandTags(args), filterSet)) {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this)
                        .setTitle(TextManager.getString(getLocale(), Category.NSFW, "porn_illegal_tag"))
                        .setDescription(TextManager.getString(getLocale(), Category.NSFW, "porn_illegal_tag_desc"));
                drawMessageNew(eb)
                        .exceptionally(ExceptionLogger.get());
                return false;
            }

            this.args = args;
            FeatureLogger.inc(PremiumFeature.PIXIV, event.getGuild().getIdLong());
            deferReply();
            try {
                return pixivDownloader.retrieveImage(event.getGuild().getIdLong(), args, JDAUtil.channelIsNsfw(event.getChannel()), filterSet).get()
                        .map(image -> {
                            if (image.isNsfw() && !JDAUtil.channelIsNsfw(event.getChannel())) {
                                drawMessageNew(EmbedFactory.getNSFWBlockEmbed(this)).exceptionally(ExceptionLogger.get());
                                return false;
                            }

                            this.pixivImage = image;
                            this.imageIndex = 0;
                            this.processedImageProxyUrls = new HashSet<>();

                            registerButtonListener(event.getMember());
                            return true;
                        }).orElseGet(() -> {
                            EmbedBuilder eb = EmbedFactory.getNoResultsEmbed(this, args);
                            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
                            return false;
                        });
            } catch (ExecutionException e) {
                EmbedBuilder eb = EmbedFactory.getApiDownEmbed(this, "pixiv.net");
                drawMessageNew(eb).exceptionally(ExceptionLogger.get());
                return false;
            }
        }
    }

    @Override
    public @NotNull AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        String key = slot.getCommandKey();
        if (key.isEmpty()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args"));
            EmbedUtil.addTrackerRemoveLog(eb, getLocale());
            slot.sendMessage(getLocale(), false, eb.build());
            return AlertResponse.STOP_AND_DELETE;
        } else {
            HashSet<String> filterSet = getFilterSet(slot.getGuildId());
            if (NSFWUtil.containsFilterTags(NSFWUtil.expandTags(key), filterSet)) {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this)
                        .setTitle(TextManager.getString(getLocale(), Category.NSFW, "porn_illegal_tag"))
                        .setDescription(TextManager.getString(getLocale(), Category.NSFW, "porn_illegal_tag_desc"));
                EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                slot.sendMessage(getLocale(), false, eb.build());
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
                int totalEmbedSize = 0;
                ArrayList<MessageEmbed> embedList = new ArrayList<>();
                ArrayList<String> proxyImageUrlList = new ArrayList<>();

                for (int i = 0; i < Math.min(5, postBundle.getPosts().size()); i++) {
                    PixivImage image = postBundle.getPosts().get(i);
                    if (!image.isNsfw() || JDAUtil.channelIsNsfw(channel)) {
                        String proxyImageUrl = downloadAndProxyImage(image.getId(), image.getImageUrls().get(0), 0);
                        MessageEmbed messageEmbed = getEmbed(image, proxyImageUrl, true).build();
                        embedList.add(0, messageEmbed);
                        proxyImageUrlList.add(0, proxyImageUrl);

                        totalEmbedSize += messageEmbed.getLength();
                        containsOnlyNsfw = false;
                        if (slot.getArgs().isEmpty()) {
                            break;
                        }
                    }
                }

                if (containsOnlyNsfw && slot.getArgs().isEmpty()) {
                    EmbedBuilder eb = EmbedFactory.getNSFWBlockEmbed(getLocale(), getPrefix());
                    EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                    slot.sendMessage(getLocale(), false, eb.build());
                    return AlertResponse.STOP_AND_DELETE;
                }

                if (!embedList.isEmpty()) {
                    while (totalEmbedSize > MessageEmbed.EMBED_MAX_LENGTH_BOT) {
                        totalEmbedSize -= embedList.remove(0).getLength();
                        proxyImageUrlList.remove(0);
                    }
                    ActionRow actionRow = ActionRow.of(generateReportButton(proxyImageUrlList));
                    slot.sendMessage(getLocale(), true, embedList, actionRow);
                }

                slot.setArgs(postBundle.getNewestPost());
                return AlertResponse.CONTINUE_AND_SAVE;
            } else {
                if (slot.getArgs().isEmpty()) {
                    EmbedBuilder eb = EmbedFactory.getNoResultsEmbed(this, key);
                    EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                    slot.sendMessage(getLocale(), false, eb.build());
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

    @Nullable
    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        String imageUrl = pixivImage.getImageUrls().get(imageIndex);

        String proxyImageUrl = downloadAndProxyImage(pixivImage.getId(), imageUrl, imageIndex);
        EmbedBuilder eb = getEmbed(pixivImage, proxyImageUrl, false);
        EmbedUtil.addTrackerNoteLog(getLocale(), member, eb, getPrefix(), getTrigger());

        List<Button> primaryButtonList = new ArrayList<>();
        primaryButtonList.add(Button.of(ButtonStyle.PRIMARY, BUTTON_ID_MORE, TextManager.getString(getLocale(), Category.NSFW, "porn_morebutton")));

        if (pixivImage.getImageUrls().size() > 1) {
            primaryButtonList.add(Button.of(ButtonStyle.SECONDARY, BUTTON_ID_PREVIOUS, TextManager.getString(getLocale(), TextManager.GENERAL, "list_previous")));
            primaryButtonList.add(Button.of(ButtonStyle.SECONDARY, BUTTON_ID_NEXT, TextManager.getString(getLocale(), TextManager.GENERAL, "list_next")));
        }

        setActionRows(
                ActionRow.of(primaryButtonList),
                ActionRow.of(generateReportButton(Collections.singletonList(proxyImageUrl)))
        );
        return eb;
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        switch (event.getComponentId()) {
            case BUTTON_ID_MORE -> {
                event.deferEdit().queue();
                deregisterListeners();
                setDrawMessage(null);
                onTrigger(getCommandEvent(), this.args);
                return false;
            }
            case BUTTON_ID_PREVIOUS -> {
                imageIndex--;
                if (imageIndex < 0) {
                    imageIndex = pixivImage.getImageUrls().size() - 1;
                }
                return true;
            }
            case BUTTON_ID_NEXT -> {
                imageIndex++;
                if (imageIndex >= pixivImage.getImageUrls().size()) {
                    imageIndex = 0;
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private EmbedBuilder getEmbed(PixivImage image, String proxyImageUrl, boolean alert) {
        String desc = StringUtil.shortenString(StringUtil.unescapeHtml(image.getDescription()), 2048);
        String imageNumberTitleString = "";
        String imageIndexFooterString = "";
        String nsfwString = "";

        if (image.getImageUrls().size() > 1) {
            if (alert) {
                imageNumberTitleString = "【" + image.getImageUrls().size() + "】";
            } else {
                imageIndexFooterString = getString("metaindex", StringUtil.numToString(imageIndex + 1), StringUtil.numToString(image.getImageUrls().size()));
            }
        }
        if (image.isNsfw()) {
            nsfwString = getString("nsfw");
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, desc)
                .setTitle(StringUtil.shortenString(image.getTitle() + imageNumberTitleString, 256), image.getUrl())
                .setAuthor(image.getAuthor(), image.getAuthorUrl(), null)
                .setImage(proxyImageUrl)
                .setTimestamp(image.getInstant());
        EmbedUtil.setFooter(eb, this,
                getString("footer", StringUtil.numToString(image.getBookmarks()), StringUtil.numToString(image.getViews())) + nsfwString + imageIndexFooterString
        );
        return eb;
    }

    private String downloadAndProxyImage(String id, String imageUrl, int imageIndex) {
        if (imageUrl.startsWith("https://api.pixiv.moe")) {
            return imageUrl;
        }

        String processUrl = "https://media-cdn.lawlietbot.xyz/pixiv_download/" + URLEncoder.encode(imageUrl, StandardCharsets.UTF_8) + "/" + id + "_" + imageIndex + "/" + URLEncoder.encode(System.getenv("MS_PROXY_AUTH"), StandardCharsets.UTF_8);
        String imageProxyUrl = "https://media-cdn.lawlietbot.xyz/pixiv/" + id + "_" + imageIndex + FileUtil.getUriExt(imageUrl);
        if (!processedImageProxyUrls.contains(imageProxyUrl)) {
            int code = HttpRequest.get(processUrl).join().getCode();
            if (HttpRequest.get(processUrl).join().getCode() != 200) {
                throw new RuntimeException(new IOException("Pixiv downloader returned response code " + code));
            }
        }
        processedImageProxyUrls.add(imageProxyUrl);

        return imageProxyUrl;
    }

    private Button generateReportButton(List<String> imageUrls) {
        StringBuilder reportArgsBuilder = new StringBuilder();
        for (String imageUrl : imageUrls) {
            if (reportArgsBuilder.length() > 0) {
                reportArgsBuilder.append(",");
            }
            reportArgsBuilder.append(imageUrl.replace("https://", ""));
        }
        String encodedArgs = Base64.getEncoder().encodeToString(reportArgsBuilder.toString().getBytes());
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
