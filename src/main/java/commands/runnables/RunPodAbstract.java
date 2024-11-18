package commands.runnables;

import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.CommandManager;
import commands.runnables.aitoyscategory.UpscalerCommand;
import commands.runnables.nsfwcategory.Txt2HentaiCommand;
import constants.Emojis;
import constants.ExternalLinks;
import constants.LogStatus;
import core.*;
import core.cache.PatreonCache;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.modals.ModalMediator;
import core.utils.*;
import modules.txt2img.*;
import mysql.hibernate.entity.user.Txt2ImgEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public abstract class RunPodAbstract extends NavigationAbstract {

    public static int LIMIT_CREATIONS_PER_WEEK = 50;
    public static int PROMPT_MAX_LENGTH = 2000;
    public static String DEFAULT_NEGATIVE_PROMPT = "low-res, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry, artist name, (deformed, distorted, disfigured:1.3), poorly drawn, bad anatomy, wrong anatomy, extra limb, missing limb, floating limbs, (mutated hands and fingers:1.4), disconnected limbs, mutation, mutated, ugly, disgusting, blurry, amputation, monochrome";
    private static final String[] INAPPROPRIATE_CONTENT_FILTERS = {"nigga", "nigger", "niggas", "niggers", "rape", "raping", "raped"};

    private static final int STATE_ADJUST_IMAGES = 1,
            STATE_ADJUST_RATIO = 2,
            STATE_UPSCALE_RESULTS = 3;

    private final String additionalNegativePrompt;
    private String prompt;
    private String negativePrompt;
    private List<? extends File> previousImageFiles = null;

    public RunPodAbstract(Locale locale, String prefix, String additionalNegativePrompt) {
        super(locale, prefix);
        this.additionalNegativePrompt = additionalNegativePrompt;
    }

    public abstract List<String> getFilters(long guildId);

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
        EmbedBuilder errorEmbedIfBanned = getErrorEmbedIfBanned();
        if (errorEmbedIfBanned != null) {
            drawMessageNew(errorEmbedIfBanned)
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        if (args.contains("|")) {
            String[] parts = args.split("\\|");
            if (parts.length <= 2) {
                prompt = parts[0].trim();
                negativePrompt = parts.length == 2 ? parts[1].trim() : "";
            } else {
                drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_ambiguous_negativeprompt", "|")))
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
            drawMessageNew(EmbedFactory.getEmbedError(this, error))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        registerNavigationListener(event.getMember());
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                String promptId = "prompt";
                TextInput textInputPrompt = TextInput.create(promptId, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_textprompt_title"), TextInputStyle.PARAGRAPH)
                        .setValue(prompt)
                        .setMinLength(1)
                        .setMaxLength(PROMPT_MAX_LENGTH)
                        .build();

                String negativePromptId = "negative_prompt";
                TextInput textInputNegativePrompt = TextInput.create(negativePromptId, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_negativeprompt_title"), TextInputStyle.PARAGRAPH)
                        .setValue(negativePrompt.isBlank() ? null : negativePrompt)
                        .setMinLength(0)
                        .setMaxLength(PROMPT_MAX_LENGTH)
                        .setRequired(false)
                        .build();

                Modal modal = ModalMediator.createDrawableCommandModal(this, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_default_prompts"), e -> {
                            String newPrompt = e.getValue(promptId).getAsString();
                            String error = checkFilterWords(newPrompt, e.getGuild().getIdLong());
                            if (error != null) {
                                setLog(LogStatus.FAILURE, error.replace("`", ""));
                                return null;
                            }

                            prompt = newPrompt;
                            negativePrompt = e.getValue(negativePromptId) != null ? e.getValue(negativePromptId).getAsString() : "";
                            setLog(LogStatus.SUCCESS, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_default_promptupdate"));
                            return null;
                        }).addComponents(ActionRow.of(textInputPrompt), ActionRow.of(textInputNegativePrompt))
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case 1 -> {
                setState(STATE_ADJUST_IMAGES);
                return true;
            }
            case 2 -> {
                setState(STATE_ADJUST_RATIO);
                return true;
            }
            case 3 -> {
                if (previousImageFiles == null) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_nopreviousresults"));
                    return true;
                }
                setState(STATE_UPSCALE_RESULTS);
                return true;
            }
            default -> {
                return true;
            }
        }
    }

    @ControllerButton(state = STATE_ADJUST_IMAGES)
    public boolean onButtonAdjustImages(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }

        Txt2ImgEntity txt2img = getUserEntity().getTxt2img();
        txt2img.beginTransaction();
        txt2img.setConfigImages(i + 1);
        txt2img.commitTransaction();

        setLog(LogStatus.SUCCESS, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_images_set", StringUtil.numToString(i + 1)));
        setState(DEFAULT_STATE);
        return true;
    }

    @ControllerButton(state = STATE_ADJUST_RATIO)
    public boolean onButtonAdjustRatio(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }
        AspectRatio aspectRatio = AspectRatio.values()[i];

        Txt2ImgEntity txt2img = getUserEntity().getTxt2img();
        txt2img.beginTransaction();
        txt2img.setConfigAspectRatio(aspectRatio);
        txt2img.commitTransaction();

        setLog(LogStatus.SUCCESS, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_ratio_set",
                TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_ratio_" + aspectRatio.name(),
                        String.valueOf(aspectRatio.getWidth(this instanceof Txt2HentaiCommand)),
                        String.valueOf(aspectRatio.getHeight(this instanceof Txt2HentaiCommand))
                )
        ));
        setState(DEFAULT_STATE);
        return true;
    }

    @ControllerButton(state = STATE_UPSCALE_RESULTS)
    public boolean onButtonUpscaleResults(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }
        return true;
    }

    @ControllerStringSelectMenu(state = DEFAULT_STATE)
    public boolean onSelectMenuMain(StringSelectInteractionEvent event, int i) throws ExecutionException, InterruptedException {
        EmbedBuilder errorEmbedIfBanned = getErrorEmbedIfBanned();
        if (errorEmbedIfBanned != null) {
            deregisterListeners();
            drawMessage(errorEmbedIfBanned)
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        boolean premium = PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), true) ||
                PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong());
        boolean isFirstToday = !LocalDate.now().equals(getUserEntity().getTxt2img().getCallsDate());
        int remainingCalls = Txt2ImgCallTracker.getRemainingCalls(getEntityManager(), event.getMember().getIdLong(), premium);
        if (remainingCalls <= 0) {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), Category.AI_TOYS, premium ? "txt2img_nocalls" : "txt2img_nocalls_nopremium"));
            return true;
        }

        previousImageFiles = null;
        Txt2ImgEntity txt2img = getUserEntity().getTxt2img();

        String localPrompt = prompt;
        String localNegativePrompt = negativePrompt;
        int localImages = Math.min(txt2img.getConfigImages(), remainingCalls);
        AspectRatio localAspectRatio = txt2img.getConfigAspectRatio();

        if (premium) {
            FeatureLogger.inc(PremiumFeature.AI, event.getGuild().getIdLong());
        }
        Txt2ImgCallTracker.increaseCalls(getEntityManager(), event.getUser().getIdLong(), premium, localImages);

        StableDiffusionModel model = StableDiffusionModel.values()[Integer.parseInt(event.getValues().get(0))];
        String predictionId = RunPodDownloader.createTxt2ImgPrediction(
                model,
                localPrompt + model.getAdditionalPrompt(),
                additionalNegativePrompt + localNegativePrompt + (localNegativePrompt.isEmpty() ? "" : ", ") + DEFAULT_NEGATIVE_PROMPT + model.getAdditionalNegativePrompt(),
                localImages,
                localAspectRatio
        ).get();

        AtomicReference<PredictionResult> predictionResult = new AtomicReference<>(null);
        Instant startTime = Instant.now();
        AtomicLong messageId = new AtomicLong(0);
        AtomicReference<Throwable> error = new AtomicReference<>();

        String modelName = getString("model_name_" + model.name());
        setLog(LogStatus.SUCCESS, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_go", modelName));

        if (requestProgress(event, error, messageId, localPrompt, localNegativePrompt, model, localImages,
                localAspectRatio, predictionId, predictionResult, startTime, isFirstToday)
        ) {
            poll(Duration.ofSeconds(2), () -> requestProgress(event, error, messageId, localPrompt,
                    localNegativePrompt, model, localImages, localAspectRatio, predictionId, predictionResult, startTime,
                    isFirstToday));
        }
        return true;
    }

    @ControllerStringSelectMenu(state = STATE_UPSCALE_RESULTS)
    public boolean onSelectMenuUpscaleResults(StringSelectInteractionEvent event, int i) {
        List<File> files = event.getValues().stream()
                .map(value -> previousImageFiles.get(Integer.parseInt(value)))
                .collect(Collectors.toList());

        Command command = CommandManager.createCommandByClass(UpscalerCommand.class, getLocale(), getPrefix());
        command.addAttachment("files", files);
        CommandManager.manage(getCommandEvent(), command, "", getGuildEntity(), Instant.now(), false);
        setState(DEFAULT_STATE);
        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) {
        ArrayList<ActionRow> actionRows = new ArrayList<>();

        ArrayList<Button> buttons = new ArrayList<>();
        String[] buttonLabels = TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_default_options").split("\n");
        for (int i = 0; i < buttonLabels.length; i++) {
            if (i == 4) {
                buttons.add(Button.of(ButtonStyle.LINK, ExternalLinks.PREMIUM_WEBSITE + "?tab=1", buttonLabels[i]));
            } else {
                buttons.add(Button.of(ButtonStyle.PRIMARY, String.valueOf(i), buttonLabels[i]));
            }
        }
        actionRows.add(ActionRow.of(buttons));

        StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("model")
                .setMinValues(1)
                .setMaxValues(1)
                .setPlaceholder(TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_selectmodel"));

        for (int i = 0; i < StableDiffusionModel.values().length; i++) {
            StableDiffusionModel model = StableDiffusionModel.values()[i];
            if (!model.getClasses().contains(getClass())) {
                continue;
            }
            menuBuilder.addOption(
                    getString("model_style_" + model.name()),
                    String.valueOf(i),
                    getString("model_name_" + model.name())
            );
        }
        actionRows.add(ActionRow.of(menuBuilder.build()));
        setActionRows(actionRows);

        Txt2ImgEntity txt2img = getUserEntity().getTxt2img();
        EmbedBuilder eb = generateOptionsEmbed(prompt, negativePrompt, null, txt2img.getConfigImages(), txt2img.getConfigAspectRatio());
        eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), "> " + getString("contentwarning"), false);
        return EmbedUtil.setFooter(eb, this, Txt2ImgCallTracker.getRemainingImagesText(getLocale(), member.getGuild().getIdLong(), member.getIdLong(), getUserEntity()));
    }

    @Draw(state = STATE_ADJUST_IMAGES)
    public EmbedBuilder drawAdjustImages(Member member) {
        setComponents(TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_images_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this,
                TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_images_desc"),
                TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_images_title")
        );
    }

    @Draw(state = STATE_ADJUST_RATIO)
    public EmbedBuilder drawAdjustRatio(Member member) {
        String[] options = Arrays.stream(AspectRatio.values())
                .map(ratio -> TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_ratio_" + ratio.name(),
                        String.valueOf(ratio.getWidth(this instanceof Txt2HentaiCommand)),
                        String.valueOf(ratio.getHeight(this instanceof Txt2HentaiCommand))
                ))
                .toArray(String[]::new);

        setComponents(options);
        return EmbedFactory.getEmbedDefault(this,
                TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_ratio_desc"),
                TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_ratio_title")
        );
    }

    @Draw(state = STATE_UPSCALE_RESULTS)
    public EmbedBuilder drawUpscaleResults(Member member) {
        StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("upscaler")
                .setMinValues(1)
                .setMaxValues(previousImageFiles.size())
                .setPlaceholder(TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_upscaler_placeholder"));
        for (int i = 0; i < previousImageFiles.size(); i++) {
            menuBuilder.addOption(
                    TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_upscaler_image", StringUtil.numToString(i + 1)),
                    String.valueOf(i)
            );
        }

        setComponents(menuBuilder.build());
        return EmbedFactory.getEmbedDefault(this,
                TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_upscale_desc"),
                TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_upscale_title")
        );
    }

    private EmbedBuilder getErrorEmbedIfBanned() {
        if (getGuildEntity().getTxt2imgBanned()) {
            String error = TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_banned_guild", ExternalLinks.SERVER_INVITE_URL);
            return EmbedFactory.getEmbedError(this, error);
        }

        Instant bannedUntil = getUserEntity()
                .getTxt2img()
                .getBannedUntil();

        if (bannedUntil != null && Instant.now().isBefore(bannedUntil)) {
            String error = TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_banned", String.valueOf(bannedUntil.getEpochSecond()));
            return EmbedFactory.getEmbedError(this, error);
        }

        return null;
    }

    private String checkFilterWords(String prompt, long guildId) {
        if (NSFWUtil.containsNormalFilterTags(prompt, List.of(INAPPROPRIATE_CONTENT_FILTERS))) {
            return TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_generalfilterblock");
        }
        if (NSFWUtil.containsNormalFilterTags(prompt, getAndExtractFilters(guildId))) {
            return getString("filterblock");
        }
        return null;
    }

    private Boolean requestProgress(StringSelectInteractionEvent event, AtomicReference<Throwable> error,
                                    AtomicLong messageId, String localPrompt, String localNegativePrompt, StableDiffusionModel model,
                                    int images, AspectRatio aspectRatio, String predictionId,
                                    AtomicReference<PredictionResult> predictionResult, Instant startTime,
                                    boolean includePromptHelp
    ) {
        if (!BotPermissionUtil.canWriteEmbed(event.getGuildChannel(), Permission.MESSAGE_HISTORY)) {
            return false;
        }
        if (error.get() != null) {
            ExceptionUtil.handleCommandException(error.get(), this, getCommandEvent(), getGuildEntity());
            return false;
        }
        if (messageId.get() == -1) {
            return true;
        }

        List<MessageEmbed> messageEmbeds = generateLoadingEmbeds(event.getMember(), localPrompt, localNegativePrompt,
                model, images, aspectRatio, predictionId, predictionResult, startTime, includePromptHelp
        );
        if (messageId.get() == 0) {
            messageId.set(-1);
            event.getHook().sendMessageEmbeds(messageEmbeds).queue(message -> messageId.set(message.getIdLong()), error::set);
        } else {
            event.getHook().editMessageEmbedsById(messageId.get(), messageEmbeds).queue(null, error::set);
        }

        return predictionResult.get() == null || List.of(PredictionResult.Status.IN_QUEUE, PredictionResult.Status.IN_PROGRESS).contains(predictionResult.get().getStatus());
    }

    private EmbedBuilder generateOptionsEmbed(String prompt, String negativePrompt, StableDiffusionModel model, int images, AspectRatio aspectRatio) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .addField(
                        TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_textprompt_title"),
                        "```" + StringUtil.shortenString(StringUtil.escapeMarkdownInField(prompt), MessageEmbed.VALUE_MAX_LENGTH - 6) + "```",
                        false
                );

        eb.addField(
                TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_negativeprompt_title"),
                negativePrompt.isEmpty() ? TextManager.getString(getLocale(), TextManager.GENERAL, "notset") : "```" + StringUtil.shortenString(StringUtil.escapeMarkdownInField(negativePrompt), MessageEmbed.VALUE_MAX_LENGTH - 6) + "```",
                false
        );

        String options = TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_options",
                StringUtil.numToString(images),
                TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_ratio_" + aspectRatio.name(),
                        String.valueOf(aspectRatio.getWidth(this instanceof Txt2HentaiCommand)),
                        String.valueOf(aspectRatio.getHeight(this instanceof Txt2HentaiCommand))
                )
        );
        if (model != null) {
            String modelName = getString("model_name_" + model.name());
            options += "\n" + TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_options_model", modelName);
        }

        return eb.addField(
                TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_options_title"),
                options,
                false
        );
    }

    private List<MessageEmbed> generateLoadingEmbeds(Member member, String prompt, String negativePrompt, StableDiffusionModel model,
                                                     int images, AspectRatio aspectRatio, String predictionId,
                                                     AtomicReference<PredictionResult> predictionResult,
                                                     Instant startTime, boolean includePromptHelp
    ) {
        ArrayList<MessageEmbed> embeds = new ArrayList<>();

        if (predictionResult.get() == null || predictionResult.get().getStatus() != PredictionResult.Status.COMPLETED) {
            try {
                predictionResult.set(RunPodDownloader.retrieveTxt2ImgPrediction(model, predictionId, startTime, images).get());
                if (predictionResult.get().getStatus() == PredictionResult.Status.COMPLETED) {
                    if (model.getCustomModel()) {
                        List<String> newOutputs = InternetUtil.base64ToTempUrl(predictionResult.get().getOutputs());
                        predictionResult.get().setOutputs(newOutputs);
                    }
                    if (model.getCheckNsfw() && Program.productionMode()) {
                        predictionResult.get().setOutputs(processNsfwImages(predictionResult.get().getOutputs()));
                    }
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
                boolean first = true;
                for (String output : predictionResult.get().getOutputs()) {
                    EmbedBuilder eb = (first ? generateOptionsEmbed(prompt, negativePrompt, model, images, aspectRatio) : EmbedFactory.getEmbedDefault(this))
                            .setImage(output);
                    EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_footer_temp"));
                    if (first && includePromptHelp) {
                        eb.addField(
                                Emojis.ZERO_WIDTH_SPACE.getFormatted(),
                                TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_prompthelp"),
                                false
                        );
                    }
                    first = false;
                    embeds.add(eb.build());
                }
            }
            case FAILED -> {
                String error;
                if (predictionResult.get().getError() == PredictionResult.Error.NSFW) {
                    error = TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_nsfw");
                } else {
                    error = TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_error");
                }
                EmbedBuilder eb = EmbedFactory.getEmbedError(this, error);
                embeds.add(eb.build());
            }
            default -> {
                String progress = predictionResult.get().getProgress() > 0.0
                        ? TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_processing_percent", String.valueOf((int) (predictionResult.get().getProgress() * 100)))
                        : TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_processing_inqueue");
                String processingString = TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_processing",
                        StringUtil.getBar(predictionResult.get().getProgress(), 12),
                        progress
                );
                EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, processingString);
                eb.setTimestamp(Instant.now());
                embeds.add(eb.build());
            }
        }

        return embeds;
    }

    private List<String> processNsfwImages(List<String> outputs) {
        return outputs.parallelStream()
                .map(url -> NsfwDetection.isNsfw(url) ? "https://cdn.discordapp.com/attachments/499629904380297226/1250414747636203623/nsfw_censored.png" : url)
                .collect(Collectors.toList());
    }

}
