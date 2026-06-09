package commands.runnables.nsfwcategory;

import commands.Command;
import commands.CommandEvent;
import commands.CommandManager;
import commands.listeners.CommandProperties;
import commands.runnables.ComponentMenuAbstract;
import commands.runnables.aitoyscategory.UpscalerCommand;
import constants.Emojis;
import constants.ExternalLinks;
import constants.LogStatus;
import constants.Settings;
import core.ExceptionLogger;
import core.LocalFile;
import core.MainLogger;
import core.TextManager;
import core.cache.PatreonCache;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.utils.*;
import modules.graphics.AIWatermarkGraphics;
import modules.txt2img.*;
import mysql.hibernate.entity.user.Txt2ImgEntity;
import mysql.modules.nsfwfilter.DBNSFWFilters;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static commands.runnables.informationcategory.HelpCommand.NSFW_SUBCATEGORY_GENERAL;

@CommandProperties(
        trigger = "txt2hentai",
        emoji = "🖌️",
        executableWithoutArgs = false,
        nsfw = true,
        aliases = {"nsfwimagine", "imaginensfw"},
        subCategory = NSFW_SUBCATEGORY_GENERAL
)
public class Txt2HentaiCommand extends ComponentMenuAbstract {

    public static int MAX_PROMPT_LENGTH = 1000;
    public static int LIMIT_CREATIONS_PER_WEEK = 50;
    public static int PROMPT_MAX_LENGTH = 2000;
    private static final String[] INAPPROPRIATE_CONTENT_FILTERS = {"nigga", "nigger", "niggas", "niggers", "rape", "raping", "raped"};
    private static final String PROMPT_ADDITIONAL = "explicit, lazynsfw, ";
    private static final String NEGATIVE_PROMPT_ADDITIONAL = "looks underage, child, teen, ";
    private final String[] ADDITIONAL_TAGS = {"chibi"};

    private static final String STATE_UPSCALE_RESULTS_ID = "upscale_results";

    private String prompt;
    private String negativePrompt;
    private List<LocalFile> previousImageFiles = Collections.EMPTY_LIST;

