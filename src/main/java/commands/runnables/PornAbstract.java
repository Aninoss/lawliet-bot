package commands.runnables;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Category;
import commands.Command;
import commands.listeners.OnAlertListener;
import constants.ExternalLinks;
import constants.RegexPatterns;
import core.EmbedFactory;
import core.MainLogger;
import core.TextManager;
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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

public abstract class PornAbstract extends Command implements OnAlertListener {

    private static final BooruImageDownloader booruImageDownloader = new BooruImageDownloader();

    private static final Cache<String, List<BooruImage>> alertsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(9, TimeUnit.MINUTES)
            .build();

    public PornAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    public abstract List<BooruImage> getBooruImages(long guildId, Set<String> nsfwFilters, String search, int amount, ArrayList<String> usedResults) throws Exception;

    public abstract Optional<String> getNoticeOptional();

    public abstract boolean isExplicit();

    protected abstract String getDomain();

    protected Set<String> getAdditionalFilters() {
        return Collections.emptySet();
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws Throwable {
        List<String> nsfwFiltersList = DBNSFWFilters.getInstance().retrieve(event.getGuild().getIdLong()).getKeywords();
        HashSet<String> nsfwFilters = new HashSet<>();
        nsfwFiltersList.forEach(filter -> nsfwFilters.add(filter.toLowerCase()));
        args = args.replace("`", "");

        Matcher m = RegexPatterns.BOORU_AMOUNT_PATTERN.matcher(args);
        long amount = 1;
        if (m.find()) {
            String group = m.group();
            args = args.replaceFirst(group, "").replace("  ", " ").trim();
            amount = Long.parseLong(group);
            boolean patreon = PatreonCache.getInstance().getUserTier(event.getMember().getIdLong(), true) >= 2 ||
                    PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong());

            if (!patreon && (amount < 1 || amount > 20)) {
                if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
                    drawMessageNew(EmbedFactory.getEmbedError(
                            this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_notinrange", "1", "20", ExternalLinks.PATREON_PAGE, "30")
                    ));
                } else {
                    drawMessageNew("❌ " + TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_notinrange", "1", "20", ExternalLinks.PATREON_PAGE, "30"));
                }
                return false;
            } else if (patreon && (amount < 1 || amount > 30)) {
                if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
                    drawMessageNew(EmbedFactory.getEmbedError(
                            this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "30")
                    ));
                } else {
                    drawMessageNew("❌ " + TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "30"));
                }
                return false;
            }
        }

        if (this instanceof PornPredefinedAbstract) {
            args = "";
        }

        boolean first = true;
        ArrayList<String> usedResults = new ArrayList<>();
        do {
            List<BooruImage> pornImages;
            try {
                pornImages = getBooruImages(event.getGuild().getIdLong(), nsfwFilters, args, Math.min(3, (int) amount), usedResults);
            } catch (IllegalTagException e) {
                if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
                    drawMessageNew(illegalTagsEmbed());
                } else {
                    drawMessageNew(illegalTagsString());
                }
                return false;
            } catch (TooManyTagsException e) {
                if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
                    drawMessageNew(tooManyTagsEmbed(e.getMaxTags()));
                } else {
                    drawMessageNew(tooManyTagsString(e.getMaxTags()));
                }
                return false;
            } catch (NoSuchElementException e) {
                postApiUnavailable(event);
                return false;
            }

            if (pornImages.size() == 0) {
                if (first) {
                    if (!checkServiceAvailable()) {
                        postApiUnavailable(event);
                    } else {
                        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
                            drawMessageNew(noResultsEmbed(args));
                        } else {
                            drawMessageNew(noResultsString(args));
                        }
                    }
                    return false;
                } else {
                    return true;
                }
            }

            amount -= pornImages.size();
            first = false;

            Optional<Message> messageTemplateOpt = generatePostMessagesText(pornImages, args, event.getChannel(), 3);
            if (messageTemplateOpt.isPresent()) {
                setComponents(generateButtons(pornImages));
                drawMessageNew(messageTemplateOpt.get().getContentRaw());
                TimeUnit.SECONDS.sleep(1);
            }
        } while (amount > 0 && BotPermissionUtil.canWrite(event.getChannel()));

        return true;
    }

    private List<Button> generateButtons(List<BooruImage> pornImages) {
        ArrayList<Button> buttons = new ArrayList<>();
        String tag = pornImages.size() > 1 ? "porn_source" : "porn_source_single";
        for (int i = 0; i < pornImages.size(); i++) {
            buttons.add(Button.of(ButtonStyle.LINK, pornImages.get(i).getPageUrl(), TextManager.getString(getLocale(), Category.NSFW, tag, String.valueOf(i + 1))));
        }
        return buttons;
    }

    private boolean checkServiceAvailable() {
        try {
            return booruImageDownloader.getPicture(0L, getDomain(), "", false, isExplicit(), Collections.emptySet(), Collections.emptyList(), true).get().isPresent();
        } catch (InterruptedException | ExecutionException | NoSuchElementException e) {
            //Ignore
            return false;
        }
    }

    private void postApiUnavailable(GuildMessageReceivedEvent event) {
        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            drawMessageNew(apiUnavailableEmbed());
        } else {
            drawMessageNew(apiUnavailableString());
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
    public AlertResponse onTrackerRequest(TrackerData slot) throws Throwable {
        TextChannel channel = slot.getTextChannel().get();

        ArrayList<String> nsfwFiltersList = new ArrayList<>(DBNSFWFilters.getInstance().retrieve(slot.getGuildId()).getKeywords());
        HashSet<String> nsfwFilters = new HashSet<>();
        nsfwFiltersList.forEach(filter -> nsfwFilters.add(filter.toLowerCase()));
        List<BooruImage> pornImages;
        try {
            pornImages = alertsCache.get(
                    getTrigger() + ":" + slot.getCommandKey().toLowerCase() + ":" + NSFWUtil.getNSFWTagRemoveList(nsfwFiltersList),
                    () -> getBooruImages(0L, nsfwFilters, slot.getCommandKey(), 1, new ArrayList<>())
            );
        } catch (ExecutionException e) {
            if (e.getCause() instanceof IllegalTagException) {
                EmbedBuilder eb = illegalTagsEmbed();
                EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                channel.sendMessageEmbeds(eb.build()).complete();
                return AlertResponse.STOP_AND_DELETE;
            } if (e.getCause() instanceof TooManyTagsException) {
                EmbedBuilder eb = tooManyTagsEmbed(((TooManyTagsException) e.getCause()).getMaxTags());
                EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                channel.sendMessageEmbeds(eb.build()).complete();
                return AlertResponse.STOP_AND_DELETE;
            } else {
                throw e;
            }
        }

        if (pornImages.size() == 0) {
            if (slot.getArgs().isEmpty()) {
                EmbedBuilder eb = noResultsEmbed(slot.getCommandKey());
                EmbedUtil.addTrackerRemoveLog(eb, getLocale());
                channel.sendMessageEmbeds(eb.build()).complete();
                return AlertResponse.STOP_AND_DELETE;
            } else {
                return AlertResponse.CONTINUE;
            }
        }

        Button messageButton = generateButtons(pornImages).get(0);
        generatePostMessagesText(pornImages, slot.getCommandKey(), channel, 1)
                .ifPresent(message -> {
                    try {
                        slot.sendMessage(true, message.getContentRaw(), ActionRow.of(messageButton));
                    } catch (InterruptedException e) {
                        //Ignore
                    }
                });

        slot.setArgs("found");
        slot.setNextRequest(Instant.now().plus(10, ChronoUnit.MINUTES));
        return AlertResponse.CONTINUE_AND_SAVE;
    }

    private Optional<Message> generatePostMessagesText(List<BooruImage> pornImages, String search, TextChannel channel, int max) {
        StringBuilder sb = new StringBuilder(TextManager.getString(getLocale(), Category.NSFW, "porn_title", this instanceof PornSearchAbstract, getCommandProperties().emoji(), TextManager.getString(getLocale(), getCategory(), getTrigger() + "_title"), getPrefix(), getTrigger(), search));
        getNoticeOptional().ifPresent(notice -> sb.append(TextManager.getString(getLocale(), Category.NSFW, "porn_notice", notice)).append("\n"));
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
                                            String search, boolean animatedOnly, boolean explicit,
                                            ArrayList<String> usedResults) throws IllegalTagException {
        if (NSFWUtil.stringContainsBannedTags(search, nsfwFilter)) {
            throw new IllegalTagException();
        }

        ArrayList<CompletableFuture<Optional<BooruImage>>> futures = new ArrayList<>();
        ArrayList<BooruImage> pornImages = new ArrayList<>();

        for (int i = 0; i < amount; i++) {
            try {
                futures.add(
                        booruImageDownloader.getPicture(guildId, domain, search, animatedOnly, explicit, nsfwFilter, usedResults, false)
                );
            } catch (ExecutionException e) {
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
