package commands.runnables;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.OnAlertListener;
import commands.listeners.OnButtonListener;
import constants.ExternalLinks;
import constants.RegexPatterns;
import constants.Settings;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.Program;
import core.TextManager;
import core.cache.PatreonCache;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.NSFWUtil;
import core.utils.StringUtil;
import modules.porn.BooruImage;
import modules.porn.BooruImageDownloader;
import modules.porn.IllegalTagException;
import modules.porn.TooManyTagsException;
import modules.schedulers.AlertResponse;
import mysql.modules.nsfwfilter.DBNSFWFilters;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class PornAbstract extends Command implements OnAlertListener, OnButtonListener {

    public static int MAX_FILES_PER_MESSAGE = 5;

    private static final BooruImageDownloader booruImageDownloader = new BooruImageDownloader();

    private static final Cache<String, List<BooruImage>> alertsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(9, TimeUnit.MINUTES)
            .build();

    private String notice = null;

    public PornAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    public abstract List<BooruImage> getBooruImages(long guildId, Set<String> nsfwFilters, String search, int amount, ArrayList<String> usedResults, boolean canBeVideo) throws Exception;

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
                    if (BotPermissionUtil.canWriteEmbed(event.getMessageChannel())) {
                        drawMessageNew(EmbedFactory.getEmbedError(
                                this,
                                TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", StringUtil.numToString(maxAmount))
                        )).exceptionally(ExceptionLogger.get());
                    } else {
                        drawMessageNew("❌ " + TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", StringUtil.numToString(maxAmount)))
                                .exceptionally(ExceptionLogger.get());
                    }
                    return false;
                }
            } else {
                if (amount < 1 || amount > 20) {
                    if (BotPermissionUtil.canWriteEmbed(event.getMessageChannel())) {
                        EmbedBuilder eb = EmbedFactory.getEmbedDefault(
                                this,
                                TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_notinrange", "1", "20", ExternalLinks.PREMIUM_WEBSITE, StringUtil.numToString(maxAmount))
                        );
                        eb.setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_title"))
                                .setColor(Settings.PREMIUM_COLOR);
                        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
                    } else {
                        drawMessageNew("❌ " + TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_notinrange", "1", "20", ExternalLinks.PREMIUM_WEBSITE, StringUtil.numToString(maxAmount)))
                                .exceptionally(ExceptionLogger.get());
                    }
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
                pornImages = getBooruImages(event.getGuild().getIdLong(), nsfwFilters, args, Math.min(MAX_FILES_PER_MESSAGE, (int) amount), usedResults, canBeVideo);
            } catch (IllegalTagException e) {
                if (BotPermissionUtil.canWriteEmbed(event.getMessageChannel())) {
                    drawMessageNew(illegalTagsEmbed()).exceptionally(ExceptionLogger.get());
                } else {
                    drawMessageNew(illegalTagsString()).exceptionally(ExceptionLogger.get());
                }
                return false;
            } catch (TooManyTagsException e) {
                if (BotPermissionUtil.canWriteEmbed(event.getMessageChannel())) {
                    drawMessageNew(tooManyTagsEmbed(e.getMaxTags())).exceptionally(ExceptionLogger.get());
                } else {
                    drawMessageNew(tooManyTagsString(e.getMaxTags())).exceptionally(ExceptionLogger.get());
                }
                return false;
            } catch (NoSuchElementException | IOException e) {
                postApiUnavailable(event.getMessageChannel());
                return false;
            }

            if (pornImages.isEmpty()) {
                if (first) {
                    String effectiveArgs = args;
                    if (this instanceof PornPredefinedAbstract) {
                        effectiveArgs = "";
                    }

                    if (BotPermissionUtil.canWriteEmbed(event.getMessageChannel())) {
                        drawMessageNew(noResultsEmbed(effectiveArgs)).exceptionally(ExceptionLogger.get());
                    } else {
                        drawMessageNew(noResultsString(effectiveArgs)).exceptionally(ExceptionLogger.get());
                    }
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
            String messageContent = null;

            while (!pornImages.isEmpty()) {
                messageContent = generatePostMessagesText(pornImages, event.getMessageChannel(), MAX_FILES_PER_MESSAGE, true);
                if (messageContent == null || messageContent.length() < 2000) {
                    break;
                } else {
                    pornImages = pornImages.stream()
                            .limit(pornImages.size() - 1)
                            .collect(Collectors.toList());
                }
            }

            if (pornImages.size() < MAX_FILES_PER_MESSAGE) {
                amount = 0;
            } else {
                amount -= pornImages.size();
            }

            if (messageContent != null) {
                ArrayList<Button> buttons = new ArrayList<>();
                if (amount <= 0) {
                    Button loadMoreButton = generateLoadMoreButton(premium);
                    buttons.add(loadMoreButton);
                }
                buttons.add(generateReportButton(pornImages));

                setComponents(buttons);
                boolean registerButton = amount <= 0 && premium;
                drawMessageNew(messageContent)
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

    private Button generateReportButton(List<BooruImage> pornImages) {
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

    private void postApiUnavailable(GuildMessageChannel channel) {
        if (BotPermissionUtil.canWriteEmbed(channel)) {
            drawMessageNew(apiUnavailableEmbed()).exceptionally(ExceptionLogger.get());
        } else {
            drawMessageNew(apiUnavailableString()).exceptionally(ExceptionLogger.get());
        }
    }

    private EmbedBuilder apiUnavailableEmbed() {
        return EmbedFactory.getApiDownEmbed(this, getDomain());
    }

    private String apiUnavailableString() {
        return "❌ " + TextManager.getString(getLocale(), TextManager.GENERAL, "api_down", getDomain());
    }

    private EmbedBuilder noResultsEmbed(String args) {
        return EmbedFactory.getNoResultsEmbed(this, args);
    }

    private String noResultsString(String args) {
        return "❌ " + TextManager.getNoResultsString(getLocale(), args);
    }

    private EmbedBuilder illegalTagsEmbed() {
        return EmbedFactory.getEmbedError(this)
                .setTitle(TextManager.getString(getLocale(), Category.NSFW, "porn_illegal_tag"))
                .setDescription(TextManager.getString(getLocale(), Category.NSFW, "porn_illegal_tag_desc"));
    }

    private String illegalTagsString() {
        return "❌ " + TextManager.getString(getLocale(), Category.NSFW, "porn_illegal_tag_desc");
    }

    private EmbedBuilder tooManyTagsEmbed(int maxTags) {
        return EmbedFactory.getEmbedError(this)
                .setTitle(TextManager.getString(getLocale(), Category.NSFW, "porn_too_many_tags"))
                .setDescription(TextManager.getString(getLocale(), Category.NSFW, "porn_too_many_tags_desc", StringUtil.numToString(maxTags)));
    }

    private String tooManyTagsString(int maxTags) {
        return "❌ " + TextManager.getString(getLocale(), Category.NSFW, "porn_too_many_tags_desc", StringUtil.numToString(maxTags));
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

        ArrayList<String> nsfwFiltersList = new ArrayList<>(DBNSFWFilters.getInstance().retrieve(slot.getGuildId()).getKeywords());
        HashSet<String> nsfwFilters = new HashSet<>();
        nsfwFiltersList.forEach(filter -> nsfwFilters.add(filter.toLowerCase()));
        List<BooruImage> pornImages;
        try {
            if (!premium && slot.getArgs().isEmpty()) {
                notice = TextManager.getString(getLocale(), Category.NSFW, "porn_novideo", ExternalLinks.PREMIUM_WEBSITE);
            }

            String cacheKey = getTrigger() + ":" + slot.getCommandKey().toLowerCase() + ":" + NSFWUtil.generateFilterString(nsfwFiltersList) + ":" + premium;
            pornImages = alertsCache.get(
                    cacheKey,
                    () -> getBooruImages(Program.getClusterId(), nsfwFilters, slot.getCommandKey(), 1, new ArrayList<>(), premium)
            );
            if (pornImages.isEmpty()) {
                alertsCache.invalidate(cacheKey);
            }
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IllegalTagException) {
                EmbedBuilder eb = illegalTagsEmbed();
                EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                slot.sendMessage(getLocale(), false, eb.build());
                return AlertResponse.STOP_AND_DELETE;
            }
            if (e.getCause() instanceof TooManyTagsException) {
                EmbedBuilder eb = tooManyTagsEmbed(((TooManyTagsException) e.getCause()).getMaxTags());
                EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                slot.sendMessage(getLocale(), false, eb.build());
                return AlertResponse.STOP_AND_DELETE;
            }
            if (e.getMessage() != null && e.getMessage().contains("Booru retrieval error")) {
                return AlertResponse.CONTINUE;
            }
            throw e;
        }

        if (pornImages.isEmpty()) {
            if (slot.getArgs().isEmpty()) {
                EmbedBuilder eb = noResultsEmbed(slot.getCommandKey());
                EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                slot.sendMessage(getLocale(), false, eb.build());
                return AlertResponse.STOP_AND_DELETE;
            } else {
                return AlertResponse.CONTINUE;
            }
        }

        if (premium && pornImages.stream().anyMatch(BooruImage::getVideo)) {
            FeatureLogger.inc(PremiumFeature.BOORUS_VIDEOS, slot.getGuildId());
        }
        if (getCommandProperties().patreonRequired()) {
            FeatureLogger.inc(PremiumFeature.BOORUS_PREMIUM_ONLY, slot.getGuildId());
        }

        Button reportButton = generateReportButton(pornImages);
        String messageContent;
        if ((messageContent = generatePostMessagesText(pornImages, channel, 1, false)) != null) {
            try {
                slot.sendMessage(getLocale(), true, messageContent, ActionRow.of(reportButton));
            } catch (InterruptedException e) {
                //Ignore
            }
        }

        slot.setArgs("found");
        slot.setNextRequest(Instant.now().plus(10, ChronoUnit.MINUTES));
        return AlertResponse.CONTINUE_AND_SAVE;
    }

    private String generatePostMessagesText(List<BooruImage> pornImages, GuildMessageChannel channel, int max, boolean doubleLineBreak) {
        StringBuilder sb = new StringBuilder();

        if (this instanceof PornSearchAbstract && !pornImages.get(0).getTags().isEmpty()) {
            List<String> tags = pornImages.get(0).getTags();
            if (tags != null) {
                sb.append(TextManager.getString(getLocale(), Category.NSFW, "porn_tags"))
                        .append(" ");
                StringBuilder tagsStringBuilder = new StringBuilder();
                for (int i = 0; i < tags.size(); i++) {
                    if (i > 0) {
                        tagsStringBuilder.append(", ");
                    }
                    tagsStringBuilder.append("`")
                            .append(tags.get(i))
                            .append("`");
                }
                sb.append(StringUtil.shortenString(tagsStringBuilder.toString(), 100))
                        .append(doubleLineBreak ? "\n\n" : "\n");
            }
        }

        for (int i = 0; i < Math.min(max, pornImages.size()); i++) {
            if (pornImages.get(i) == null) {
                continue;
            }
            String line = TextManager.getString(getLocale(), Category.NSFW, "porn_file", String.valueOf(i + 1), pornImages.get(i).getImageUrl(), pornImages.get(i).getPageUrl());
            sb.append(line)
                    .append('\n');
        }

        if (notice != null) {
            sb.append('\n')
                    .append(TextManager.getString(getLocale(), Category.NSFW, "porn_notice", notice));
        }

        if (BotPermissionUtil.canWrite(channel)) {
            return sb.toString();
        }
        return null;
    }

    protected List<BooruImage> downloadPorn(long guildId, Set<String> nsfwFilter, int amount, String domain,
                                            String search, boolean animatedOnly, boolean mustBeExplicit, boolean canBeVideo,
                                            ArrayList<String> usedResults) throws IOException {
        if (NSFWUtil.containsFilterTags(search, nsfwFilter)) {
            throw new IllegalTagException();
        }

        try {
            List<BooruImage> booruImages = booruImageDownloader.getImages(guildId, domain, search, animatedOnly, mustBeExplicit, canBeVideo, nsfwFilter, usedResults, amount).get();
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