    public Txt2HentaiCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    public List<String> getAndExtractFilters(long guildId) {
        ArrayList<String> extractedFilters = new ArrayList<>();
        for (String filter : getFilters(guildId)) {
            extractedFilters.add(filter);
            if (filter.contains("_")) {
                extractedFilters.add(filter.replace("_", " "));
            }
        }
        return extractedFilters;
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        MessageComponentTree errorComponentsIfBanned = createErrorComponentsIfBanned();
        if (errorComponentsIfBanned != null) {
            drawMessageNew(errorComponentsIfBanned)
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        if (args.contains("|")) {
            String[] parts = args.split("\\|");
            if (parts.length <= 2) {
                prompt = parts[0].trim();
                negativePrompt = parts.length == 2 ? parts[1].trim() : "";
            } else {
                String error = getString("error_ambiguous_negativeprompt", "|");
                drawMessageNew(ComponentsUtil.createCommandComponentTreeError(this, TextDisplay.of(error)))
                        .exceptionally(ExceptionLogger.get());
                return false;
            }
        } else {
            prompt = args;
            negativePrompt = "";
        }
        prompt = StringUtil.shortenString(prompt, PROMPT_MAX_LENGTH);
        negativePrompt = StringUtil.shortenString(negativePrompt, PROMPT_MAX_LENGTH);

        String error = checkFilterWords(prompt, event.getGuild().getIdLong());
        if (error != null) {
            drawMessageNew(ComponentsUtil.createCommandComponentTreeError(this, TextDisplay.of(error)))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        StateData stateUpscale = StateData.of(STATE_UPSCALE_RESULTS_ID, STATE_ROOT_ID, getString("root_upscaleprevciousresults"));
        registerListeners(event.getMember(), stateUpscale);
        return true;
    }

    @Draw(state = STATE_ROOT_ID)
    public List<ContainerChildComponent> drawDefault(Member member) {
        ArrayList<ContainerChildComponent> components = new ArrayList<>();

        // Text Prompt
        String promptString = getString("root_textprompt");
        Button promptButton = buttonSecondary(Emojis.MENU_EDIT, e -> {
            Modal modal = setStringModal(
                    promptString,
                    prompt,
                    null,
                    TextInputStyle.PARAGRAPH,
                    1,
                    PROMPT_MAX_LENGTH,
                    newPrompt -> prompt = newPrompt);
            e.replyModal(modal).queue();
            return false;
        });
        String promptContent = StringUtil.shortenString(StringUtil.escapeMarkdownInField(prompt), MAX_PROMPT_LENGTH);
        components.add(Section.of(promptButton, TextDisplay.of(promptString + "\n>>> " + StringUtil.escapeMarkdown(promptContent))));

        // Negative Prompt
        String negativePromptString = getString("root_negativeprompt");
        Button negativePromptButton = buttonSecondary(Emojis.MENU_EDIT, e -> {
            Modal modal = setStringModal(
                    negativePromptString,
                    negativePrompt,
                    null,
                    TextInputStyle.PARAGRAPH,
                    0,
                    PROMPT_MAX_LENGTH,
                    newPrompt -> negativePrompt = newPrompt);
            e.replyModal(modal).queue();
            return false;
        });
        String negativePromptContent = negativePrompt.isEmpty() ? TextManager.getString(getLocale(), TextManager.GENERAL, "notset") : StringUtil.shortenString(StringUtil.escapeMarkdownInField(negativePrompt), MAX_PROMPT_LENGTH);
        components.add(Section.of(negativePromptButton, TextDisplay.of(negativePromptString + "\n>>> " + StringUtil.escapeMarkdown(negativePromptContent))));

        // Number of Images
        String numberOfImagesString = getString("root_numberofimages");
        Button numberOfImagesButton = buttonSecondary(Emojis.MENU_EDIT, e -> {
            Modal modal = setIntModal(
                    numberOfImagesString,
                    null,
                    getUserEntity().getTxt2img().getConfigImages(),
                    1,
                    4,
                    newImages -> {
                        Txt2ImgEntity txt2img = getUserEntity().getTxt2img();
                        txt2img.beginTransaction();
                        txt2img.setConfigImages(newImages);
                        txt2img.commitTransaction();
                    });
            e.replyModal(modal).queue();
            return false;
        });
        String numberOfImagesContent = numberOfImagesString + "\n> " + getUserEntity().getTxt2img().getConfigImages();
        Section numberOfImagesSection = Section.of(numberOfImagesButton, TextDisplay.of(numberOfImagesContent));
        components.add(numberOfImagesSection);

        // Aspect Ratio
        String aspectRatioString = getString("root_aspectratio");
        Function<AspectRatio, String> aspectRatioLabelFunction = aspectRatio -> getString("root_ratio_" + aspectRatio.name(), String.valueOf(aspectRatio.getWidth()), String.valueOf(aspectRatio.getHeight()));
        Button aspectRatioButton = buttonSecondary(Emojis.MENU_EDIT, e -> {
            Modal modal = setEnumModal(
                    aspectRatioString,
                    AspectRatio.class,
                    getUserEntity().getTxt2img().getConfigAspectRatio(),
                    null,
                    aspectRatioLabelFunction,
                    newAspectRatio -> {
                        Txt2ImgEntity txt2img = getUserEntity().getTxt2img();
                        txt2img.beginTransaction();
                        txt2img.setConfigAspectRatio(newAspectRatio);
                        txt2img.commitTransaction();
                    }
            );
            e.replyModal(modal).queue();
            return false;
        });
        AspectRatio aspectRatio = getUserEntity().getTxt2img().getConfigAspectRatio();
        String aspectRatioContent = aspectRatioString + "\n> " + aspectRatioLabelFunction.apply(aspectRatio);
        Section aspectRatioSection = Section.of(aspectRatioButton, TextDisplay.of(aspectRatioContent));
        components.add(aspectRatioSection);

        //Buttons
        Button buyButton = Button.of(ButtonStyle.LINK, "https://lawlietbot.xyz/premium?tab=1", getString("root_buyimagecreations"));
        Button upscaleButton = buttonPrimary(getString("root_upscaleprevciousresults"), event -> {
            setState(STATE_UPSCALE_RESULTS_ID);
            return true;
        });
        components.add(ActionRow.of(buyButton, upscaleButton));

        // Model
        StringSelectMenu.Builder modelSelectMenu = stringSelectMenu(e -> {
            try {
                onModelSelected(e);
            } catch (ExecutionException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });
        modelSelectMenu.setMinValues(1)
                .setMaxValues(1)
                .setPlaceholder(getString("root_selectmodel"));

        for (int i = 0; i < StableDiffusionModel.values().length; i++) {
            StableDiffusionModel model = StableDiffusionModel.values()[i];
            modelSelectMenu.addOption(
                    getString("model_style_" + model.name()),
                    String.valueOf(i),
                    getString("model_name_" + model.name())
            );
        }

        components.add(Separator.createDivider(Separator.Spacing.LARGE));
        components.add(ActionRow.of(modelSelectMenu.build()));
        components.add(TextDisplay.of(Txt2ImgCallTracker.getRemainingImagesText(getLocale(), member.getGuild().getIdLong(), member.getIdLong(), getUserEntity())));
        components.add(TextDisplay.of("> " + getString("root_contentwarning")));
        return components;
    }

    @Draw(state = STATE_UPSCALE_RESULTS_ID)
    public List<ContainerChildComponent> drawUpscaleResults(Member member) {
        if (previousImageFiles.isEmpty()) {
            return List.of(
                    TextDisplay.of(getString("upscale_noimages"))
            );
        }

        boolean markAsSpoiler = getGuildEntity().getNsfwSpoilers();
        ArrayList<ContainerChildComponent> components = new ArrayList<>();
        components.add(TextDisplay.of(getString("upscale_desc")));

        for (int i = 0; i < previousImageFiles.size(); i++) {
            LocalFile file = previousImageFiles.get(i);
            components.add(Separator.createInvisible(Separator.Spacing.SMALL));

            String text = getString("upscale_image", String.valueOf(i + 1));
            Section section = Section.of(Thumbnail.fromUrl(file.cdnGetUrl()).withSpoiler(markAsSpoiler), TextDisplay.of(text));
            components.add(section);

            Button button = buttonPrimary(getString("upscale_button"), Emojis.MENU_SHORT_ARROW_RIGHT_GRAY, event -> {
                Command command = CommandManager.createCommandByClass(UpscalerCommand.class, getLocale(), getPrefix());
                command.addAttachment("files", List.of(file));
                CommandManager.manage(getCommandEvent(), command, "", getGuildEntity(), Instant.now(), false);
                return false;
            });
            components.add(ActionRow.of(button));
        }

        return components;
    }

    private void onModelSelected(StringSelectInteractionEvent event) throws ExecutionException, InterruptedException {
        MessageComponentTree errorComponentsIfBanned = createErrorComponentsIfBanned();
        if (errorComponentsIfBanned != null) {
            deregisterListeners();
            drawMessage(errorComponentsIfBanned)
                    .exceptionally(ExceptionLogger.get());
            return;
        }

        boolean premium = PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), true) ||
                PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong());
        boolean isFirstToday = !LocalDate.now().equals(getUserEntity().getTxt2img().getCallsDate());
        int remainingCalls = Txt2ImgCallTracker.getRemainingCalls(getEntityManager(), event.getMember().getIdLong(), premium);
        if (remainingCalls <= 0) {
            setLog(LogStatus.FAILURE, getString(premium ? "error_nocalls" : "error_nocalls_nopremium"));
            return;
        }

        Txt2ImgEntity txt2img = getUserEntity().getTxt2img();
        String localPrompt = prompt;
        String localNegativePrompt = negativePrompt;
        int localImages = Math.min(txt2img.getConfigImages(), remainingCalls);
        AspectRatio localAspectRatio = txt2img.getConfigAspectRatio();
        boolean markAsSpoiler = getGuildEntity().getNsfwSpoilers();

        if (premium) {
            FeatureLogger.inc(PremiumFeature.AI, event.getGuild().getIdLong());
        }
        Txt2ImgCallTracker.increaseCalls(getEntityManager(), event.getUser().getIdLong(), premium, localImages);

        StableDiffusionModel model = StableDiffusionModel.values()[Integer.parseInt(event.getValues().get(0))];
        String predictionId = RunPodDownloader.createTxt2ImgPrediction(
                model,
                PROMPT_ADDITIONAL + model.getAdditionalPrompt() + localPrompt,
                NEGATIVE_PROMPT_ADDITIONAL + model.getAdditionalNegativePrompt() + localNegativePrompt,
                localImages,
                localAspectRatio
        ).get();

        AtomicReference<PredictionResult> predictionResult = new AtomicReference<>(null);
        Instant startTime = Instant.now();
        AtomicLong messageId = new AtomicLong(0);
        AtomicReference<Throwable> error = new AtomicReference<>();

        if (requestProgress(event, error, messageId, localPrompt, localNegativePrompt, model, localImages,
                predictionId, predictionResult, startTime, isFirstToday, markAsSpoiler)
        ) {
            poll(Duration.ofSeconds(2), () -> requestProgress(event, error, messageId, localPrompt,
                    localNegativePrompt, model, localImages, predictionId, predictionResult, startTime,
                    isFirstToday, markAsSpoiler));
        }
    }

