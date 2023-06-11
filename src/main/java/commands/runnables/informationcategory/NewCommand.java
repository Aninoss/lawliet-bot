package commands.runnables.informationcategory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import constants.AssetIds;
import constants.Emojis;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.BotUtil;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import modules.schedulers.AlertResponse;
import mysql.modules.tracker.TrackerData;
import mysql.modules.version.DBVersion;
import mysql.modules.version.VersionData;
import mysql.modules.version.VersionSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "new",
        emoji = "\uD83C\uDD95",
        executableWithoutArgs = true,
        aliases = { "changelog" }
)
public class NewCommand extends Command implements OnAlertListener {

    VersionData versionData;

    public NewCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        versionData = DBVersion.getInstance().retrieve();

        // without args
        if (args.length() == 0) {
            List<VersionSlot> versions = versionData.getCurrentVersions(1);
            drawMessageNew(getEmbedNormal(event.getMember(), versions, true))
                    .exceptionally(ExceptionLogger.get());
            return true;
        } else {
            // number
            if (StringUtil.stringIsLong(args)) {
                int i = Integer.parseInt(args);
                if (i >= 1) {
                    if (i <= 10) {
                        List<VersionSlot> versions = versionData.getCurrentVersions(i);
                        drawMessageNew(getEmbedNormal(event.getMember(), versions, false))
                                .exceptionally(ExceptionLogger.get());
                        return true;
                    } else {
                        drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_large", "10")))
                                .exceptionally(ExceptionLogger.get());
                        return false;
                    }
                } else {
                    drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1")))
                            .exceptionally(ExceptionLogger.get());
                    return false;
                }
            } else {
                List<String> askVersions = Arrays.stream(args.split(" ")).filter(str -> !str.isEmpty()).collect(Collectors.toList());
                List<VersionSlot> versions = versionData.getSlots().stream().filter(slot -> askVersions.contains(slot.getVersion())).collect(Collectors.toList());

                if (versions.size() > 0) {
                    drawMessageNew(getEmbedNormal(event.getMember(), versions, false))
                            .exceptionally(ExceptionLogger.get());
                    return true;
                } else {
                    drawMessageNew(EmbedFactory.getNoResultsEmbed(this, args))
                            .exceptionally(ExceptionLogger.get());
                    return false;
                }
            }
        }
    }

    private EmbedBuilder getEmbedNormal(Member member, List<VersionSlot> versions, boolean showEmptyFooter) {
        EmbedBuilder eb = getVersionsEmbed(versions, showEmptyFooter);
        EmbedUtil.addTrackerNoteLog(getLocale(), member, eb, getPrefix(), getTrigger());
        return eb;
    }

    private EmbedBuilder getVersionsEmbed(VersionSlot slot) {
        ArrayList<VersionSlot> versions = new ArrayList<>();
        versions.add(slot);
        return getVersionsEmbed(versions, false);
    }

    private EmbedBuilder getVersionsEmbed(List<VersionSlot> versions, boolean showEmptyFooter) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);
        if (showEmptyFooter) EmbedUtil.setFooter(eb, this, getString("footer"));
        for (int i = versions.size() - 1; i >= 0; i--) {
            VersionSlot slot = versions.get(i);

            StringBuilder[] stringBuilders = new StringBuilder[] { new StringBuilder(), new StringBuilder() };
            String[] lines = TextManager.getString(getLocale(), TextManager.VERSIONS, slot.getVersion())
                    .replace("{PREFIX}", getPrefix())
                    .split("\n");

            for (String line : lines) {
                String newLine = "• " + line + "\n";
                if (stringBuilders[0].length() + newLine.length() < 1024) {
                    stringBuilders[0].append(newLine);
                } else {
                    stringBuilders[1].append(newLine);
                }
            }

            eb.addField(slot.getVersion(), stringBuilders[0].toString(), false);
            if (!stringBuilders[1].isEmpty()) {
                eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), stringBuilders[1].toString(), false);
            }
        }
        return eb;
    }

    @Override
    public @NotNull AlertResponse onTrackerRequest(@NotNull TrackerData slot) throws Throwable {
        if (slot.getArgs().isEmpty() || !slot.getArgs().get().equals(BotUtil.getCurrentVersion())) {
            VersionSlot newestSlot = DBVersion.getInstance().retrieve().getCurrentVersion();
            long messageId = slot.sendMessage(true, getVersionsEmbed(newestSlot).build()).orElse(0L);
            if (slot.getGuildId() == AssetIds.SUPPORT_SERVER_ID && messageId != 0) {
                ((NewsChannel) slot.getStandardGuildMessageChannel().get()).crosspostMessageById(messageId).queueAfter(10, TimeUnit.MINUTES);
            }

            slot.setArgs(BotUtil.getCurrentVersion());
            return AlertResponse.STOP_AND_SAVE;
        }

        return AlertResponse.STOP;
    }

    @Override
    public boolean trackerUsesKey() {
        return false;
    }

}
