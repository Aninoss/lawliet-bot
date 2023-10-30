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
import core.utils.*;
import modules.txt2img.*;
import mysql.hibernate.entity.UserEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
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

    public static int LIMIT_CREATIONS_PER_DAY = 10;
    public static String SELECT_ID_MODEL = "model";
    public static String DEFAULT_NEGATIVE_PROMPT = "worst quality, low quality, low-res, ugly, extra limbs, missing limb, floating limbs, disconnected limbs, mutated hands, extra legs, extra arms, bad anatomy, bad proportions, weird hands, malformed hands, disproportionate, disfigured, mutation, mutated, deformed, head out of frame, body out of frame, poorly drawn face, poorly drawn hands, poorly drawn feet, disfigured, out of frame, long neck, big ears, tiling, bad hands, bad art, cross-eye, blurry, blurred, watermark";
    private static final String[] INAPPROPRIATE_CONTENT_FILTERS = {"nigga", "nigger", "niggas", "niggers", "rape", "raping", "raped"};

    private String prompt;
    private String negativePrompt;

    public RunPodAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    public abstract List<String> getFilters(@NotNull CommandEvent event);

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        Instant bannedUntil = getEntityManager()
                .findOrDefaultReadOnly(UserEntity.class, event.getUser().getId())
                .getTxt2ImgBannedUntil();

        if (bannedUntil != null && Instant.now().isBefore(bannedUntil)) {
            String error = TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_banned", String.valueOf(bannedUntil.getEpochSecond()));
            drawMessageNew(EmbedFactory.getEmbedError(this, error))
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
        prompt = StringUtil.shortenString(prompt, MessageEmbed.DESCRIPTION_MAX_LENGTH - 50);

        if (NSFWUtil.containsNormalFilterTags(prompt, List.of(INAPPROPRIATE_CONTENT_FILTERS))) {
            drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_generalfilterblock")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }
        if (NSFWUtil.containsNormalFilterTags(prompt, getFilters(event))) {
            drawMessageNew(EmbedFactory.getEmbedError(this, getString("filterblock")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        FeatureLogger.inc(PremiumFeature.AI, event.getGuild().getIdLong());
        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public boolean controllerButton(ButtonInteractionEvent event, int i, int state) {
        if (i == -1) {
            deregisterListenersWithComponentMessage();
            return false;
        }
        return true;
    }

    @Override
    public boolean controllerStringSelectMenu(StringSelectInteractionEvent event, int i, int state) throws Throwable {
        int calls = Txt2ImgCallTracker.getCalls(getEntityManager(), event.getUser().getIdLong());
        if (calls < LIMIT_CREATIONS_PER_DAY) {
            Txt2ImgCallTracker.increaseCalls(getEntityManager(), event.getUser().getIdLong());

            String localPrompt = prompt;
            String localNegativePrompt = negativePrompt;

            Model model = Model.values()[Integer.parseInt(event.getValues().get(0))];
            String predictionId = RunPodDownloader.createPrediction(model, localPrompt, localNegativePrompt).get();
            AtomicReference<PredictionResult> predictionResult = new AtomicReference<>(null);
            Instant startTime = Instant.now();
            AtomicLong messageId = new AtomicLong(0);
            AtomicReference<Throwable> error = new AtomicReference<>();

            String modelName = getString("model_" + model.name());
            setLog(LogStatus.SUCCESS, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_go", modelName));

            if (requestProgress(event, error, messageId, localPrompt, localNegativePrompt, model, predictionId, predictionResult, startTime, calls == 0)) {
                poll(Duration.ofSeconds(2), () -> requestProgress(event, error, messageId, localPrompt, localNegativePrompt, model, predictionId, predictionResult, startTime, calls == 0));
            }
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_nocalls"));
        }
        return true;
    }

    @NotNull
    private Boolean requestProgress(StringSelectInteractionEvent event, AtomicReference<Throwable> error, AtomicLong messageId,
                                    String localPrompt, String localNegativePrompt, Model model, String predictionId,
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
                model, predictionId, predictionResult, startTime, includePromptHelp
        );
        if (messageId.get() == 0) {
            messageId.set(-1);
            event.getHook().sendMessageEmbeds(messageEmbeds).queue(message -> messageId.set(message.getIdLong()), error::set);
        } else {
            event.getHook().editMessageEmbedsById(messageId.get(), messageEmbeds).queue();
        }

        return predictionResult.get() == null || List.of(PredictionResult.Status.IN_QUEUE, PredictionResult.Status.IN_PROGRESS).contains(predictionResult.get().getStatus());
    }

    @Override
    public EmbedBuilder draw(Member member, int state) throws Throwable {
        StringSelectMenu.Builder menuBuilder = StringSelectMenu.create(SELECT_ID_MODEL)
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
        setComponents(menuBuilder.build());
        EmbedBuilder eb = generateOptionsEmbed(prompt, negativePrompt, null);
        eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), getString("contentwarning"), false);

        String footer = TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_footer",
                String.valueOf(LIMIT_CREATIONS_PER_DAY - Txt2ImgCallTracker.getCalls(getEntityManager(), member.getIdLong())),
                String.valueOf(LIMIT_CREATIONS_PER_DAY)
        );
        return EmbedUtil.setFooter(eb, this, footer);
    }

    private EmbedBuilder generateOptionsEmbed(String prompt, String negativePrompt, Model model) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .addField(
                        TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_textprompt_title"),
                        "```" + StringUtil.shortenString(StringUtil.escapeMarkdownInField(prompt), 1024) + "```",
                        false
                );

        if (!negativePrompt.isEmpty()) {
            eb.addField(
                    TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_negativeprompt_title"),
                    "```" + StringUtil.shortenString(StringUtil.escapeMarkdownInField(negativePrompt), 1024) + "```",
                    false
            );
        }

        if (model != null) {
            String modelName = getString("model_" + model.name());
            eb.addField(
                    TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_options_title"),
                    TextManager.getString(getLocale(), Category.AI_TOYS, "txt2img_options", modelName),
                    false
            );
        }

        return eb;
    }

    private List<MessageEmbed> generateLoadingEmbeds(Member member, String prompt, String negativePrompt, Model model,
                                                     String predictionId, AtomicReference<PredictionResult> predictionResult,
                                                     Instant startTime, boolean includePromptHelp
    ) {
        ArrayList<MessageEmbed> embeds = new ArrayList<>();

        if (predictionResult.get() == null || predictionResult.get().getStatus() != PredictionResult.Status.COMPLETED) {
            try {
                predictionResult.set(RunPodDownloader.retrievePrediction(model, predictionId, startTime).get());
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
                    EmbedBuilder eb = (first ? generateOptionsEmbed(prompt, negativePrompt, model) : EmbedFactory.getEmbedDefault(this))
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