    private MessageComponentTree createErrorComponentsIfBanned() {
        if (getGuildEntity().getTxt2imgBanned()) {
            String error = getString("error_banned_guild", ExternalLinks.SERVER_INVITE_URL);
            return ComponentsUtil.createCommandComponentTreeError(this, TextDisplay.of(error));
        }

        Instant bannedUntil = getUserEntity()
                .getTxt2img()
                .getBannedUntil();

        if (bannedUntil != null && Instant.now().isBefore(bannedUntil)) {
            String error = getString("error_banned", String.valueOf(bannedUntil.getEpochSecond()));
            return ComponentsUtil.createCommandComponentTreeError(this, TextDisplay.of(error));
        }

        return null;
    }

    private String checkFilterWords(String prompt, long guildId) {
        if (NSFWUtil.containsNormalFilterTags(prompt, List.of(INAPPROPRIATE_CONTENT_FILTERS))) {
            return getString("error_generalfilterblock");
        }
        if (NSFWUtil.containsNormalFilterTags(prompt, getAndExtractFilters(guildId))) {
            return getString("error_filterblock");
        }
        return null;
    }

    private Boolean requestProgress(StringSelectInteractionEvent event, AtomicReference<Throwable> error,
                                    AtomicLong messageId, String localPrompt, String localNegativePrompt, StableDiffusionModel model,
                                    int images, String predictionId,
                                    AtomicReference<PredictionResult> predictionResult, Instant startTime,
                                    boolean includePromptHelp, boolean markAsSpoiler
    ) {
        if (!BotPermissionUtil.canWrite(event.getGuildChannel(), Permission.MESSAGE_HISTORY)) {
            return false;
        }
        if (error.get() != null) {
            ExceptionUtil.handleCommandException(error.get(), this, getCommandEvent(), getGuildEntity());
            return false;
        }
        if (messageId.get() == -1) {
            return true;
        }

        List<ContainerChildComponent> loadingComponents = generateLoadingComponents(event.getMember(), localPrompt, localNegativePrompt,
                model, images, predictionId, predictionResult, startTime, includePromptHelp, markAsSpoiler
        );
        if (messageId.get() == 0) {
            messageId.set(-1);
            event.getHook().sendMessageComponents(ComponentsUtil.createCommandComponentTree(this, loadingComponents)).useComponentsV2().queue(message -> messageId.set(message.getIdLong()), error::set);
        } else {
            event.getHook().editMessageComponentsById(messageId.get(), ComponentsUtil.createCommandComponentTree(this, loadingComponents)).useComponentsV2().queue(null, error::set);
        }

        return predictionResult.get() == null || List.of(PredictionResult.Status.IN_QUEUE, PredictionResult.Status.IN_PROGRESS).contains(predictionResult.get().getStatus());
    }

