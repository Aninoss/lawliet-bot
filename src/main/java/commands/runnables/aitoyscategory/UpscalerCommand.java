package commands.runnables.aitoyscategory;

import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnStringSelectMenuListener;
import constants.ExternalLinks;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.cache.PatreonCache;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.utils.*;
import modules.txt2img.PredictionResult;
import modules.txt2img.RunPodDownloader;
import modules.txt2img.Txt2ImgCallTracker;
import modules.txt2img.UpscalerModel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "upscaler",
        emoji = "📈",
        executableWithoutArgs = true,
        aliases = {"waifu2x", "waifu4x", "upscale"}
)
public class UpscalerCommand extends Command implements OnStringSelectMenuListener {

    public static int MAX_IMAGES = 5;
    public static long MAX_ALLOWED_PIXEL_SIZE = 1024 * 1024;
    private static final ScaleAndModel[] SCALE_AND_MODELS = new ScaleAndModel[]{
            new ScaleAndModel(1, UpscalerModel.R_ESRGAN, Long.MAX_VALUE),
            new ScaleAndModel(1, UpscalerModel.NMKD_SIAX, Long.MAX_VALUE),
            new ScaleAndModel(1, UpscalerModel.NMKD_ULTRAYANDERE, Long.MAX_VALUE),
            new ScaleAndModel(2, UpscalerModel.R_ESRGAN, Long.MAX_VALUE),
            new ScaleAndModel(2, UpscalerModel.NMKD_SIAX, Long.MAX_VALUE),
            new ScaleAndModel(2, UpscalerModel.NMKD_ULTRAYANDERE, Long.MAX_VALUE),
            new ScaleAndModel(4, UpscalerModel.R_ESRGAN, 614_400L),
            new ScaleAndModel(4, UpscalerModel.NMKD_SIAX, 614_400L),
            new ScaleAndModel(4, UpscalerModel.NMKD_ULTRAYANDERE, 614_400L),
    };

    private List<String> base64Images;
    private PredictionResult predictionResult = null;
    private long biggestImagePixels = 0L;

    public UpscalerCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        List<File> attachedFiles = getAttachment("files", List.class);
        if (attachedFiles != null) {
            base64Images = attachedFiles.stream()
                    .map(file -> {
                        try {
                            BufferedImage image = ImageIO.read(file);
                            biggestImagePixels = Math.max(biggestImagePixels, (long) image.getWidth() * image.getHeight());
                            return FileUtil.fileToBase64(file);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());
            registerStringSelectMenuListener(event.getMember());
            return true;
        }

        List<Message.Attachment> imageAttachments = event.getAttachments().stream()
                .filter(attachment -> InternetUtil.uriIsImage(attachment.getUrl(), false))
                .collect(Collectors.toList());
        if (imageAttachments.isEmpty() || imageAttachments.size() > MAX_IMAGES) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("error_invalidattachments", StringUtil.numToString(MAX_IMAGES)));
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }

        deferReply();
        base64Images = imageAttachments.stream()
                .filter(attachment -> (long) attachment.getWidth() * attachment.getHeight() <= MAX_ALLOWED_PIXEL_SIZE)
                .map(attachment -> {
                    biggestImagePixels = Math.max(biggestImagePixels, (long) attachment.getWidth() * attachment.getHeight());

                    try {
                        InputStream inputStream = attachment.getProxy().download().get();
                        String base64 = InternetUtil.inputStreamToBase64(inputStream);
                        inputStream.close();
                        return base64;
                    } catch (IOException | InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        if (base64Images.size() < imageAttachments.size()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("error_toolarge"));
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }

        registerStringSelectMenuListener(event.getMember());
        return true;
    }

    @Override
    public boolean onStringSelectMenu(@NotNull StringSelectInteractionEvent event) throws ExecutionException, InterruptedException {
        boolean premium = PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), true) ||
                PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong());
        int remainingCalls = Txt2ImgCallTracker.getRemainingCalls(getEntityManager(), event.getMember().getIdLong(), premium);

        if (remainingCalls >= base64Images.size()) {
            deregisterListeners();
            if (premium) {
                FeatureLogger.inc(PremiumFeature.AI, event.getGuild().getIdLong());
            }
            Txt2ImgCallTracker.increaseCalls(getEntityManager(), event.getUser().getIdLong(), premium, base64Images.size());

            ScaleAndModel scaleAndModel = SCALE_AND_MODELS[Integer.parseInt(event.getValues().get(0))];
            String predictionId = RunPodDownloader.createUpscalePrediction(scaleAndModel.model, scaleAndModel.scale, base64Images).get();
            base64Images.clear();

            AtomicReference<Throwable> error = new AtomicReference<>();
            if (requestProgress(event.getMember(), event.getGuildChannel(), error, predictionId)) {
                poll(Duration.ofSeconds(2), () -> {
                    try {
                        return requestProgress(event.getMember(), event.getGuildChannel(), error, predictionId);
                    } catch (ExecutionException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            return false;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), Category.AI_TOYS, premium ? "txt2img_nocalls" : "txt2img_nocalls_nopremium"));
            return true;
        }
    }

    @Nullable
    @Override
    public EmbedBuilder draw(Member member) {
        if (predictionResult == null) {
            ArrayList<ActionRow> actionRows = new ArrayList<>();
            Button button = Button.of(ButtonStyle.LINK, ExternalLinks.PREMIUM_WEBSITE + "?tab=1", getString("buybutton"));
            actionRows.add(ActionRow.of(button));

            StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("scale_and_model")
                    .setRequiredRange(1, 1)
                    .setPlaceholder(getString("home_placeholder"));
            for (int i = 0; i < SCALE_AND_MODELS.length; i++) {
                ScaleAndModel scaleAndModel = SCALE_AND_MODELS[i];
                if (biggestImagePixels <= scaleAndModel.maxAllowedPixels) {
                    menuBuilder.addOption(
                            getString("model_style_" + scaleAndModel.model.name(), String.valueOf(scaleAndModel.scale)),
                            String.valueOf(i),
                            getString("model_name_" + scaleAndModel.model.name())
                    );
                }
            }
            SelectMenu menu = menuBuilder.build();
            actionRows.add(ActionRow.of(menu));
            setActionRows(actionRows);

            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("home_desc"))
                    .addField(getString("home_images"), StringUtil.numToString(base64Images.size()), true);
            EmbedUtil.setFooter(eb, this, Txt2ImgCallTracker.getRemainingImagesText(getLocale(), member.getGuild().getIdLong(), member.getIdLong(), getUserEntity()));
            return eb;
        }

        switch (predictionResult.getStatus()) {
            case IN_QUEUE, IN_PROGRESS -> {
                String text = getString("status_processing",
                        EmojiUtil.getLoadingEmojiMention(getGuildMessageChannel().get()),
                        getString("status_processing_status").split("\n")[predictionResult.getStatus() == PredictionResult.Status.IN_PROGRESS ? 1 : 0]
                );
                EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, text);
                EmbedUtil.setFooter(eb, this, Txt2ImgCallTracker.getRemainingImagesText(getLocale(), member.getGuild().getIdLong(), member.getIdLong(), getUserEntity()));
                return eb;
            }
            case COMPLETED -> {
                List<String> imageUrls = InternetUtil.base64ToTempUrl(predictionResult.getOutputs());
                EmbedBuilder mainEmbed = null;
                ArrayList<MessageEmbed> additionalEmbeds = new ArrayList<>();

                for (int i = 0; i < imageUrls.size(); i++) {
                    EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                            .setImage(imageUrls.get(i));
                    EmbedUtil.setFooter(eb, this, getString("footer_temp"));
                    if (i == 0) {
                        eb.setDescription(getString("status_success"));
                        mainEmbed = eb;
                    } else {
                        additionalEmbeds.add(eb.build());
                    }
                }
                setAdditionalEmbeds(additionalEmbeds);
                return mainEmbed;
            }
        }
        return EmbedFactory.getEmbedError(this, getString("error_unspecific"));
    }

    private boolean requestProgress(Member member, GuildChannel guildChannel, AtomicReference<Throwable> error, String predictionId) throws ExecutionException, InterruptedException {
        if (!BotPermissionUtil.canWriteEmbed(guildChannel, Permission.MESSAGE_HISTORY)) {
            return false;
        }
        if (error.get() != null) {
            ExceptionUtil.handleCommandException(error.get(), this, getCommandEvent(), getGuildEntity());
            return false;
        }

        predictionResult = RunPodDownloader.retrieveUpscalePrediction(predictionId).get();
        drawMessage(draw(member)).exceptionally(exception -> {
            error.set(exception);
            return null;
        });
        return List.of(PredictionResult.Status.IN_QUEUE, PredictionResult.Status.IN_PROGRESS).contains(predictionResult.getStatus());
    }


    private static class ScaleAndModel {

        private final int scale;
        private final UpscalerModel model;
        private final long maxAllowedPixels;

        public ScaleAndModel(int scale, UpscalerModel model, long maxAllowedPixels) {
            this.scale = scale;
            this.model = model;
            this.maxAllowedPixels = maxAllowedPixels;
        }

    }

}
