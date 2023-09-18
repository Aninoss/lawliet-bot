package commands.runnables.aitoyscategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnStringSelectMenuListener;
import constants.Emojis;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.MainLogger;
import core.utils.EmbedUtil;
import core.utils.ExceptionUtil;
import core.utils.NSFWUtil;
import core.utils.StringUtil;
import modules.txt2img.*;
import mysql.hibernate.entity.UserEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@CommandProperties(
        trigger = "txt2img",
        emoji = "🖌️",
        executableWithoutArgs = false,
        patreonRequired = true,
        aliases = {"stablediffusion", "diffusion", "imagine"}
)
public class Txt2ImgCommand extends Command implements OnStringSelectMenuListener {

    public static int LIMIT_CREATIONS_PER_DAY = 5;
    public static String SELECT_ID_MODEL = "model";
    public static String SELECT_ID_IMAGE = "image";
    public static String DEFAULT_NEGATIVE_PROMPT = "worst quality, low quality, low-res, ugly, extra limbs, missing limb, floating limbs, disconnected limbs, mutated hands, extra legs, extra arms, bad anatomy, bad proportions, weird hands, malformed hands, disproportionate, disfigured, mutation, mutated, deformed, head out of frame, body out of frame, poorly drawn face, poorly drawn hands, poorly drawn feet, disfigured, out of frame, long neck, big ears, tiling, bad hands, bad art, cross-eye, blurry, blurred, watermark";
    public static String[] NSFW_FILTERS = {"nsfw", "porn", "porno", "pornography", "sex", "intercourse", "coitus", "explicit", "fuck", "fucking", "fucked", "rape", "raping", "raped", "blowjob", "anal", "penis", "cock", "dick", "vagina", "pussy", "cum", "sperm"};

    private String prompt;
    private String negativePrompt;
    private String predictionId = null;
    private Model model = null;
    private PredictionResult predictionResult = null;
    private int currentImage = 0;
    private Instant startTime;

    public Txt2ImgCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        Instant bannedUntil = getEntityManager()
                .findOrDefaultReadOnly(UserEntity.class, event.getUser().getId())
                .getTxt2ImgBannedUntil();