    private List<ContainerChildComponent> generateLoadingComponents(Member member, String prompt, String negativePrompt, StableDiffusionModel model,
                                                                    int images, String predictionId, AtomicReference<PredictionResult> predictionResult,
                                                                    Instant startTime, boolean includePromptHelp, boolean markAsSpoiler
    ) {
        ArrayList<ContainerChildComponent> components = new ArrayList<>();

        if (predictionResult.get() == null || predictionResult.get().getStatus() != PredictionResult.Status.COMPLETED) {
            try {
                predictionResult.set(RunPodDownloader.retrieveTxt2ImgPrediction(model, predictionId, startTime, images).get());
                if (predictionResult.get().getStatus() == PredictionResult.Status.COMPLETED) {
                    List<LocalFile> newOutputs = InternetUtil.base64ToLocalFile(predictionResult.get().getOutputs());
                    newOutputs.forEach(file -> {
                        try {
                            AIWatermarkGraphics.addAIWatermark(file);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    predictionResult.get().setOutputs(newOutputs.stream().map(LocalFile::cdnGetUrl).collect(Collectors.toList()));
                    Txt2ImgLogger.log(prompt, negativePrompt, member, model.name(), predictionResult.get().getOutputs());
                }
            } catch (Throwable e) {
                MainLogger.get().error("Prediction failed", e);
                predictionResult.set(PredictionResult.failed(PredictionResult.Error.GENERAL));
            }
        }

        switch (predictionResult.get().getStatus()) {
            case COMPLETED -> {
                previousImageFiles = predictionResult.get().getOutputs().stream()
                        .filter(url -> url.startsWith(LocalFile.CDN_ROOT_URL))
                        .map(LocalFile::cdnFromUrl)
                        .collect(Collectors.toList());
                List<MediaGalleryItem> items = predictionResult.get().getOutputs().stream()
                        .map(url -> MediaGalleryItem.fromUrl(url).withSpoiler(markAsSpoiler))
                        .collect(Collectors.toList());
                components.add(MediaGallery.of(items));
                components.add(TextDisplay.of(getString("loading_footer")));
                if (includePromptHelp) {
                    String promptHelp = getString("loading_prompthelp");
                    components.add(TextDisplay.of(promptHelp));
                }
            }
            case FAILED -> {
                components.add(TextDisplay.of(getString("loading_error")));
            }
            default -> {
                String processing = getString("loading_processing");
                String progressValue = predictionResult.get().getProgress() > 0.0
                        ? getString("loading_processing_percent", String.valueOf((int) (predictionResult.get().getProgress() * 100)))
                        : getString("loading_processing_inqueue");
                String progress = getString("loading_processing_progress",
                        StringUtil.getBar(predictionResult.get().getProgress(), 12),
                        progressValue
                );
                components.add(TextDisplay.of(processing + "\n" + progress));
            }
        }

        components.add(Separator.createDivider(Separator.Spacing.SMALL));

        String promptString = getString("root_textprompt");
        String promptContent = StringUtil.shortenString(StringUtil.escapeMarkdownInField(prompt), MAX_PROMPT_LENGTH);
        components.add(TextDisplay.of(promptString + "\n>>> " + StringUtil.escapeMarkdown(promptContent)));

        String negativePromptString = getString("root_negativeprompt");
        String negativePromptContent = negativePrompt.isEmpty() ? TextManager.getString(getLocale(), TextManager.GENERAL, "notset") : StringUtil.shortenString(StringUtil.escapeMarkdownInField(negativePrompt), MAX_PROMPT_LENGTH);
        components.add(TextDisplay.of(negativePromptString + "\n>>> " + StringUtil.escapeMarkdown(negativePromptContent)));

        String modelString = getString("loading_model");
        components.add(TextDisplay.of(modelString + "\n> " + getString("model_name_" + model.name())));

        return components;
    }

    public List<String> getFilters(long guildId) {
        List<String> guildFilters = DBNSFWFilters.getInstance().retrieve(guildId).getKeywords();
        ArrayList<String> filters = new ArrayList<>(List.of(Settings.NSFW_FILTERS));
        guildFilters.forEach(filter -> filters.add(filter.toLowerCase()));
        filters.addAll(Arrays.asList(ADDITIONAL_TAGS));
        return filters;
    }

}
