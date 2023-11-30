package commands.runnables;

import commands.Category;
import commands.CommandEvent;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.MainLogger;
import core.TextManager;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.modals.ModalMediator;
import core.utils.*;
import modules.txt2img.*;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public abstract class RunPodAbstract extends NavigationAbstract {

    public static int LIMIT_CREATIONS_PER_WEEK = 50;
    public static int PROMPT_MAX_LENGTH = 2000;
    public static String DEFAULT_NEGATIVE_PROMPT = "worst quality, low quality, low-res, ugly, extra limbs, missing limb, floating limbs, disconnected limbs, mutated hands, extra legs, extra arms, bad anatomy, bad proportions, weird hands, malformed hands, disproportionate, disfigured, mutation, mutated, deformed, head out of frame, body out of frame, poorly drawn face, poorly drawn hands, poorly drawn feet, disfigured, out of frame, long neck, big ears, tiling, bad hands, bad art, cross-eye, blurry, blurred, watermark";
    private static final String[] INAPPROPRIATE_CONTENT_FILTERS = {"nigga", "nigger", "niggas", "niggers", "rape", "raping", "raped"};

    private static final int STATE_ADJUST_IMAGES = 1;

    private final String additionalNegativePrompt;
    private String prompt;
    private String negativePrompt;
    private int images = 1;

    public RunPodAbstract(Locale locale, String prefix, String additionalNegativePrompt) {
        super(locale, prefix);
        this.additionalNegativePrompt = additionalNegativePrompt;
    }

    public abstract List<String> getFilters(long guildId);

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        EmbedBuilder errorEmbedIfBanned = getErrorEmbedIfBanned(event.getUser().getIdLong());
        if (errorEmbedIfBanned != null) {
            drawMessageNew(errorEmbedIfBanned)
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        if (args.contains("||")) {
            String[] parts = args.split("\\|\\|");
            if (parts.length <= 2) {
                prompt = parts[0].trim();
                negativePrompt = parts.length == 2 ? parts[1].trim() : "";
            } else {
                drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_ambiguous_negativeprompt", "||")))
                        .exceptionally(ExceptionLogger.get());
                return false;
            }
        } else if (args.contains("|")) {
            String[] parts = args.split("\\|");
            if (parts.length <= 2) {
                prompt = parts[0].trim();
                String tempNegativePromptInput = parts.length == 2 ? parts[1].trim() : "";
                StringBuilder negativePromptBuilder = new StringBuilder(tempNegativePromptInput);
                for (String p : DEFAULT_NEGATIVE_PROMPT.split(", ")) {
                    if (!tempNegativePromptInput.matches("(?i)(^|.*,)[ ]*" + Pattern.quote(p) + "[ ]*(,.*|$)")) {
                        if (!negativePromptBuilder.isEmpty()) {
                            negativePromptBuilder.append(", ");
                        }
                        negativePromptBuilder.append(p);
                    }
                }
                negativePrompt = negativePromptBuilder.toString();
            } else {
                drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_ambiguous_negativeprompt", "|")))
                        .exceptionally(ExceptionLogger.get());
                return false;
            }
        } else {
            prompt = args;
            negativePrompt = DEFAULT_NEGATIVE_PROMPT;
        }
        prompt = StringUtil.shortenString(prompt, PROMPT_MAX_LENGTH);
        negativePrompt = StringUtil.shortenString(negativePrompt, PROMPT_MAX_LENGTH);

        String error = checkFilterWords(prompt, event.getGuild().getIdLong());
        if (error != null) {
            drawMessageNew(EmbedFactory.getEmbedError(this, error))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        FeatureLogger.inc(PremiumFeature.AI, event.getGuild().getIdLong());
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
                        .setValue(negativePrompt)
                        .setMinLength(1)
                        .setMaxLength(PROMPT_MAX_LENGTH)
                        .build();

                Modal modal = ModalMediator.createDrawableCommandModal(this, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_default_prompts"), e -> {
                            String newPrompt = e.getValue(promptId).getAsString();
                            String error = checkFilterWords(newPrompt, e.getGuild().getIdLong());
                            if (error != null) {
                                setLog(LogStatus.FAILURE, error.replace("`", ""));
                                return null;
                            }

                            prompt = newPrompt;
                            negativePrompt = e.getValue(negativePromptId).getAsString();
                            setLog(LogStatus.SUCCESS, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_default_promptupdate"));
                            return null;
                        }).addActionRows(ActionRow.of(textInputPrompt), ActionRow.of(textInputNegativePrompt))
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case 1 -> {
                setState(STATE_ADJUST_IMAGES);
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

        images = i + 1;
        setLog(LogStatus.SUCCESS, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_images_set", StringUtil.numToString(images)));
        setState(DEFAULT_STATE);
        return true;
    }

    @Override
    public boolean controllerStringSelectMenu(StringSelectInteractionEvent event, int i, int state) throws Throwable {
        EmbedBuilder errorEmbedIfBanned = getErrorEmbedIfBanned(event.getUser().getIdLong());
        if (errorEmbedIfBanned != null) {
            deregisterListeners();
            drawMessage(errorEmbedIfBanned)
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        int calls = Txt2ImgCallTracker.getCalls(getEntityManager(), event.getUser().getIdLong());
        if (calls + images <= LIMIT_CREATIONS_PER_WEEK) {
            Txt2ImgCallTracker.increaseCalls(getEntityManager(), event.getUser().getIdLong(), images);

            String localPrompt = prompt;
            String localNegativePrompt = negativePrompt;
            int localImages = images;

            Model model = Model.values()[Integer.parseInt(event.getValues().get(0))];
            String predictionId = RunPodDownloader.createPrediction(model, localPrompt, additionalNegativePrompt + localNegativePrompt, localImages).get();
            AtomicReference<PredictionResult> predictionResult = new AtomicReference<>(null);
            Instant startTime = Instant.now();
            AtomicLong messageId = new AtomicLong(0);
            AtomicReference<Throwable> error = new AtomicReference<>();

            String modelName = getString("model_" + model.name());
            setLog(LogStatus.SUCCESS, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_go", modelName));

            if (requestProgress(event, error, messageId, localPrompt, localNegativePrompt, model, localImages,
                    predictionId, predictionResult, startTime, calls == 0)
            ) {
                poll(Duration.ofSeconds(2), () -> requestProgress(event, error, messageId, localPrompt, localNegativePrompt,
                        model, images, predictionId, predictionResult, startTime, calls == 0));
            }
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_nocalls"));
        }
        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) {
        ArrayList<ActionRow> actionRows = new ArrayList<>();

        ArrayList<Button> buttons = new ArrayList<>();
        String[] buttonLabels = TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_default_options").split("\n");
        for (int i = 0; i < buttonLabels.length; i++) {
            buttons.add(Button.of(ButtonStyle.PRIMARY, String.valueOf(i), buttonLabels[i]));
        }
        actionRows.add(ActionRow.of(buttons));

        StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("model")
                .setMinValues(1)
                .setMaxValues(1)
                .setPlaceholder(TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_selectmodel"));

        for (int i = 0; i < Model.values().length; i++) {
            Model model = Model.values()[i];
            if (!model.getClasses().contains(getClass())) {
                continue;
            }
            menuBuilder.addOption(
                    TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_run", getString("model_" + model.name())),
                    String.valueOf(i)
            );
        }
        actionRows.add(ActionRow.of(menuBuilder.build()));
        setActionRows(actionRows);

        EmbedBuilder eb = generateOptionsEmbed(prompt, negativePrompt, null, images);
        eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), getString("contentwarning"), false);

        String footer = TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_footer",
                String.valueOf(LIMIT_CREATIONS_PER_WEEK - Txt2ImgCallTracker.getCalls(getEntityManager(), member.getIdLong())),
                String.valueOf(LIMIT_CREATIONS_PER_WEEK)
        );
        return EmbedUtil.setFooter(eb, this, footer);
    }

    @Draw(state = STATE_ADJUST_IMAGES)
    public EmbedBuilder drawAdjustImages(Member member) {
        setComponents(TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_images_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this,
                TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_images_desc"),
                TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_images_title")
        );
    }

    private EmbedBuilder getErrorEmbedIfBanned(long userId) {
        Instant bannedUntil = getEntityManager()
                .findUserEntityReadOnly(userId)
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
        if (NSFWUtil.containsNormalFilterTags(prompt, getFilters(guildId))) {
            return getString("filterblock");
        }
        return null;
    }

    private Boolean requestProgress(StringSelectInteractionEvent event, AtomicReference<Throwable> error, AtomicLong messageId,
                                    String localPrompt, String localNegativePrompt, Model model, int images, String predictionId,
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
                model, images, predictionId, predictionResult, startTime, includePromptHelp
        );
        if (messageId.get() == 0) {
            messageId.set(-1);
            event.getHook().sendMessageEmbeds(messageEmbeds).queue(message -> messageId.set(message.getIdLong()), error::set);
        } else {
            event.getHook().editMessageEmbedsById(messageId.get(), messageEmbeds).queue();
        }

        return predictionResult.get() == null || List.of(PredictionResult.Status.IN_QUEUE, PredictionResult.Status.IN_PROGRESS).contains(predictionResult.get().getStatus());
    }

    private EmbedBuilder generateOptionsEmbed(String prompt, String negativePrompt, Model model, int images) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .addField(
                        TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_textprompt_title"),
                        "```" + StringUtil.shortenString(StringUtil.escapeMarkdownInField(prompt), MessageEmbed.VALUE_MAX_LENGTH - 6) + "```",
                        false
                );

        if (!negativePrompt.isEmpty()) {
            eb.addField(
                    TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_negativeprompt_title"),
                    "```" + StringUtil.shortenString(StringUtil.escapeMarkdownInField(negativePrompt), MessageEmbed.VALUE_MAX_LENGTH - 6) + "```",
                    false
            );
        }

        String options = TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_options",
                StringUtil.numToString(images)
        );
        if (model != null) {
            String modelName = getString("model_" + model.name());
            options += "\n" + TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_options_model", modelName);
        }

        return eb.addField(
                TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_options_title"),
                options,
                false
        );
    }

    private List<MessageEmbed> generateLoadingEmbeds(Member member, String prompt, String negativePrompt, Model model,
                                                     int images, String predictionId, AtomicReference<PredictionResult> predictionResult,
                                                     Instant startTime, boolean includePromptHelp
    ) {
        ArrayList<MessageEmbed> embeds = new ArrayList<>();

        if (predictionResult.get() == null || predictionResult.get().getStatus() != PredictionResult.Status.COMPLETED) {
            try {
                predictionResult.set(RunPodDownloader.retrievePrediction(model, predictionId, startTime, images).get());
                if (predictionResult.get().getStatus() == PredictionResult.Status.COMPLETED) {
                    if (model.getCustomModel()) {
                        List<String> newOutputs = convertBase64ToTempFileUrls(predictionResult.get().getOutputs());
                        predictionResult.get().setOutputs(newOutputs);
                    }
                    Txt2ImgLogger.log(prompt, member.getUser(), model.name(), predictionResult.get().getOutputs());
                }
            } catch (Throwable e) {
                MainLogger.get().error("Prediction failed", e);
                predictionResult.set(PredictionResult.failed(PredictionResult.Error.GENERAL));
            }
        }

        switch (predictionResult.get().getStatus()) {
            case COMPLETED -> {
                boolean first = true;
                for (String output : predictionResult.get().getOutputs()) {
                    EmbedBuilder eb = (first ? generateOptionsEmbed(prompt, negativePrompt, model, images) : EmbedFactory.getEmbedDefault(this))
                            .setImage(output);
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
                String processingString = TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_processing",
                        StringUtil.getBar(predictionResult.get().getProgress(), 12),
                        String.valueOf((int) (predictionResult.get().getProgress() * 100))
                );
                EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, processingString);
                embeds.add(eb.build());
            }
        }

        return embeds;
    }

    private List<String> convertBase64ToTempFileUrls(List<String> base64Strings) {
        ArrayList<String> imageUrls = new ArrayList<>();
        for (String base64String : base64Strings) {
            byte[] bytes = Base64.getDecoder().decode(base64String);
            try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
                String imageUrl = InternetUtil.getUrlFromInputStream(is, "png");
                imageUrls.add(imageUrl);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return imageUrls;
    }

}