        if (bannedUntil != null && Instant.now().isBefore(bannedUntil)) {
            String error = getString("banned", String.valueOf(bannedUntil.getEpochSecond()));
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
                drawMessageNew(EmbedFactory.getEmbedError(this, getString("ambiguous_negativeprompt", "||")))
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
                drawMessageNew(EmbedFactory.getEmbedError(this, getString("ambiguous_negativeprompt", "|")))
                        .exceptionally(ExceptionLogger.get());
                return false;
            }
        } else {
            prompt = args;
            negativePrompt = DEFAULT_NEGATIVE_PROMPT;
        }
        prompt = StringUtil.shortenString(prompt, MessageEmbed.DESCRIPTION_MAX_LENGTH - 50);
        if (NSFWUtil.containsNormalFilterTags(prompt, List.of(NSFW_FILTERS))) {
            drawMessageNew(EmbedFactory.getEmbedError(this, getString("filterblock")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }
        registerStringSelectMenuListener(event.getMember());
        return true;
    }

    @Override
    public boolean onStringSelectMenu(@NotNull StringSelectInteractionEvent event) throws Throwable {
        if (event.getComponentId().equals(SELECT_ID_MODEL)) {
            if (Txt2ImgCallTracker.getCalls(event.getGuild().getIdLong(), event.getUser().getIdLong()) < LIMIT_CREATIONS_PER_DAY) {
                Txt2ImgLogger.log(prompt, event.getUser().getIdLong());
                Txt2ImgCallTracker.increaseCalls(event.getGuild().getIdLong(), event.getUser().getIdLong());
                model = Model.values()[Integer.parseInt(event.getValues().get(0))];
                predictionId = RunPodDownloader.createPrediction(model, prompt, negativePrompt).get();
                startTime = Instant.now();
            }
        } else if (event.getComponentId().equals(SELECT_ID_IMAGE) && predictionResult != null && predictionResult.getOutputs().size() > 1) {
            currentImage = Integer.parseInt(event.getValues().get(0));
        }
        return true;
    }

    @Nullable
    @Override
    public EmbedBuilder draw(@NotNull Member member) throws Throwable {
        EmbedBuilder eb;
        if (predictionId == null) {
            if (Txt2ImgCallTracker.getCalls(member.getGuild().getIdLong(), member.getIdLong()) >= LIMIT_CREATIONS_PER_DAY) {
                return EmbedFactory.getEmbedError(this, getString("nocalls"), getString("nocalls_title"));
            }

            StringSelectMenu.Builder menuBuilder = StringSelectMenu.create(SELECT_ID_MODEL)
                    .setMinValues(1)
                    .setMaxValues(1)
                    .setPlaceholder(getString("selectmodel"));
            for (int i = 0; i < Model.values().length; i++) {
                menuBuilder.addOption(
                        getString("run", getString("models", i)),
                        String.valueOf(i)
                );
            }
            setComponents(menuBuilder.build());
            eb = generateOptionsEmbed(null);
            eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), getString("nsfwwarning"), false);
        } else {
            if (predictionResult == null || predictionResult.getStatus() != PredictionResult.Status.COMPLETED) {
                try {
                    predictionResult = RunPodDownloader.retrievePrediction(model, predictionId, startTime).get();
                } catch (Throwable e) {
                    MainLogger.get().error("Prediction failed", e);
                    predictionResult = PredictionResult.failed(PredictionResult.Error.GENERAL);
                }
            }

            switch (predictionResult.getStatus()) {
                case COMPLETED -> {
                    if (predictionResult.getOutputs().size() > 1) {
                        StringSelectMenu.Builder menuBuilder = StringSelectMenu.create(SELECT_ID_IMAGE)
                                .setMinValues(1)
                                .setMaxValues(1);
                        for (int i = 0; i < predictionResult.getOutputs().size(); i++) {
                            menuBuilder.addOption(
                                    getString("image", String.valueOf(i + 1)),
                                    String.valueOf(i)
                            );
                        }
                        menuBuilder = menuBuilder.setDefaultValues(String.valueOf(currentImage));
                        setComponents(menuBuilder.build());
                    }

                    eb = generateOptionsEmbed(predictionResult.getOutputs().get(currentImage));
                }
                case FAILED -> {
                    String error;
                    if (predictionResult.getError() == PredictionResult.Error.NSFW) {
                        error = getString("nsfw");
                    } else {
                        error = getString("error");
                    }
                    eb = EmbedFactory.getEmbedError(this, error);
                }
                default -> {
                    String processingString = getString(
                            "processing",
                            StringUtil.getBar(predictionResult.getProgress(), 12),
                            String.valueOf((int) (predictionResult.getProgress() * 100))
                    );
                    schedule(Duration.ofSeconds(1), () -> {
                        try {
                            drawMessage(draw(member));
                        } catch (Throwable e) {
                            ExceptionUtil.handleCommandException(e, this, getCommandEvent(), getGuildEntity());
                        }
                    });
                    eb = EmbedFactory.getEmbedDefault(this, processingString);
                }
            }
        }

        String footer = getString(
                "footer",
                String.valueOf(LIMIT_CREATIONS_PER_DAY - Txt2ImgCallTracker.getCalls(member.getGuild().getIdLong(), member.getIdLong())),
                String.valueOf(LIMIT_CREATIONS_PER_DAY)
        );
        return EmbedUtil.setFooter(eb, this, footer);
    }

    private EmbedBuilder generateOptionsEmbed(String imageUrl) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .addField(getString("textprompt_title"), "```" + StringUtil.shortenString(StringUtil.escapeMarkdownInField(prompt), 1024) + "```", false);

        if (!negativePrompt.isEmpty()) {
            eb.addField(getString("negativeprompt_title"), "```" + StringUtil.shortenString(StringUtil.escapeMarkdownInField(negativePrompt), 1024) + "```", false);
        }

        if (imageUrl != null) {
            String modelName = getString("models", model.ordinal());
            eb.addField(getString("options_title"), getString("options", modelName), false);
            eb.setImage(imageUrl);
        }

        return eb;
    }

}
