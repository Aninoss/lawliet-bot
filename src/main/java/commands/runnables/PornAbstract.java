package commands.runnables;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.OnAlertListener;
import constants.ExternalLinks;
import constants.RegexPatterns;
import constants.Settings;
import core.*;
import core.cache.PatreonCache;
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
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.BaseGuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

public abstract class PornAbstract extends Command implements OnAlertListener {

    private static final BooruImageDownloader booruImageDownloader = new BooruImageDownloader();

    private static final Cache<String, List<BooruImage>> alertsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(9, TimeUnit.MINUTES)
            .build();

    public PornAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    public abstract List<BooruImage> getBooruImages(long guildId, Set<String> nsfwFilters, String search, int amount, ArrayList<String> usedResults, boolean canBeVideo) throws Exception;

    public abstract Optional<String> getNoticeOptional();

    public abstract boolean isExplicit();

    public abstract String getDomain();

    protected Set<String> getAdditionalFilters() {
        return Collections.emptySet();
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        List<String> nsfwFiltersList = DBNSFWFilters.getInstance().retrieve(event.getGuild().getIdLong()).getKeywords();
        HashSet<String> nsfwFilters = new HashSet<>();
        nsfwFiltersList.forEach(filter -> nsfwFilters.add(filter.toLowerCase()));
        args = args.replace("`", "");

        Matcher m = RegexPatterns.BOORU_AMOUNT.matcher(args);
        long amount = 1;
        if (m.find()) {
            String group = m.group();
            args = args.replaceFirst(group, "").replace("  ", " ").trim();
            amount = Long.parseLong(group);
            boolean patreon = PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), true) ||
                    PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong());

            if (!patreon && (amount < 1 || amount > 20)) {
                if (BotPermissionUtil.canWriteEmbed(event.getTextChannel())) {
                    EmbedBuilder eb = EmbedFactory.getEmbedDefault(
                            this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_notinrange", "1", "20", ExternalLinks.PREMIUM_WEBSITE, "30")
                    );
                    eb.setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_title"))
                            .setColor(Settings.PREMIUM_COLOR);
                    drawMessageNew(eb).exceptionally(ExceptionLogger.get());
                } else {
                    drawMessageNew("❌ " + TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_notinrange", "1", "20", ExternalLinks.PREMIUM_WEBSITE, "30"))
                            .exceptionally(ExceptionLogger.get());
                }
                return false;
            } else if (patreon && (amount < 1 || amount > 30)) {
                if (BotPermissionUtil.canWriteEmbed(event.getTextChannel())) {
                    drawMessageNew(EmbedFactory.getEmbedError(
                            this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "30")
                    )).exceptionally(ExceptionLogger.get());
                } else {
                    drawMessageNew("❌ " + TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "30"))
                            .exceptionally(ExceptionLogger.get());
                }
                return false;
            }
        }

        boolean first = true;
        boolean canBeVideo = amount == 1;
        ArrayList<String> usedResults = new ArrayList<>();
        event.deferReply();
        do {
            List<BooruImage> pornImages;
            try {
                pornImages = getBooruImages(event.getGuild().getIdLong(), nsfwFilters, args, Math.min(3, (int) amount), usedResults, canBeVideo);
            } catch (IllegalTagException e) {
                if (BotPermissionUtil.canWriteEmbed(event.getTextChannel())) {
                    drawMessageNew(illegalTagsEmbed()).exceptionally(ExceptionLogger.get());
                } else {
                    drawMessageNew(illegalTagsString()).exceptionally(ExceptionLogger.get());
                }
                return false;
            } catch (TooManyTagsException e) {
                if (BotPermissionUtil.canWriteEmbed(event.getTextChannel())) {
                    drawMessageNew(tooManyTagsEmbed(e.getMaxTags())).exceptionally(ExceptionLogger.get());
                } else {
                    drawMessageNew(tooManyTagsString(e.getMaxTags())).exceptionally(ExceptionLogger.get());
                }
                return false;
            } catch (NoSuchElementException e) {
                postApiUnavailable(event.getTextChannel());
                return false;
            }

            if (pornImages.size() == 0) {
                if (first) {
                    if (!checkServiceAvailable()) {
                        postApiUnavailable(event.getTextChannel());
                    } else {
                        String effectiveArgs = args;
                        if (this instanceof PornPredefinedAbstract) {
                            effectiveArgs = "";
                        }

                        if (BotPermissionUtil.canWriteEmbed(event.getTextChannel())) {
                            drawMessageNew(noResultsEmbed(effectiveArgs)).exceptionally(ExceptionLogger.get());
                        } else {
                            drawMessageNew(noResultsString(effectiveArgs)).exceptionally(ExceptionLogger.get());
                        }
                    }
                    return false;
                } else {
                    return true;
                }
            }

            amount -= pornImages.size();
            first = false;

            Optional<Message> messageTemplateOpt = generatePostMessagesText(pornImages, event.getTextChannel(), 3);
            if (messageTemplateOpt.isPresent()) {
                setComponents(generateButtons(pornImages));
                drawMessageNew(messageTemplateOpt.get().getContentRaw()).exceptionally(ExceptionLogger.get());
                TimeUnit.SECONDS.sleep(1);
            }
        } while (amount > 0 && BotPermissionUtil.canWrite(event.getTextChannel()));

        return true;
    }

    private List<Button> generateButtons(List<BooruImage> pornImages) {
        ArrayList<Button> buttons = new ArrayList<>();
        String tag = pornImages.size() > 1 ? "porn_source" : "porn_source_single";
        StringBuilder reportArgsBuilder = new StringBuilder();
        for (int i = 0; i < pornImages.size(); i++) {
            BooruImage pornImage = pornImages.get(i);
            Button button = Button.of(ButtonStyle.LINK, pornImage.getPageUrl(), TextManager.getString(getLocale(), Category.NSFW, tag, String.valueOf(i + 1)));
            buttons.add(button);

            if (reportArgsBuilder.length() > 0) {
                reportArgsBuilder.append(",");
            }
            reportArgsBuilder.append(pornImage.getOriginalImageUrl());
        }

        String encodedArgs = Base64.getEncoder().encodeToString(reportArgsBuilder.toString().getBytes());
        String url = ExternalLinks.REPORT_URL + URLEncoder.encode(encodedArgs, StandardCharsets.UTF_8);
        Button reportButton = Button.of(ButtonStyle.LINK, url, TextManager.getString(getLocale(), Category.NSFW, "porn_report"));
        buttons.add(reportButton);
        return buttons;
    }

    private boolean checkServiceAvailable() {
        try {
            return booruImageDownloader.getPicture(0L, getDomain(), "", false, isExplicit(), false, Collections.emptySet(), Collections.emptyList(), true).get().isPresent();
        } catch (InterruptedException | ExecutionException | NoSuchElementException | JsonProcessingException e) {
            //Ignore
            return false;
        }
    }

    private void postApiUnavailable(TextChannel textChannel) {
        if (BotPermissionUtil.canWriteEmbed(textChannel)) {
            drawMessageNew(apiUnavailableEmbed()).exceptionally(ExceptionLogger.get());
        } else {
            drawMessageNew(apiUnavailableString()).exceptionally(ExceptionLogger.get());
        }
    }

    private EmbedBuilder apiUnavailableEmbed() {
        return EmbedFactory.getApiDownEmbed(getLocale(), getDomain());
    }

    private String apiUnavailableString() {
        return "❌ " + TextManager.getString(getLocale(), TextManager.GENERAL, "api_down", getDomain());
    }

    private EmbedBuilder noResultsEmbed(String args) {
        EmbedBuilder eb = EmbedFactory.getEmbedError(this)
                .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"));
        if (args.length() > 0) {
            eb.setDescription(TextManager.getNoResultsString(getLocale(), args));
        }
        return eb;
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
    public @NotNull AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        BaseGuildMessageChannel channel = slot.getBaseGuildMessageChannel().get();

        ArrayList<String> nsfwFiltersList = new ArrayList<>(DBNSFWFilters.getInstance().retrieve(slot.getGuildId()).getKeywords());
        HashSet<String> nsfwFilters = new HashSet<>();
        nsfwFiltersList.forEach(filter -> nsfwFilters.add(filter.toLowerCase()));
        List<BooruImage> pornImages;
        try {
            String cacheKey = getTrigger() + ":" + slot.getCommandKey().toLowerCase() + ":" + NSFWUtil.getNSFWTagRemoveList(nsfwFiltersList);
            pornImages = alertsCache.get(cacheKey,
                    () -> getBooruImages(Program.getClusterId(), nsfwFilters, slot.getCommandKey(), 1, new ArrayList<>(), false)
            );
            if (pornImages.isEmpty()) {
                alertsCache.invalidate(cacheKey);
            }
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IllegalTagException) {
                EmbedBuilder eb = illegalTagsEmbed();
                EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                slot.sendMessage(false, eb.build());
                return AlertResponse.STOP_AND_DELETE;
            }
            if (e.getCause() instanceof TooManyTagsException) {
                EmbedBuilder eb = tooManyTagsEmbed(((TooManyTagsException) e.getCause()).getMaxTags());
                EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                slot.sendMessage(false, eb.build());
                return AlertResponse.STOP_AND_DELETE;
            } else {
                throw e;
            }
        }

        if (pornImages.size() == 0) {
            if (slot.getArgs().isEmpty()) {
                EmbedBuilder eb = noResultsEmbed(slot.getCommandKey());
                EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                slot.sendMessage(false, eb.build());
                return AlertResponse.STOP_AND_DELETE;
            } else {
                return AlertResponse.CONTINUE;
            }
        }

        List<Button> messageButtons = generateButtons(pornImages);
        generatePostMessagesText(pornImages, channel, 1)
                .ifPresent(message -> {
                    try {
                        slot.sendMessage(true, message.getContentRaw(), ActionRow.of(messageButtons));
                    } catch (InterruptedException e) {
                        //Ignore
                    }
                });

        slot.setArgs("found");
        slot.setNextRequest(Instant.now().plus(10, ChronoUnit.MINUTES));
        return AlertResponse.CONTINUE_AND_SAVE;
    }

    private Optional<Message> generatePostMessagesText(List<BooruImage> pornImages, BaseGuildMessageChannel channel, int max) {
        StringBuilder sb = new StringBuilder(TextManager.getString(getLocale(), Category.NSFW, "porn_title",
                getCommandProperties().emoji(), TextManager.getString(getLocale(), getCategory(), getTrigger() + "_title"),
                getPrefix(), getTrigger()));

        String notice = getNoticeOptional().orElse(null);
        if (notice != null) {
            sb.append(TextManager.getString(getLocale(), Category.NSFW, "porn_notice", notice))
                    .append("\n");
        } else if (this instanceof PornSearchAbstract) {
            List<String> tags = pornImages.get(0).getTags();
            if (tags != null) {
                sb.append(TextManager.getString(getLocale(), Category.NSFW, "porn_tags"))
                        .append(" ");
                for (int i = 0; i < tags.size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append("`")
                            .append(tags.get(i))
                            .append("`");
                }
                sb.append("\n");
            }
        }

        sb.append("\n");
        for (int i = 0; i < Math.min(max, pornImages.size()); i++) {
            if (pornImages.get(i) != null) {
                sb.append(pornImages.size() > 1 ? "[" + (i + 1) + "] " : "")
                        .append(pornImages.get(i).getImageUrl())
                        .append('\n');
            }
        }

        if (BotPermissionUtil.canWrite(channel)) {
            Message message = new MessageBuilder(sb.toString()).build();
            return Optional.of(message);
        }
        return Optional.empty();
    }

    protected List<BooruImage> downloadPorn(long guildId, Set<String> nsfwFilter, int amount, String domain,
                                            String search, boolean animatedOnly, boolean explicit, boolean canBeVideo,
                                            ArrayList<String> usedResults) throws IllegalTagException {
        if (NSFWUtil.stringContainsBannedTags(search, nsfwFilter)) {
            throw new IllegalTagException();
        }

        ArrayList<CompletableFuture<Optional<BooruImage>>> futures = new ArrayList<>();
        ArrayList<BooruImage> pornImages = new ArrayList<>();

        for (int i = 0; i < amount; i++) {
            try {
                futures.add(
                        booruImageDownloader.getPicture(guildId, domain, search, animatedOnly, explicit, canBeVideo, nsfwFilter, usedResults, false)
                );
            } catch (ExecutionException | JsonProcessingException e) {
                MainLogger.get().error("Error while downloading porn", e);
            }
        }

        futures.forEach(future -> {
            try {
                Optional<BooruImage> pornImageOpt = future.get(10, TimeUnit.SECONDS);
                synchronized (this) {
                    pornImageOpt.ifPresent(pornImage -> {
                        pornImages.add(pornImage);
                        usedResults.add(pornImage.getImageUrl());
                    });
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                if (!e.toString().contains("java.util.NoSuchElementException") &&
                        !e.toString().contains("must start with '[' at 0")
                ) {
                    MainLogger.get().error("Error while downloading porn", e);
                }
            }
        });

        return pornImages;
    }

}
