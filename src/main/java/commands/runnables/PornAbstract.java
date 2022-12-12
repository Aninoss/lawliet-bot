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
import java.util.stream.Collectors;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import core.*;
import core.cache.PatreonCache;
import core.components.ActionRows;
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
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

        Matcher m = RegexPatterns.BOORU_AMOUNT.matcher(args);
        long amount = 1;
        boolean patreon = PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), true) ||
                PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong());

        if (m.find()) {
            String group = m.group();
            args = args.replaceFirst(group, "").replace("  ", " ").trim();
            amount = Long.parseLong(group);
            this.newAmount = (int) Math.min(5, amount);

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
        boolean canBeVideo = patreon || amount == 1;
        if (!canBeVideo) {
            notice = TextManager.getString(getLocale(), Category.NSFW, "porn_novideo", ExternalLinks.PREMIUM_WEBSITE);
        }

        this.args = args;
        ArrayList<String> usedResults = new ArrayList<>();
        event.deferReply();
        do {
            List<BooruImage> pornImages;
            try {
                pornImages = getBooruImages(event.getGuild().getIdLong(), nsfwFilters, args, Math.min(MAX_FILES_PER_MESSAGE, (int) amount), usedResults, canBeVideo);
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

            first = false;
            String messageContent = null;

            while (pornImages.size() > 0) {
                messageContent = generatePostMessagesText(pornImages, event.getTextChannel(), MAX_FILES_PER_MESSAGE);
                if (messageContent == null || messageContent.length() < 2000) {
                    break;
                } else {
                    pornImages = pornImages.stream()
                            .limit(pornImages.size() - 1)
                            .collect(Collectors.toList());
                }
            }
            amount -= pornImages.size();

            if (messageContent != null) {
                ArrayList<ActionRow> actionRows = new ArrayList<>(ActionRows.of(generateButtons(pornImages)));
                if (amount <= 0) {
                    Button loadMoreButton = generateLoadMoreButton(patreon);
                    actionRows.add(ActionRow.of(loadMoreButton));
                }

                setActionRows(actionRows);
                boolean registerButton = amount <= 0 && patreon;
                drawMessageNew(messageContent)
                        .thenAccept(message -> {
                            if (registerButton) {
                                setDrawMessage(message);
                                registerButtonListener(event.getMember(), false);
                            }
                        })
                        .exceptionally(ExceptionLogger.get());
                TimeUnit.SECONDS.sleep(MAX_FILES_PER_MESSAGE / 2);
            }
        } while (amount > 0 && BotPermissionUtil.canWrite(event.getTextChannel()));

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
            String newUrl = pornImage.getOriginalImageUrl()
                    .replace("https://", "")
                    .replace("/images/", "#")
                    .replace("/data/", "|")
                    .replace("/original/", "\\");
            reportArgsBuilder.append(newUrl);
        }

        String encodedArgs = Base64.getEncoder().encodeToString(reportArgsBuilder.toString().getBytes());
        String url = ExternalLinks.REPORT_URL + URLEncoder.encode(encodedArgs, StandardCharsets.UTF_8);
        Button reportButton = Button.of(ButtonStyle.LINK, url, TextManager.getString(getLocale(), Category.NSFW, "porn_report"));
        buttons.add(reportButton);
        return buttons;
    }

    private boolean checkServiceAvailable() {
        try {
            return booruImageDownloader.getPicture(0L, getDomain(), "", false, mustBeExplicit(), false, Collections.emptySet(), Collections.emptyList(), true).get().isPresent();
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
        deregisterListeners();
        onTrigger(getCommandEvent(), this.newAmount + " " + this.args);
        return false;
    }

    @Nullable
    @Override
    public EmbedBuilder draw(@NotNull Member member) throws Throwable {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        StandardGuildMessageChannel channel = slot.getStandardGuildMessageChannel().get();

        ArrayList<String> nsfwFiltersList = new ArrayList<>(DBNSFWFilters.getInstance().retrieve(slot.getGuildId()).getKeywords());
        HashSet<String> nsfwFilters = new HashSet<>();
        nsfwFiltersList.forEach(filter -> nsfwFilters.add(filter.toLowerCase()));
        List<BooruImage> pornImages;
        try {
            boolean premium = PatreonCache.getInstance().isUnlocked(slot.getGuildId());
            if (!premium && slot.getArgs().isEmpty()) {
                notice = TextManager.getString(getLocale(), Category.NSFW, "porn_novideo", ExternalLinks.PREMIUM_WEBSITE);
            }

            String cacheKey = getTrigger() + ":" + slot.getCommandKey().toLowerCase() + ":" + NSFWUtil.getNSFWTagRemoveList(nsfwFiltersList) + ":" + premium;
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
        String messageContent;
        if ((messageContent = generatePostMessagesText(pornImages, channel, 1)) != null) {
            try {
                slot.sendMessage(true, messageContent, ActionRow.of(messageButtons));
            } catch (InterruptedException e) {
                //Ignore
            }
        }

        slot.setArgs("found");
        slot.setNextRequest(Instant.now().plus(10, ChronoUnit.MINUTES));
        return AlertResponse.CONTINUE_AND_SAVE;
    }

    private String generatePostMessagesText(List<BooruImage> pornImages, StandardGuildMessageChannel channel, int max) {
        StringBuilder sb = new StringBuilder(TextManager.getString(getLocale(), Category.NSFW, "porn_title",
                getCommandProperties().emoji(), TextManager.getString(getLocale(), getCategory(), getTrigger() + "_title"),
                getPrefix(), getTrigger()
        ));

        if (this instanceof PornSearchAbstract && pornImages.get(0).getTags().size() > 0) {
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
                                            ArrayList<String> usedResults) throws IllegalTagException {
        if (NSFWUtil.stringContainsBannedTags(search, nsfwFilter)) {
            throw new IllegalTagException();
        }

        ArrayList<CompletableFuture<Optional<BooruImage>>> futures = new ArrayList<>();
        ArrayList<BooruImage> pornImages = new ArrayList<>();

        for (int i = 0; i < amount; i++) {
            try {
                futures.add(
                        booruImageDownloader.getPicture(guildId, domain, search, animatedOnly, mustBeExplicit, canBeVideo, nsfwFilter, usedResults, false)
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

    protected void setNotice(String notice) {
        if (this.notice == null) {
            this.notice = notice;
        }
    }

}
