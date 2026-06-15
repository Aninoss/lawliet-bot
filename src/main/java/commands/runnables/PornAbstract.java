package commands.runnables;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.OnAlertListener;
import commands.listeners.OnButtonListener;
import constants.ExternalLinks;
import constants.LogStatus;
import constants.RegexPatterns;
import core.ExceptionLogger;
import core.Program;
import core.TextManager;
import core.cache.PatreonCache;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.utils.*;
import modules.porn.BooruImage;
import modules.porn.BooruImageDownloader;
import modules.porn.IllegalTagException;
import modules.porn.TooManyTagsException;
import modules.schedulers.AlertResponse;
import mysql.modules.nsfwfilter.DBNSFWFilters;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class PornAbstract extends Command implements OnAlertListener, OnButtonListener {

    public static int MAX_FILES_PER_MESSAGE = 5;
    public static LocalDate NEW_ALERT_DATE = LocalDate.of(2025, 3, 30);

    private static final BooruImageDownloader booruImageDownloader = new BooruImageDownloader();

    private static final Cache<String, List<BooruImage>> alertsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(9, TimeUnit.MINUTES)
            .build();

    private String notice = null;

    public PornAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    public abstract List<BooruImage> getBooruImages(long guildId, Set<String> nsfwFilters, String search, int amount, ArrayList<String> usedResults, boolean canBeVideo, boolean bulkMode) throws Exception;

    public abstract boolean mustBeExplicit();

    public abstract String getDomain();

    private String args = null;
    private int newAmount = 5;

    protected Set<String> getAdditionalFilters() {
        return Collections.emptySet();
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        List<String> nsfwFiltersList = DBNSFWFilters.getInstance().retrieve(event.getGuild().getIdLong()).getKeywords();
        HashSet<String> nsfwFilters = new HashSet<>();
        nsfwFiltersList.forEach(filter -> nsfwFilters.add(filter.toLowerCase()));
        getUserEntityReadOnly().getPersonalNSFWFilter().forEach(filter -> nsfwFilters.add(filter.toLowerCase()));
        args = args.replace("`", "");

        Matcher m = RegexPatterns.BOORU_NUMBER.matcher(args);
        long amount = 1;
        boolean premium = PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), true) ||
                PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong());

        String maxAmountString = System.getenv("BOORU_MAX_AMOUNT");
        int maxAmount = maxAmountString == null ? 30 : Integer.parseInt(maxAmountString);

        if (m.find()) {
            String group = m.group().replace(" ", "");
            args = args.replaceAll("(^| )" + Pattern.quote(group) + "( |$)", " ").replace("  ", " ").trim();
            amount = Long.parseLong(group);
            this.newAmount = (int) Math.min(5, amount);

            if (premium) {
                if (amount > 20 && amount <= maxAmount) {
                    FeatureLogger.inc(PremiumFeature.BOORUS_HIGHER_LIMIT, event.getGuild().getIdLong());
                } else if (amount < 1 || amount > maxAmount) {
                    String content = TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", StringUtil.numToString(maxAmount));
                    drawMessageNew(ComponentsUtil.createCommandComponentTreeError(this, content))
                            .exceptionally(ExceptionLogger.get());
                    return false;
                }
            } else {
                if (amount < 1 || amount > 20) {
                    String content = TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_notinrange", "1", "20", ExternalLinks.PREMIUM_WEBSITE, StringUtil.numToString(maxAmount));
                    drawMessageNew(ComponentsUtil.createCommandComponentTreeError(this, content))
                            .exceptionally(ExceptionLogger.get());
                    return false;
                }
            }
        }

        boolean first = true;
        boolean singleRequest = amount == 1;
        boolean canBeVideo = premium || singleRequest;
        if (!canBeVideo) {
            notice = TextManager.getString(getLocale(), Category.NSFW, "porn_novideo", ExternalLinks.PREMIUM_WEBSITE);
        }

        this.args = args;
        ArrayList<String> usedResults = new ArrayList<>();
        deferReply();
        do {
            List<BooruImage> pornImages;
            try {
                pornImages = getBooruImages(event.getGuild().getIdLong(), nsfwFilters, args, Math.min(MAX_FILES_PER_MESSAGE, (int) amount), usedResults, canBeVideo, false);
            } catch (IllegalTagException e) {
                drawMessageNew(illegalTagsComponents())
                        .exceptionally(ExceptionLogger.get());
                return false;
            } catch (TooManyTagsException e) {
                drawMessageNew(tooManyTagsComponents(e.getMaxTags()))
                        .exceptionally(ExceptionLogger.get());
                return false;
            } catch (NoSuchElementException | IOException e) {
                drawMessageNew(apiUnavailableComponents())
                        .exceptionally(ExceptionLogger.get());
                return false;
            }

            if (pornImages.isEmpty()) {
                if (first) {
                    String effectiveArgs = args;
                    if (this instanceof PornPredefinedAbstract) {
                        effectiveArgs = "";
                    }

                    drawMessageNew(noResultsComponents(effectiveArgs))
                            .exceptionally(ExceptionLogger.get());
                    return false;
                } else {
                    return true;
                }
            }

            if (premium && !singleRequest && pornImages.stream().anyMatch(BooruImage::getVideo)) {
                FeatureLogger.inc(PremiumFeature.BOORUS_VIDEOS, event.getGuild().getIdLong());
            }
            if (getCommandProperties().patreonRequired()) {
                FeatureLogger.inc(PremiumFeature.BOORUS_PREMIUM_ONLY, event.getGuild().getIdLong());
            }

            first = false;
            if (pornImages.size() < MAX_FILES_PER_MESSAGE) {
                amount = 0;
            } else {
                amount -= pornImages.size();
            }

            MessageComponentTree messageComponents = generateComponents(pornImages, event.getMessageChannel(), MAX_FILES_PER_MESSAGE, amount <= 0, premium);
            if (messageComponents != null) {
                boolean registerButton = amount <= 0 && premium;
                drawMessageNew(messageComponents)
                        .thenAccept(message -> {
                            if (registerButton) {
                                setDrawMessage(message);
                                registerButtonListener(event.getMember(), false);
                            }
                        })
                        .exceptionally(ExceptionLogger.get());
                TimeUnit.SECONDS.sleep(MAX_FILES_PER_MESSAGE);
            }
        } while (amount > 0 && BotPermissionUtil.canWrite(event.getMessageChannel()));

        return true;
    }

    private Button generateLoadMoreButton(boolean patreon) {
        String key = patreon ? "porn_morebutton" : "porn_morebutton_disabled";
        Button button = Button.of(ButtonStyle.PRIMARY, "more", TextManager.getString(getLocale(), Category.NSFW, key));
        if (!patreon) {
            button = button.asDisabled();
        }
        return button;
    }

    protected Button generateReportButton(List<BooruImage> pornImages) {
        StringBuilder reportArgsBuilder = new StringBuilder();
        for (BooruImage pornImage : pornImages) {
            if (!reportArgsBuilder.isEmpty()) {
                reportArgsBuilder.append(",");
            }
            String newUrl = pornImage.getOriginalImageUrl()
                    .replace("https://", "")
                    .replace("/images/", "#")
                    .replace("/_images/", "<")
                    .replace("/data/", "|")
                    .replace("/original/", "\\");
            reportArgsBuilder.append(newUrl);
        }

        String encodedArgs = Base64.getEncoder().encodeToString(reportArgsBuilder.toString().getBytes());
        String url = ExternalLinks.REPORT_URL + URLEncoder.encode(encodedArgs, StandardCharsets.UTF_8);
        return Button.of(ButtonStyle.LINK, url, TextManager.getString(getLocale(), Category.NSFW, "porn_report"));
    }

    private MessageComponentTree apiUnavailableComponents() {
        return ComponentsUtil.createErrorApiDown(this, getDomain());
    }

    private MessageComponentTree noResultsComponents(String args) {
        return ComponentsUtil.createErrorNoResults(this, args);
    }

    private MessageComponentTree illegalTagsComponents() {
        String content = TextManager.getString(getLocale(), Category.NSFW, "porn_illegal_tag_desc");
        return ComponentsUtil.createCommandComponentTreeError(this, content);
    }

    private MessageComponentTree tooManyTagsComponents(int maxTags) {
        String content = TextManager.getString(getLocale(), Category.NSFW, "porn_too_many_tags_desc", StringUtil.numToString(maxTags));
        return ComponentsUtil.createCommandComponentTreeError(this, content);
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        FeatureLogger.inc(PremiumFeature.BOORUS_LOAD_MORE_BUTTON, event.getGuild().getIdLong());
        event.deferEdit().queue();
        deregisterListeners();
        onTrigger(getCommandEvent(), this.newAmount + " " + this.args);
        return false;
    }

    @Nullable
    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        GuildMessageChannel channel = slot.getGuildMessageChannel().get();
        boolean premium = PatreonCache.getInstance().isUnlocked(slot.getGuildId());
        boolean newMode = !Program.productionMode() || slot.getCreationTime().isAfter(NEW_ALERT_DATE.atStartOfDay().toInstant(ZoneOffset.UTC));

        ArrayList<String> nsfwFiltersList = new ArrayList<>(DBNSFWFilters.getInstance().retrieve(slot.getGuildId()).getKeywords());
        HashSet<String> nsfwFilters = new HashSet<>();
        nsfwFiltersList.forEach(filter -> nsfwFilters.add(filter.toLowerCase()));
        List<BooruImage> pornImages;
        try {
            if (!premium && slot.getArgs().isEmpty()) {
                notice = TextManager.getString(getLocale(), Category.NSFW, "porn_novideo", ExternalLinks.PREMIUM_WEBSITE);
            }

            if (newMode) {
                pornImages = getBooruImages(Program.getClusterId(), nsfwFilters, slot.getCommandKey(), 1, new ArrayList<>(), premium, true);
            } else {
                String cacheKey = getTrigger() + ":" + slot.getCommandKey().toLowerCase() + ":" + NSFWUtil.generateFilterString(nsfwFiltersList) + ":" + premium;
                pornImages = alertsCache.get(
                        cacheKey,
                        () -> getBooruImages(Program.getClusterId(), nsfwFilters, slot.getCommandKey(), 1, new ArrayList<>(), premium, false)
                );
                if (pornImages.isEmpty()) {
                    alertsCache.invalidate(cacheKey);
                }
            }
        } catch (Throwable e) {
            if (e instanceof IllegalTagException) {
                MessageComponentTree components = illegalTagsComponents();
                ComponentsUtil.addTrackerRemoveLog(getLocale(), components);
                slot.sendMessageComponentTree(getLocale(), false, components);
                return AlertResponse.STOP_AND_DELETE;
            }
            if (e instanceof TooManyTagsException) {
                MessageComponentTree components = tooManyTagsComponents(((TooManyTagsException) e).getMaxTags());
                ComponentsUtil.addTrackerRemoveLog(getLocale(), components);
                slot.sendMessageComponentTree(getLocale(), false, components);
                return AlertResponse.STOP_AND_DELETE;
            }
            if (e.getMessage() != null && e.getMessage().contains("Booru retrieval error")) {
                return AlertResponse.CONTINUE;
            }
            throw e;
        }

        if (pornImages.isEmpty()) {
            if (slot.getArgs().isEmpty()) {
                MessageComponentTree components = noResultsComponents(slot.getCommandKey());
                ComponentsUtil.addTrackerRemoveLog(getLocale(), components);
                slot.sendMessageComponentTree(getLocale(), false, components);
                return AlertResponse.STOP_AND_DELETE;
            } else {
                return AlertResponse.CONTINUE;
            }
        }

        if (newMode) {
            ArrayList<Long> idList = new ArrayList<>();
            slot.getArgs().ifPresent(args -> {
                for (String idString : args.split("\\|")) {
                    if (!StringUtil.stringIsLong(idString)) {
                        continue;
                    }
                    idList.add(Long.parseLong(idString));
                }
            });

            if (slot.getArgs().isEmpty()) {
                for (int i = 1; i < pornImages.size(); i++) {
                    idList.add(0, pornImages.get(i).getId());
                }
            }

            ArrayList<BooruImage> usedBooruImages = new ArrayList<>();
            List<BooruImage> sortedPornImage = pornImages.stream().sorted(Comparator.comparingInt(BooruImage::getScore).reversed()).collect(Collectors.toList());
            for (BooruImage booruImage : sortedPornImage) {
                if (!idList.contains(booruImage.getId())) {
                    idList.add(booruImage.getId());
                    usedBooruImages.add(booruImage);
                }
                if (usedBooruImages.size() >= MAX_FILES_PER_MESSAGE) {
                    break;
                }
            }

            while (idList.size() > 250) {
                idList.remove(0);
            }

            slot.setArgs(Strings.join(idList, '|'));
            slot.setNextRequest(Instant.now().plus(15, ChronoUnit.MINUTES));
            if (usedBooruImages.isEmpty()) {
                return AlertResponse.CONTINUE_AND_SAVE;
            }

            pornImages = usedBooruImages;
        } else {
            slot.setArgs("found");
            slot.setNextRequest(Instant.now().plus(10, ChronoUnit.MINUTES));
        }

        if (premium && pornImages.stream().anyMatch(BooruImage::getVideo)) {
            FeatureLogger.inc(PremiumFeature.BOORUS_VIDEOS, slot.getGuildId());
        }
        if (getCommandProperties().patreonRequired()) {
            FeatureLogger.inc(PremiumFeature.BOORUS_PREMIUM_ONLY, slot.getGuildId());
        }

        MessageComponentTree messageComponents;
        if ((messageComponents = generateComponents(pornImages, channel, newMode ? MAX_FILES_PER_MESSAGE : 1, false, premium)) != null) {
            try {
                slot.sendMessageComponentTree(getLocale(), true, messageComponents);
            } catch (InterruptedException e) {
                //Ignore
            }
        }

        return AlertResponse.CONTINUE_AND_SAVE;
    }

    private MessageComponentTree generateComponents(List<BooruImage> pornImages, GuildMessageChannel channel, int max, boolean showLoadMoreButton, boolean premium) {
        ArrayList<ContainerChildComponent> components = new ArrayList<>();

        if (this instanceof PornSearchAbstract && !pornImages.get(0).getTags().isEmpty()) {
            List<String> tags = pornImages.get(0).getTags();
            if (tags != null && !tags.isEmpty()) {
                StringBuilder tagsStringBuilder = new StringBuilder();
                for (int i = 0; i < tags.size(); i++) {
                    if (i > 0) {
                        tagsStringBuilder.append(", ");
                    }
                    tagsStringBuilder.append("`")
                            .append(tags.get(i))
                            .append("`");
                }
                String tagsString = TextManager.getString(getLocale(), Category.NSFW, "porn_tags") + " " + StringUtil.shortenString(tagsStringBuilder.toString(), 100);
                components.add(TextDisplay.of(tagsString));
                components.add(Separator.createInvisible(Separator.Spacing.SMALL));
            }
        }

        StringBuilder contentStringBuilder = new StringBuilder();
        ArrayList<MediaGalleryItem> mediaGalleryItems = new ArrayList<>();
        boolean spoiler = getGuildEntity().getNsfwSpoilers() && getCommandProperties().nsfw();
        for (int i = 0; i < Math.min(max, pornImages.size()); i++) {
            if (pornImages.get(i) == null) {
                continue;
            }
            String line = TextManager.getString(getLocale(), Category.NSFW, "porn_file", String.valueOf(i + 1), pornImages.get(i).getImageUrl(), pornImages.get(i).getPageUrl());
            contentStringBuilder.append(line)
                    .append('\n');
            if (InternetUtil.stringIsURL(pornImages.get(i).getImageUrl())) {
                mediaGalleryItems.add(
                        MediaGalleryItem.fromUrl(pornImages.get(i).getImageUrl())
                                .withSpoiler(spoiler)
                );
            }
        }
        components.add(TextDisplay.of(contentStringBuilder.toString()));
        components.add(MediaGallery.of(mediaGalleryItems));

        ArrayList<Button> buttons = new ArrayList<>();
        if (showLoadMoreButton) {
            Button loadMoreButton = generateLoadMoreButton(premium);
            buttons.add(loadMoreButton);
        }
        Button reportButton = generateReportButton(pornImages);
        if (reportButton != null) {
            buttons.add(reportButton);
        }
        if (!buttons.isEmpty()) {
            components.add(ActionRow.of(buttons));
        }

        MessageComponentTree commandComponentTree = ComponentsUtil.createCommandComponentTree(this, components);
        if (notice != null) {
            commandComponentTree = ComponentsUtil.addLog(commandComponentTree, LogStatus.WARNING, notice);
        }

        if (BotPermissionUtil.canWrite(channel)) {
            return commandComponentTree;
        }
        return null;
    }

    protected List<BooruImage> downloadPorn(long guildId, Set<String> nsfwFilter, int amount, String domain,
                                            String search, boolean animatedOnly, boolean mustBeExplicit, boolean canBeVideo,
                                            boolean bulkMode, ArrayList<String> usedResults
    ) throws IOException {
        if (NSFWUtil.containsFilterTags(search, nsfwFilter)) {
            throw new IllegalTagException();
        }

        try {
            List<BooruImage> booruImages = booruImageDownloader.getImages(guildId, domain, search, animatedOnly, mustBeExplicit, canBeVideo, bulkMode, nsfwFilter, usedResults, amount).get();
            booruImages.forEach(booruImage -> usedResults.add(booruImage.getImageUrl()));
            return booruImages;
        } catch (Throwable e) {
            throw new IOException("Booru retrieval error");
        }
    }

    protected void setNotice(String notice) {
        if (this.notice == null) {
            this.notice = notice;
        }
    }

}
