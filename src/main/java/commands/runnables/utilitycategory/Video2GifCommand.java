package commands.runnables.utilitycategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.ComponentMenuAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.ExceptionLogger;
import core.GlobalThreadPool;
import core.LocalFile;
import core.MainLogger;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.utils.ComponentsUtil;
import core.utils.EmojiUtil;
import core.utils.StringUtil;
import modules.video2gif.Video2GifConverter;
import modules.video2gif.Video2GifConverter.Failure;
import modules.video2gif.Video2GifConverter.Video2GifException;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

@CommandProperties(
        trigger = "video2gif",
        emoji = "🎞️",
        executableWithoutArgs = true,
        patreonRequired = true,
        releaseVersion = "2.75",
        aliases = {"vid2gif", "videotogif"}
)
public class Video2GifCommand extends ComponentMenuAbstract {

    private static final String STATE_CONVERTING = "converting";

    private Message.Attachment attachment;
    private int width = Video2GifConverter.DEFAULT_WIDTH;
    private int fps = Video2GifConverter.DEFAULT_FPS;
    private int length = Video2GifConverter.DEFAULT_LENGTH_SECONDS;
    private int skip = Video2GifConverter.DEFAULT_SKIP_SECONDS;
    private boolean loop = true;

    public Video2GifCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        List<Message.Attachment> attachments = event.getAttachments();
        if (attachments.isEmpty()) {
            drawMessageNew(ComponentsUtil.createCommandComponentTreeError(this, getString("error_noattachment", getSupportedExtensionsText())))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        attachment = attachments.stream()
                .filter(a -> Video2GifConverter.getSupportedExtension(a) != null)
                .findFirst()
                .orElse(null);
        if (attachment == null) {
            drawMessageNew(ComponentsUtil.createCommandComponentTreeError(this, getFailureText(Failure.UNSUPPORTED_TYPE)))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        FeatureLogger.inc(PremiumFeature.VIDEO2GIF, event.getGuild().getIdLong());
        registerListeners(event.getMember());
        return true;
    }

    @Draw(state = STATE_ROOT_ID)
    public List<ContainerChildComponent> drawRoot(Member member) {
        setDescription(getString("root_description", getMaxOutputMegabytes()));

        ArrayList<ContainerChildComponent> components = new ArrayList<>();
        components.add(TextDisplay.of(getString("root_video") + "\n> " + getVideoText()));

        String widthString = getString("root_width");
        Button widthButton = buttonSecondary(Emojis.MENU_EDIT, e -> {
            Modal modal = setIntModal(
                    widthString,
                    width,
                    null,
                    Video2GifConverter.MIN_WIDTH,
                    Video2GifConverter.MAX_WIDTH,
                    newWidth -> width = newWidth
            );
            e.replyModal(modal).queue();
            return false;
        });
        components.add(Section.of(widthButton, TextDisplay.of(widthString + "\n> " + getString("root_width_value", String.valueOf(width)))));

        String fpsString = getString("root_fps");
        Button fpsButton = buttonSecondary(Emojis.MENU_EDIT, e -> {
            Modal modal = setIntModal(
                    fpsString,
                    fps,
                    null,
                    Video2GifConverter.MIN_FPS,
                    Video2GifConverter.MAX_FPS,
                    newFps -> fps = newFps
            );
            e.replyModal(modal).queue();
            return false;
        });
        components.add(Section.of(fpsButton, TextDisplay.of(fpsString + "\n> " + getString("root_fps_value", String.valueOf(fps)))));

        String lengthString = getString("root_length");
        Button lengthButton = buttonSecondary(Emojis.MENU_EDIT, e -> {
            Modal modal = setIntModal(
                    lengthString,
                    length,
                    null,
                    Video2GifConverter.MIN_LENGTH_SECONDS,
                    Video2GifConverter.MAX_LENGTH_SECONDS,
                    newLength -> length = newLength,
                    TextDisplay.of("-# " + getString("root_length_hint"))
            );
            e.replyModal(modal).queue();
            return false;
        });
        components.add(Section.of(lengthButton, TextDisplay.of(lengthString + "\n> " + getString("root_length_value", String.valueOf(length)))));

        String skipString = getString("root_skip");
        Button skipButton = buttonSecondary(Emojis.MENU_EDIT, e -> {
            Modal modal = setIntModal(
                    skipString,
                    skip,
                    null,
                    Video2GifConverter.MIN_SKIP_SECONDS,
                    Video2GifConverter.MAX_SKIP_SECONDS,
                    newSkip -> skip = newSkip
            );
            e.replyModal(modal).queue();
            return false;
        });
        components.add(Section.of(skipButton, TextDisplay.of(skipString + "\n> " + getString("root_skip_value", String.valueOf(skip)))));

        components.add(buttonBoolean(getString("root_loop"), loop, newLoop -> loop = newLoop));

        components.add(Separator.createDivider(Separator.Spacing.LARGE));
        components.add(ActionRow.of(buttonPrimary(getString("root_start"), this::onStart)));
        return components;
    }

    @Draw(state = STATE_CONVERTING)
    public List<ContainerChildComponent> drawConverting(Member member) {
        String loadingEmoji = EmojiUtil.getLoadingEmojiMention(getGuildMessageChannel().orElse(null));
        return List.of(
                TextDisplay.of(getString("converting", loadingEmoji)),
                TextDisplay.of(getSettingsText(width, fps, loop, length, skip))
        );
    }

    private boolean onStart(ButtonInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        Failure failure = Video2GifConverter.acquire(userId);
        if (failure != null) {
            setLog(LogStatus.FAILURE, getFailureText(failure));
            return true;
        }

        int localWidth = width;
        int localFps = fps;
        boolean localLoop = loop;
        int localLength = length;
        int localSkip = skip;
        Member member = event.getMember();

        setState(STATE_CONVERTING);
        drawMessage(draw(member))
                .whenComplete((message, e) -> GlobalThreadPool.submit(() -> convert(event, member, userId, localWidth, localFps, localLoop, localLength, localSkip)));
        return false;
    }

    private void convert(ButtonInteractionEvent event, Member member, long userId, int width, int fps, boolean loop, int length, int skip) {
        LocalFile output = null;
        Failure failure = null;
        try {
            output = Video2GifConverter.convert(attachment, width, fps, loop, length, skip);
        } catch (Video2GifException e) {
            failure = e.getFailure();
        } catch (Throwable e) {
            MainLogger.get().error("Video2gif unexpected exception", e);
            failure = Failure.CONVERSION_FAILED;
        } finally {
            Video2GifConverter.release(userId);
        }

        try (GuildEntity guildEntity = refreshGuildEntity()) {
            setState(STATE_ROOT_ID);
            if (failure == null) {
                MessageComponentTree resultComponents = generateResultComponents(output, width, fps, loop, length, skip);
                event.getHook().sendMessageComponents(resultComponents)
                        .useComponentsV2()
                        .queue(null, e -> MainLogger.get().error("Video2gif result message exception", e));
                setLog(LogStatus.SUCCESS, getString("success"));
            } else {
                setLog(LogStatus.FAILURE, getFailureText(failure));
            }

            if (getDrawMessage().isPresent()) {
                drawMessage(draw(member))
                        .exceptionally(ExceptionLogger.get());
            }
        } catch (Throwable e) {
            MainLogger.get().error("Video2gif result exception", e);
        }
    }

    private MessageComponentTree generateResultComponents(LocalFile gifFile, int width, int fps, boolean loop, int length, int skip) {
        String url = gifFile.cdnGetUrl();
        ArrayList<ContainerChildComponent> components = new ArrayList<>();
        components.add(MediaGallery.of(List.of(MediaGalleryItem.fromUrl(url))));
        components.add(TextDisplay.of(getSettingsText(width, fps, loop, length, skip) + "｜" + formatMegabytes(gifFile.length()) + " MB"));
        components.add(ActionRow.of(Button.of(ButtonStyle.LINK, url + "?download=1", getString("result_download"))));
        components.add(Separator.createDivider(Separator.Spacing.SMALL));
        components.add(TextDisplay.of(getString("result_warning")));
        return ComponentsUtil.createCommandComponentTree(this, components);
    }

    private String getSettingsText(int width, int fps, boolean loop, int length, int skip) {
        return getString("settings_length", String.valueOf(length)) +
                (skip > 0 ? "｜" + getString("settings_skip", String.valueOf(skip)) : "") +
                "｜" + getString("root_width_value", String.valueOf(width)) +
                "｜" + getString("root_fps_value", String.valueOf(fps)) +
                "｜" + getString("result_loop").split("\n")[loop ? 1 : 0];
    }

    private String getVideoText() {
        String fileName = StringUtil.escapeMarkdown(StringUtil.shortenString(attachment.getFileName(), 100));
        return fileName + " (" + formatMegabytes(attachment.getSize()) + " MB)";
    }

    private String getFailureText(Failure failure) {
        String key = "error_" + failure.name().toLowerCase(Locale.ROOT);
        return switch (failure) {
            case UNSUPPORTED_TYPE -> getString(key, getSupportedExtensionsText());
            case OUTPUT_TOO_LARGE -> getString(key, getMaxOutputMegabytes());
            default -> getString(key);
        };
    }

    private static String getMaxOutputMegabytes() {
        return String.valueOf(Video2GifConverter.MAX_OUTPUT_BYTES / 1024 / 1024);
    }

    private String getSupportedExtensionsText() {
        return String.join(", ", new TreeSet<>(Video2GifConverter.getSupportedExtensions()));
    }

    private static String formatMegabytes(long bytes) {
        return String.format(Locale.ROOT, "%.1f", bytes / 1024.0 / 1024.0);
    }

}
